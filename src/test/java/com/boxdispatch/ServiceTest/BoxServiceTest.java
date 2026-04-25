package com.boxdispatch.ServiceTest;

import com.boxdispatch.Components.Mapper.BoxDispatchMapper;
import com.boxdispatch.Enums.BoxState;
import com.boxdispatch.Exceptions.BusinessRuleViolationException;
import com.boxdispatch.Exceptions.DuplicateResourceException;
import com.boxdispatch.Exceptions.ResourceNotFoundException;
import com.boxdispatch.Models.Box;
import com.boxdispatch.Models.Item;
import com.boxdispatch.Payloads.CreateBoxRequest;
import com.boxdispatch.Payloads.ItemRequest;
import com.boxdispatch.Payloads.LoadItemsRequest;
import com.boxdispatch.Repositories.BoxRepository;
import com.boxdispatch.Repositories.ItemRepository;
import com.boxdispatch.Responses.BatteryResponse;
import com.boxdispatch.Responses.BoxResponse;
import com.boxdispatch.Responses.ItemResponse;
import com.boxdispatch.Responses.LoadItemsResponse;
import com.boxdispatch.Services.BoxService;
import com.boxdispatch.Services.IdempotencyService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoxService Unit Tests")
public class BoxServiceTest {

    @Mock private BoxRepository      boxRepository;
    @Mock private ItemRepository     itemRepository;
    @Mock private BoxDispatchMapper  mapper;
    @Mock private IdempotencyService idempotencyService;

    @InjectMocks
    private BoxService boxService;

    // ─── Fixtures ──────────────────────────────────────────────────────────────

    private Box idleBox;
    private Box loadingBox;
    private Box loadedBox;
    private Box lowBatteryBox;

    @BeforeEach
    void setUp() {
        idleBox = Box.builder()
                .id(1L)
                .txref("BOX-001")
                .weightLimit(new BigDecimal("500.000"))
                .batteryCapacity(95)
                .state(BoxState.IDLE)
                .items(new ArrayList<>())
                .build();

        loadingBox = Box.builder()
                .id(2L)
                .txref("BOX-002")
                .weightLimit(new BigDecimal("500.000"))
                .batteryCapacity(80)
                .state(BoxState.LOADING)
                .items(new ArrayList<>())
                .build();

        loadedBox = Box.builder()
                .id(3L)
                .txref("BOX-LOADED")
                .weightLimit(new BigDecimal("200.000"))
                .batteryCapacity(70)
                .state(BoxState.LOADED)
                .items(new ArrayList<>())
                .build();

        lowBatteryBox = Box.builder()
                .id(4L)
                .txref("BOX-LOW-BAT")
                .weightLimit(new BigDecimal("500.000"))
                .batteryCapacity(20)
                .state(BoxState.IDLE)
                .items(new ArrayList<>())
                .build();
    }

    // ─── createBox ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createBox()")
    class CreateBoxTests {

        @Test
        @DisplayName("should create a box successfully")
        void createBox_success() {
            CreateBoxRequest request = new CreateBoxRequest();
            request.setTxref("BOX-NEW");
            request.setWeightLimit(new BigDecimal("300.000"));
            request.setBatteryCapacity(90);

            BoxResponse expected = new BoxResponse();
            when(boxRepository.existsByTxref("BOX-NEW")).thenReturn(false);
            when(mapper.toBoxEntity(request)).thenReturn(idleBox);
            when(boxRepository.save(any())).thenReturn(idleBox);
            when(mapper.toBoxResponse(any())).thenReturn(expected);

            BoxResponse result = boxService.createBox(request);

            assertThat(result).isNotNull();
            verify(boxRepository).save(any());
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when txref already exists")
        void createBox_duplicateTxref_throwsDuplicateResourceException() {
            CreateBoxRequest request = new CreateBoxRequest();
            request.setTxref("BOX-001");
            request.setWeightLimit(new BigDecimal("300.000"));
            request.setBatteryCapacity(90);

            when(boxRepository.existsByTxref("BOX-001")).thenReturn(true);

            assertThatThrownBy(() -> boxService.createBox(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("BOX-001");

            verify(boxRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessRuleViolationException when weight exceeds 500g")
        void createBox_weightExceeds500_throwsBusinessRuleViolation() {
            CreateBoxRequest request = new CreateBoxRequest();
            request.setTxref("BOX-HEAVY");
            request.setWeightLimit(new BigDecimal("500.001"));
            request.setBatteryCapacity(90);

            when(boxRepository.existsByTxref(any())).thenReturn(false);

            assertThatThrownBy(() -> boxService.createBox(request))
                    .isInstanceOf(BusinessRuleViolationException.class);

            verify(boxRepository, never()).save(any());
        }

        @Test
        @DisplayName("should allow weight exactly at 500g limit")
        void createBox_weightExactly500_succeeds() {
            CreateBoxRequest request = new CreateBoxRequest();
            request.setTxref("BOX-MAX");
            request.setWeightLimit(new BigDecimal("500.000"));
            request.setBatteryCapacity(90);

            when(boxRepository.existsByTxref(any())).thenReturn(false);
            when(mapper.toBoxEntity(request)).thenReturn(idleBox);
            when(boxRepository.save(any())).thenReturn(idleBox);
            when(mapper.toBoxResponse(any())).thenReturn(new BoxResponse());

            assertThatCode(() -> boxService.createBox(request)).doesNotThrowAnyException();
        }
    }

    // ─── loadItems ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("loadItems()")
    class LoadItemsTests {

        private LoadItemsRequest buildRequest(String... codes) {
            List<ItemRequest> items = new ArrayList<>();
            for (String code : codes) {
                ItemRequest ir = new ItemRequest();
                ir.setName("Item-" + code);
                ir.setWeight(new BigDecimal("10.000"));
                ir.setCode(code);
                items.add(ir);
            }
            LoadItemsRequest request = new LoadItemsRequest();
            request.setItems(items);
            return request;
        }

        @Test
        @DisplayName("should load items into IDLE box and transition to LOADED")
        void loadItems_idleBox_success() {
            LoadItemsRequest request = buildRequest("ITEM_001");
            when(boxRepository.findByTxrefWithItems("BOX-001")).thenReturn(Optional.of(idleBox));
            when(itemRepository.findExistingCodes(any())).thenReturn(Collections.emptySet());
            when(itemRepository.saveAll(any())).thenReturn(Collections.emptyList());
            when(boxRepository.save(any())).thenReturn(idleBox);
            when(mapper.toItemEntity(any(), any())).thenReturn(
                Item.builder()
                    .name("Item-ITEM_001")
                    .weight(new BigDecimal("10.000")) 
                    .code("ITEM_001")
                    .build()
            );
            when(mapper.toItemResponseList(any())).thenReturn(Collections.emptyList());

            LoadItemsResponse response = boxService.loadItems("BOX-001", request);

            assertThat(response).isNotNull();
            assertThat(idleBox.getState()).isEqualTo(BoxState.LOADED);
        }

        @Test
        @DisplayName("should load items into LOADING box and transition to LOADED")
        void loadItems_loadingBox_success() {
            LoadItemsRequest request = buildRequest("ITEM_002");
            when(boxRepository.findByTxrefWithItems("BOX-002")).thenReturn(Optional.of(loadingBox));
            when(itemRepository.findExistingCodes(any())).thenReturn(Collections.emptySet());
            when(itemRepository.saveAll(any())).thenReturn(Collections.emptyList());
            when(boxRepository.save(any())).thenReturn(loadingBox);
            when(mapper.toItemEntity(any(), any())).thenReturn(
                Item.builder()
                    .name("Item-ITEM_001")
                    .weight(new BigDecimal("10.000"))  
                    .code("ITEM_001")
                    .build()
            );
            when(mapper.toItemResponseList(any())).thenReturn(Collections.emptyList());

            LoadItemsResponse response = boxService.loadItems("BOX-002", request);

            assertThat(response).isNotNull();
            assertThat(loadingBox.getState()).isEqualTo(BoxState.LOADED);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when box does not exist")
        void loadItems_boxNotFound_throwsNotFoundException() {
            LoadItemsRequest request = buildRequest("ITEM_001");
            when(boxRepository.findByTxrefWithItems("GHOST")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> boxService.loadItems("GHOST", request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw BusinessRuleViolationException when battery is below 25%")
        void loadItems_lowBattery_throwsBusinessRuleViolation() {
            LoadItemsRequest request = buildRequest("ITEM_001");
            when(boxRepository.findByTxrefWithItems("BOX-LOW-BAT")).thenReturn(Optional.of(lowBatteryBox));

            assertThatThrownBy(() -> boxService.loadItems("BOX-LOW-BAT", request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("battery");
        }

        @Test
        @DisplayName("should throw BusinessRuleViolationException when box is not in loadable state")
        void loadItems_boxAlreadyLoaded_throwsInvalidStateTransition() {
            LoadItemsRequest request = buildRequest("ITEM_001");
            when(boxRepository.findByTxrefWithItems("BOX-LOADED")).thenReturn(Optional.of(loadedBox));

            assertThatThrownBy(() -> boxService.loadItems("BOX-LOADED", request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("LOADED");
        }

        @Test
        @DisplayName("should throw BusinessRuleViolationException on duplicate codes in request")
        void loadItems_duplicateCodesInRequest_throwsBusinessRuleViolation() {
            LoadItemsRequest request = buildRequest("ITEM_001", "ITEM_001");
            when(boxRepository.findByTxrefWithItems("BOX-001")).thenReturn(Optional.of(idleBox));

            assertThatThrownBy(() -> boxService.loadItems("BOX-001", request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Duplicate");
        }

        @Test
        @DisplayName("should throw BusinessRuleViolationException when code already exists in DB")
        void loadItems_existingCodeInDb_throwsBusinessRuleViolation() {
            LoadItemsRequest request = buildRequest("EXISTING_001");
            when(boxRepository.findByTxrefWithItems("BOX-001")).thenReturn(Optional.of(idleBox));
            when(itemRepository.findExistingCodes(any())).thenReturn(Set.of("EXISTING_001"));

            assertThatThrownBy(() -> boxService.loadItems("BOX-001", request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("EXISTING_001");
        }

        @Test
        @DisplayName("should throw BusinessRuleViolationException when incoming weight exceeds remaining capacity")
        void loadItems_weightExceedsCapacity_throwsBusinessRuleViolation() {
            // Box has 500g limit, add a 100g item already loaded
            Item existing = Item.builder()
                    .weight(new BigDecimal("450.000"))
                    .code("EXISTING")
                    .build();
            idleBox.getItems().add(existing);

            // Try to add 100g more — only 50g remaining
            LoadItemsRequest request = buildRequest("OVER_WEIGHT");
            request.getItems().get(0).setWeight(new BigDecimal("100.000"));

            when(boxRepository.findByTxrefWithItems("BOX-001")).thenReturn(Optional.of(idleBox));
            when(itemRepository.findExistingCodes(any())).thenReturn(Collections.emptySet());

            assertThatThrownBy(() -> boxService.loadItems("BOX-001", request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Weight limit exceeded"); 
        }

        @Test
        @DisplayName("should return cached response on idempotent replay")
        void loadItems_idempotentReplay_returnsCachedResponse() {
            LoadItemsRequest request = buildRequest("ITEM_001");
            request.setIdempotencyKey("idem-key-123");

            LoadItemsResponse cached = LoadItemsResponse.builder()
                    .boxTxref("BOX-001")
                    .idempotent(true)
                    .build();

            when(idempotencyService.acquire("idem-key-123")).thenReturn(Optional.of(cached));

            LoadItemsResponse result = boxService.loadItems("BOX-001", request);

            assertThat(result.isIdempotent()).isTrue();
            verify(boxRepository, never()).findByTxrefWithItems(any());
        }

        @Test
        @DisplayName("should release lock on failure when idempotency key is present")
        void loadItems_exceptionDuringExecution_releasesLock() {
            LoadItemsRequest request = buildRequest("ITEM_001");
            request.setIdempotencyKey("idem-key-fail");

            when(idempotencyService.acquire("idem-key-fail")).thenReturn(Optional.empty());
            when(boxRepository.findByTxrefWithItems("BOX-001")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> boxService.loadItems("BOX-001", request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(idempotencyService).commit("idem-key-fail", null);
        }
    }

    // ─── getLoadedItems ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLoadedItems()")
    class GetLoadedItemsTests {

        @Test
        @DisplayName("should return items for a valid box txref")
        void getLoadedItems_validTxref_returnsItems() {
            ItemResponse ir = new ItemResponse();
            when(boxRepository.existsByTxref("BOX-001")).thenReturn(true);
            when(itemRepository.findByBoxTxref("BOX-001")).thenReturn(List.of(new Item()));
            when(mapper.toItemResponseList(any())).thenReturn(List.of(ir));

            List<ItemResponse> result = boxService.getLoadedItems("BOX-001");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should return empty list when box has no items")
        void getLoadedItems_noItems_returnsEmptyList() {
            when(boxRepository.existsByTxref("BOX-001")).thenReturn(true);
            when(itemRepository.findByBoxTxref("BOX-001")).thenReturn(Collections.emptyList());
            when(mapper.toItemResponseList(any())).thenReturn(Collections.emptyList());

            List<ItemResponse> result = boxService.getLoadedItems("BOX-001");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when box does not exist")
        void getLoadedItems_boxNotFound_throwsNotFoundException() {
            when(boxRepository.existsByTxref("GHOST")).thenReturn(false);

            assertThatThrownBy(() -> boxService.getLoadedItems("GHOST"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── getAvailableBoxes ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAvailableBoxes()")
    class GetAvailableBoxesTests {

        @Test
        @DisplayName("should return boxes in IDLE or LOADING state with battery >= 25%")
        void getAvailableBoxes_returnsEligibleBoxes() {
            BoxResponse br = new BoxResponse();
            when(boxRepository.findAvailableForLoading(eq(25), anyList()))
                    .thenReturn(List.of(idleBox, loadingBox));
            when(mapper.toBoxResponse(any())).thenReturn(br);

            List<BoxResponse> result = boxService.getAvailableBoxes();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no boxes are available")
        void getAvailableBoxes_noBoxes_returnsEmpty() {
            when(boxRepository.findAvailableForLoading(eq(25), anyList()))
                    .thenReturn(Collections.emptyList());

            List<BoxResponse> result = boxService.getAvailableBoxes();

            assertThat(result).isEmpty();
        }
    }

    // ─── getBatteryLevel ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getBatteryLevel()")
    class GetBatteryLevelTests {

        @Test
        @DisplayName("should return battery level for a valid box")
        void getBatteryLevel_validBox_returnsBattery() {
            BatteryResponse br = new BatteryResponse();
            when(boxRepository.findByTxref("BOX-001")).thenReturn(Optional.of(idleBox));
            when(mapper.toBatteryResponse(idleBox)).thenReturn(br);

            BatteryResponse result = boxService.getBatteryLevel("BOX-001");

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when box does not exist")
        void getBatteryLevel_boxNotFound_throwsNotFoundException() {
            when(boxRepository.findByTxref("GHOST")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> boxService.getBatteryLevel("GHOST"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
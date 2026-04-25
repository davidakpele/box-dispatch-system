package com.boxdispatch.ServiceTest;

import com.boxdispatch.Enums.BoxState;
import com.boxdispatch.Exceptions.BusinessRuleViolationException;
import com.boxdispatch.Exceptions.DuplicateResourceException;
import com.boxdispatch.Exceptions.ResourceNotFoundException;
import com.boxdispatch.Interface.IBoxService;
import com.boxdispatch.Models.Box;
import com.boxdispatch.Models.Item;
import com.boxdispatch.Payloads.CreateBoxRequest;
import com.boxdispatch.Payloads.ItemRequest;
import com.boxdispatch.Payloads.LoadItemsRequest;
import com.boxdispatch.Repositories.BoxRepository;
import com.boxdispatch.Repositories.ItemRepository;
import com.boxdispatch.Responses.BoxResponse;
import com.boxdispatch.Responses.ItemResponse;
import com.boxdispatch.Responses.LoadItemsResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests using H2 in-memory database.
 * Requires src/test/resources/application-test.yml with H2 + Redis mock config.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("BoxService Integration Tests (H2)")
public class BoxServiceIntegrationTest {

    @Autowired private IBoxService    boxService;
    @Autowired private BoxRepository  boxRepository;
    @Autowired private ItemRepository itemRepository;

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Box saveBox(String txref, int battery, BoxState state, BigDecimal limit) {
        return boxRepository.save(Box.builder()
                .txref(txref)
                .weightLimit(limit)
                .batteryCapacity(battery)
                .state(state)
                .build());
    }

    private LoadItemsRequest buildLoadRequest(String code, BigDecimal weight) {
        ItemRequest ir = new ItemRequest();
        ir.setName("Item-" + code);
        ir.setWeight(weight);
        ir.setCode(code);

        LoadItemsRequest req = new LoadItemsRequest();
        req.setItems(List.of(ir));
        return req;
    }

    private CreateBoxRequest buildCreateRequest(String txref, BigDecimal limit, int battery) {
        CreateBoxRequest req = new CreateBoxRequest();
        req.setTxref(txref);
        req.setWeightLimit(limit);
        req.setBatteryCapacity(battery);
        return req;
    }

    // ─── createBox integration ─────────────────────────────────────────────────

    @Nested
    @DisplayName("createBox() - DB")
    class CreateBoxIntegration {

        @Test
        @DisplayName("should persist box to database")
        void createBox_persistsToDb() {
            BoxResponse response = boxService.createBox(
                    buildCreateRequest("INT-BOX-001", new BigDecimal("400.000"), 80));

            assertThat(response.getTxref()).isEqualTo("INT-BOX-001");
            assertThat(boxRepository.existsByTxref("INT-BOX-001")).isTrue();
        }

        @Test
        @DisplayName("should reject duplicate txref at DB level")
        void createBox_duplicateTxref_throwsDuplicateResourceException() {
            saveBox("INT-DUP", 80, BoxState.IDLE, new BigDecimal("300.000"));

            assertThatThrownBy(() ->
                    boxService.createBox(buildCreateRequest("INT-DUP", new BigDecimal("200.000"), 60)))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    // ─── loadItems integration ─────────────────────────────────────────────────

    @Nested
    @DisplayName("loadItems() - DB")
    class LoadItemsIntegration {

        @Test
        @DisplayName("should persist items and transition box to LOADED")
        void loadItems_persistsItemsAndUpdatesState() {
            saveBox("INT-LOAD-001", 80, BoxState.IDLE, new BigDecimal("500.000"));

            LoadItemsResponse response = boxService.loadItems(
                    "INT-LOAD-001",
                    buildLoadRequest("INT_ITEM_001", new BigDecimal("100.000")));

            assertThat(response.getBoxState()).isEqualTo(BoxState.LOADED);
            assertThat(response.getItemsLoaded()).isEqualTo(1);

            Box saved = boxRepository.findByTxref("INT-LOAD-001").orElseThrow();
            assertThat(saved.getState()).isEqualTo(BoxState.LOADED);

            List<Item> items = itemRepository.findByBoxTxref("INT-LOAD-001");
            assertThat(items).hasSize(1);
            assertThat(items.get(0).getCode()).isEqualTo("INT_ITEM_001");
        }

        @Test
        @DisplayName("should correctly calculate remaining capacity after loading")
        void loadItems_remainingCapacityIsCorrect() {
            saveBox("INT-CAP-001", 80, BoxState.IDLE, new BigDecimal("200.000"));

            LoadItemsResponse response = boxService.loadItems(
                    "INT-CAP-001",
                    buildLoadRequest("INT_ITEM_CAP", new BigDecimal("75.000")));

            assertThat(response.getRemainingCapacity())
                    .isEqualByComparingTo(new BigDecimal("125.000"));
            assertThat(response.getTotalWeight())
                    .isEqualByComparingTo(new BigDecimal("75.000"));
        }

        @Test
        @DisplayName("should reject loading into LOADED box")
        void loadItems_alreadyLoaded_throwsBusinessRuleViolation() {
            saveBox("INT-LOADED-001", 80, BoxState.LOADED, new BigDecimal("500.000"));

            assertThatThrownBy(() ->
                    boxService.loadItems("INT-LOADED-001",
                            buildLoadRequest("INT_ITEM_X", new BigDecimal("50.000"))))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("should reject loading when battery is below 25%")
        void loadItems_lowBattery_throwsBusinessRuleViolation() {
            saveBox("INT-LOWBAT-001", 20, BoxState.IDLE, new BigDecimal("500.000"));

            assertThatThrownBy(() ->
                    boxService.loadItems("INT-LOWBAT-001",
                            buildLoadRequest("INT_ITEM_BAT", new BigDecimal("50.000"))))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("should reject loading when weight exceeds remaining capacity")
        void loadItems_overweight_throwsBusinessRuleViolation() {
            saveBox("INT-OVER-001", 80, BoxState.IDLE, new BigDecimal("50.000"));

            assertThatThrownBy(() ->
                    boxService.loadItems("INT-OVER-001",
                            buildLoadRequest("INT_ITEM_HEAVY", new BigDecimal("100.000"))))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("should reject duplicate item code across different requests")
        void loadItems_duplicateCodeAcrossRequests_throwsBusinessRuleViolation() {
            saveBox("INT-DUP-CODE", 80, BoxState.IDLE, new BigDecimal("500.000"));
            boxService.loadItems("INT-DUP-CODE",
                    buildLoadRequest("UNIQUE_CODE_001", new BigDecimal("50.000")));

            // Reset to IDLE for second load attempt
            Box box = boxRepository.findByTxref("INT-DUP-CODE").orElseThrow();
            box.setState(BoxState.IDLE);
            boxRepository.save(box);

            assertThatThrownBy(() ->
                    boxService.loadItems("INT-DUP-CODE",
                            buildLoadRequest("UNIQUE_CODE_001", new BigDecimal("50.000"))))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("UNIQUE_CODE_001");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for unknown txref")
        void loadItems_unknownBox_throwsNotFoundException() {
            assertThatThrownBy(() ->
                    boxService.loadItems("GHOST-BOX",
                            buildLoadRequest("INT_ITEM_G", new BigDecimal("10.000"))))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── getLoadedItems integration ────────────────────────────────────────────

    @Nested
    @DisplayName("getLoadedItems() - DB")
    class GetLoadedItemsIntegration {

        @Test
        @DisplayName("should return all items for a given box txref")
        void getLoadedItems_returnsCorrectItems() {
            saveBox("INT-ITEMS-001", 80, BoxState.IDLE, new BigDecimal("500.000"));
            boxService.loadItems("INT-ITEMS-001",
                    buildLoadRequest("ITEM_ALPHA_001", new BigDecimal("50.000")));

            List<ItemResponse> items = boxService.getLoadedItems("INT-ITEMS-001");

            assertThat(items).hasSize(1);
            assertThat(items.get(0).getCode()).isEqualTo("ITEM_ALPHA_001");
        }

        @Test
        @DisplayName("should return empty list when box has no items")
        void getLoadedItems_emptyBox_returnsEmpty() {
            saveBox("INT-EMPTY-001", 80, BoxState.IDLE, new BigDecimal("500.000"));

            List<ItemResponse> items = boxService.getLoadedItems("INT-EMPTY-001");

            assertThat(items).isEmpty();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for unknown txref")
        void getLoadedItems_unknownBox_throwsNotFoundException() {
            assertThatThrownBy(() -> boxService.getLoadedItems("GHOST-BOX"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── getAvailableBoxes integration ────────────────────────────────────────

    @Nested
    @DisplayName("getAvailableBoxes() - DB")
    class GetAvailableBoxesIntegration {

        @Test
        @DisplayName("should return only IDLE and LOADING boxes with battery >= 25%")
        void getAvailableBoxes_filtersCorrectly() {
            saveBox("INT-AVAIL-IDLE",     80, BoxState.IDLE,       new BigDecimal("500.000"));
            saveBox("INT-AVAIL-LOADING",  60, BoxState.LOADING,    new BigDecimal("500.000"));
            saveBox("INT-AVAIL-LOADED",   70, BoxState.LOADED,     new BigDecimal("500.000"));
            saveBox("INT-AVAIL-LOW-BAT",  15, BoxState.IDLE,       new BigDecimal("500.000"));

            List<BoxResponse> available = boxService.getAvailableBoxes();

            List<String> txrefs = available.stream().map(BoxResponse::getTxref).toList();
            assertThat(txrefs).contains("INT-AVAIL-IDLE", "INT-AVAIL-LOADING");
            assertThat(txrefs).doesNotContain("INT-AVAIL-LOADED", "INT-AVAIL-LOW-BAT");
        }

        @Test
        @DisplayName("should return empty list when no boxes are eligible")
        void getAvailableBoxes_noneEligible_returnsEmpty() {
            saveBox("INT-DELIVERING", 60, BoxState.DELIVERING, new BigDecimal("500.000"));

            List<BoxResponse> available = boxService.getAvailableBoxes();

            List<String> txrefs = available.stream().map(BoxResponse::getTxref).toList();
            assertThat(txrefs).doesNotContain("INT-DELIVERING");
        }
    }

    // ─── getBatteryLevel integration ──────────────────────────────────────────

    @Nested
    @DisplayName("getBatteryLevel() - DB")
    class GetBatteryIntegration {

        @Test
        @DisplayName("should return correct battery level from DB")
        void getBatteryLevel_returnsCorrectValue() {
            saveBox("INT-BAT-001", 73, BoxState.IDLE, new BigDecimal("500.000"));

            var battery = boxService.getBatteryLevel("INT-BAT-001");

            assertThat(battery.getBatteryCapacity()).isEqualTo(73);
            assertThat(battery.getTxref()).isEqualTo("INT-BAT-001");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for unknown txref")
        void getBatteryLevel_unknownBox_throwsNotFoundException() {
            assertThatThrownBy(() -> boxService.getBatteryLevel("GHOST-BOX"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
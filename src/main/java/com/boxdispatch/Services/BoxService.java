package com.boxdispatch.Services;

import com.boxdispatch.Components.Mapper.BoxDispatchMapper;
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
import com.boxdispatch.Responses.BatteryResponse;
import com.boxdispatch.Responses.BoxResponse;
import com.boxdispatch.Responses.ItemResponse;
import com.boxdispatch.Responses.LoadItemsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BoxService implements IBoxService {

    private static final int        MIN_BATTERY_FOR_LOADING = 25;
    private static final BigDecimal MAX_WEIGHT              = new BigDecimal("500.000");

    private final BoxRepository      boxRepository;
    private final ItemRepository     itemRepository;
    private final BoxDispatchMapper  mapper;
    private final IdempotencyService idempotencyService;

    @Override
    @Transactional
    public BoxResponse createBox(CreateBoxRequest request) {
        if (boxRepository.existsByTxref(request.getTxref()))
            throw new DuplicateResourceException("Box with txref '" + request.getTxref() + "' already exists");

        if (request.getWeightLimit().compareTo(MAX_WEIGHT) > 0)
            throw new BusinessRuleViolationException(
                    "Weight limit cannot exceed 500g. Provided: " + request.getWeightLimit(),
                    "WEIGHT_LIMIT_TOO_HIGH");

        return mapper.toBoxResponse(boxRepository.save(mapper.toBoxEntity(request)));
    }

    @Override
    @Transactional
    public LoadItemsResponse loadItems(String txref, LoadItemsRequest request) {
        String  key               = request.getIdempotencyKey();
        boolean idempotencyActive = key != null && !key.isBlank();

        if (idempotencyActive) {
            Optional<LoadItemsResponse> cached = idempotencyService.acquire(key);
            if (cached.isPresent()) return cached.get();
        }

        LoadItemsResponse response;
        try {
            response = executeLoadItems(txref, request);
        } catch (RuntimeException ex) {
            if (idempotencyActive) idempotencyService.commit(key, null);
            throw ex;
        }

        if (idempotencyActive) idempotencyService.commit(key, response);
        return response;
    }
    
    private LoadItemsResponse executeLoadItems(String txref, LoadItemsRequest request) {
        Box box = boxRepository.findByTxrefWithItems(txref)
                .orElseThrow(() -> ResourceNotFoundException.box(txref));

        if (box.getBatteryCapacity() < MIN_BATTERY_FOR_LOADING)
            throw BusinessRuleViolationException.lowBattery(box.getBatteryCapacity());

        if (!box.getState().canTransitionTo(BoxState.LOADING))
            throw BusinessRuleViolationException.invalidStateTransition(box.getState(), BoxState.LOADING);

        List<String> requestCodes = request.getItems().stream().map(ItemRequest::getCode).toList();

        if (requestCodes.stream().distinct().count() < requestCodes.size())
            throw new BusinessRuleViolationException("Duplicate item codes in request.", "DUPLICATE_ITEM_CODE_IN_REQUEST");

        Set<String> existingCodes = itemRepository.findExistingCodes(new HashSet<>(requestCodes));
        if (!existingCodes.isEmpty())
            throw BusinessRuleViolationException.duplicateItemCode(String.join(", ", existingCodes));

        BigDecimal incomingWeight = request.getItems().stream()
                .map(ItemRequest::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (incomingWeight.compareTo(box.getRemainingCapacity()) > 0)
            throw BusinessRuleViolationException.weightLimitExceeded(
                    box.getRemainingCapacity().doubleValue(), incomingWeight.doubleValue());

        if (box.getState() == BoxState.IDLE) box.setState(BoxState.LOADING);

        List<Item> newItems = request.getItems().stream()
                .map(ir -> mapper.toItemEntity(ir, box)).toList();

        box.getItems().addAll(newItems);
        itemRepository.saveAll(newItems);
        box.setState(BoxState.LOADED);
        boxRepository.save(box);

        return LoadItemsResponse.builder()
                .boxTxref(txref)
                .boxState(box.getState())
                .itemsLoaded(newItems.size())
                .totalWeight(box.getTotalItemWeight())
                .remainingCapacity(box.getRemainingCapacity())
                .loadedItems(mapper.toItemResponseList(newItems))
                .idempotent(false)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemResponse> getLoadedItems(String txref) {
        if (!boxRepository.existsByTxref(txref)) throw ResourceNotFoundException.box(txref);
        return mapper.toItemResponseList(itemRepository.findByBoxTxref(txref));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoxResponse> getAvailableBoxes() {
        return boxRepository.findAvailableForLoading(MIN_BATTERY_FOR_LOADING,
                        List.of(BoxState.IDLE, BoxState.LOADING))
                .stream().map(mapper::toBoxResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BatteryResponse getBatteryLevel(String txref) {
        return mapper.toBatteryResponse(boxRepository.findByTxref(txref)
                .orElseThrow(() -> ResourceNotFoundException.box(txref)));
    }
}
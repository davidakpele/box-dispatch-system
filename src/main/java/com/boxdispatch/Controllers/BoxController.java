package com.boxdispatch.Controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boxdispatch.DTO.BoxApiResponse;
import com.boxdispatch.Interface.IBoxService;
import com.boxdispatch.Payloads.CreateBoxRequest;
import com.boxdispatch.Payloads.LoadItemsRequest;
import com.boxdispatch.Responses.BatteryResponse;
import com.boxdispatch.Responses.BoxResponse;
import com.boxdispatch.Responses.ItemResponse;
import com.boxdispatch.Responses.LoadItemsResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/boxes")
@Slf4j
public class BoxController {

    private final IBoxService boxService;

    public BoxController(IBoxService boxService) {
        this.boxService = boxService;
    }

    /**
     * POST /api/v1/boxes
     * Creates a new box.
     */
    @PostMapping
    public ResponseEntity<BoxApiResponse<BoxResponse>> createBox(@Valid @RequestBody CreateBoxRequest request) {
        BoxResponse box = boxService.createBox(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BoxApiResponse.success("Box created successfully", box));
    }

    /**
     * POST /api/v1/boxes/{txref}/load
     * Loads items into a box.
     */
    @PostMapping("/{txref}/load")
    @PreAuthorize("hasAnyRole('USER','ADMIN') and @security.isOwnerOrAdmin(#userId)")
    public ResponseEntity<BoxApiResponse<LoadItemsResponse>> loadItems(
            @PathVariable String txref,
            @Valid @RequestBody LoadItemsRequest request) {
        LoadItemsResponse response = boxService.loadItems(txref, request);
        HttpStatus status = response.isIdempotent() ? HttpStatus.OK : HttpStatus.CREATED;

        return ResponseEntity.status(status)
                .body(BoxApiResponse.success("Items loaded successfully", response));
    }

    /**
     * GET /api/v1/boxes/{txref}/items
     * Returns items currently loaded in the box.
     */
    @GetMapping("/{txref}/items")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN') and @security.isOwnerOrAdmin(#userId)")
    public ResponseEntity<BoxApiResponse<List<ItemResponse>>> getLoadedItems(
            @PathVariable String txref) {
        List<ItemResponse> items = boxService.getLoadedItems(txref);
        return ResponseEntity.ok(BoxApiResponse.success(items));
    }


    /**
     * GET /api/v1/boxes/available
     * Returns all boxes available for loading.
     */
    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('USER','ADMIN') and @security.isOwnerOrAdmin(#userId)")
    public ResponseEntity<BoxApiResponse<List<BoxResponse>>> getAvailableBoxes() {
        List<BoxResponse> boxes = boxService.getAvailableBoxes();
        return ResponseEntity.ok(BoxApiResponse.success(boxes));
    }

    /**
     * GET /api/v1/boxes/{txref}/battery
     * Returns battery status of a box.
     */
    @GetMapping("/{txref}/battery")
    @PreAuthorize("hasAnyRole('USER','ADMIN') and @security.isOwnerOrAdmin(#userId)")
    public ResponseEntity<BoxApiResponse<BatteryResponse>> getBattery(
            @PathVariable String txref) {
        BatteryResponse battery = boxService.getBatteryLevel(txref);
        return ResponseEntity.ok(BoxApiResponse.success(battery));
    }
}

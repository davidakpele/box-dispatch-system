package com.boxdispatch.Payloads;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
 
import java.util.List;
 
@Data
public class LoadItemsRequest {
 
    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<ItemRequest> items;
    private String idempotencyKey;
}
 
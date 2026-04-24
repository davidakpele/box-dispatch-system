package com.boxdispatch.Responses;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import com.boxdispatch.Enums.BoxState;
 
@Data
@Builder
public class LoadItemsResponse {
    private String boxTxref;
    private BoxState boxState;
    private int itemsLoaded;
    private BigDecimal totalWeight;
    private BigDecimal remainingCapacity;
    private List<ItemResponse> loadedItems;
    private boolean idempotent; 
}

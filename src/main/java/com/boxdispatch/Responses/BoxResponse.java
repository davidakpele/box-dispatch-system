package com.boxdispatch.Responses;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import com.boxdispatch.Enums.BoxState;
 
@Data
@Builder
public class BoxResponse {
    private Long id;
    private String txref;
    private BigDecimal weightLimit;
    private Integer batteryCapacity;
    private BoxState state;
    private BigDecimal totalLoadedWeight;
    private BigDecimal remainingCapacity;
    private int itemCount;
    private Instant createdAt;
    private Instant updatedAt;
}
 
package com.boxdispatch.Responses;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ItemResponse {
    private Long id;
    private String name;
    private BigDecimal weight;
    private String code;
    private String boxTxref;
    private Instant createdAt;
}
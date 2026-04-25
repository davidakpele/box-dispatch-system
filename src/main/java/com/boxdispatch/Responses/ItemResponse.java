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


    public ItemResponse() {
    }

    public ItemResponse(Long id, String name, BigDecimal weight, String code, String boxTxref, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.code = code;
        this.boxTxref = boxTxref;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getWeight() {
        return this.weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getBoxTxref() {
        return this.boxTxref;
    }

    public void setBoxTxref(String boxTxref) {
        this.boxTxref = boxTxref;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

}
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

    public BoxResponse() {
    }

    public BoxResponse(Long id, String txref, BigDecimal weightLimit, Integer batteryCapacity, BoxState state, BigDecimal totalLoadedWeight, BigDecimal remainingCapacity, int itemCount, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.txref = txref;
        this.weightLimit = weightLimit;
        this.batteryCapacity = batteryCapacity;
        this.state = state;
        this.totalLoadedWeight = totalLoadedWeight;
        this.remainingCapacity = remainingCapacity;
        this.itemCount = itemCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTxref() {
        return this.txref;
    }

    public void setTxref(String txref) {
        this.txref = txref;
    }

    public BigDecimal getWeightLimit() {
        return this.weightLimit;
    }

    public void setWeightLimit(BigDecimal weightLimit) {
        this.weightLimit = weightLimit;
    }

    public Integer getBatteryCapacity() {
        return this.batteryCapacity;
    }

    public void setBatteryCapacity(Integer batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
    }

    public BoxState getState() {
        return this.state;
    }

    public void setState(BoxState state) {
        this.state = state;
    }

    public BigDecimal getTotalLoadedWeight() {
        return this.totalLoadedWeight;
    }

    public void setTotalLoadedWeight(BigDecimal totalLoadedWeight) {
        this.totalLoadedWeight = totalLoadedWeight;
    }

    public BigDecimal getRemainingCapacity() {
        return this.remainingCapacity;
    }

    public void setRemainingCapacity(BigDecimal remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    public int getItemCount() {
        return this.itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

}
 
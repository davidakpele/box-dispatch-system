package com.boxdispatch.Responses;

import lombok.Builder;
import lombok.Data;
 
@Data
@Builder
public class BatteryResponse {
    private String txref;
    private Integer batteryCapacity;
    private String status; // "CRITICAL", "LOW", "NORMAL", "FULL"


    public BatteryResponse() {
    }

    public BatteryResponse(String txref, Integer batteryCapacity, String status) {
        this.txref = txref;
        this.batteryCapacity = batteryCapacity;
        this.status = status;
    }

    public String getTxref() {
        return this.txref;
    }

    public void setTxref(String txref) {
        this.txref = txref;
    }

    public Integer getBatteryCapacity() {
        return this.batteryCapacity;
    }

    public void setBatteryCapacity(Integer batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
package com.boxdispatch.Responses;

import lombok.Builder;
import lombok.Data;
 
@Data
@Builder
public class BatteryResponse {
    private String txref;
    private Integer batteryCapacity;
    private String status; // "CRITICAL", "LOW", "NORMAL", "FULL"
}
package com.boxdispatch.Payloads;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
 
@Data
public class CreateBoxRequest {
 
    @NotBlank(message = "txref is required")
    @Size(max = 20, message = "txref must not exceed 20 characters")
    private String txref;
 
    @NotNull(message = "weightLimit is required")
    @DecimalMin(value = "0.001", message = "weightLimit must be greater than 0")
    @DecimalMax(value = "500.000", message = "weightLimit must not exceed 500g")
    @Digits(integer = 5, fraction = 3, message = "weightLimit must have at most 5 integer digits and 3 decimal places")
    private BigDecimal weightLimit;
 
    @NotNull(message = "batteryCapacity is required")
    @Min(value = 0, message = "batteryCapacity must be between 0 and 100")
    @Max(value = 100, message = "batteryCapacity must be between 0 and 100")
    private Integer batteryCapacity;
}
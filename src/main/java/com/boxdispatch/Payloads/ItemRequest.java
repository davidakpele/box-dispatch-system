package com.boxdispatch.Payloads;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ItemRequest {
 
    @NotBlank(message = "Item name is required")
    @Pattern(
        regexp = "^[a-zA-Z0-9\\-_]+$",
        message = "Item name may only contain letters, numbers, hyphens, and underscores"
    )
    @Size(max = 100, message = "Item name must not exceed 100 characters")
    private String name;
 
    @NotNull(message = "Item weight is required")
    @DecimalMin(value = "0.001", message = "Item weight must be greater than 0")
    @Digits(integer = 5, fraction = 3, message = "Item weight must have at most 5 integer digits and 3 decimal places")
    private BigDecimal weight;
 
    @NotBlank(message = "Item code is required")
    @Pattern(
        regexp = "^[A-Z0-9_]+$",
        message = "Item code may only contain uppercase letters, numbers, and underscores"
    )
    @Size(max = 100, message = "Item code must not exceed 100 characters")
    private String code;
}
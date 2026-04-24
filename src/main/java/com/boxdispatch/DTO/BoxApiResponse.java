package com.boxdispatch.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
 
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BoxApiResponse <T> {

    private boolean success;
    private String message;
    private T data;
    private String errorCode;
 
    @Builder.Default
    private Instant timestamp = Instant.now();
 
    public static <T> BoxApiResponse<T> success(T data) {
        return BoxApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }
 
    public static <T> BoxApiResponse<T> success(String message, T data) {
        return BoxApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
 
    public static <T> BoxApiResponse<T> error(String message, String errorCode) {
        return BoxApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}
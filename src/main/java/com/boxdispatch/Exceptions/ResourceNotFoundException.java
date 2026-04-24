package com.boxdispatch.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
 
    public static ResourceNotFoundException box(String txref) {
        return new ResourceNotFoundException("Box not found with txref: " + txref);
    }
}
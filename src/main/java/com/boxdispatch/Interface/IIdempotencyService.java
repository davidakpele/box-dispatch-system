package com.boxdispatch.Interface;

import java.util.Optional;
import com.boxdispatch.Responses.LoadItemsResponse;

public interface IIdempotencyService {
    Optional<LoadItemsResponse> acquire(String key);
    void commit(String key, LoadItemsResponse response);
}

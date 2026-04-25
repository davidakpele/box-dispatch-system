package com.boxdispatch.Interface;

import java.util.List;
import com.boxdispatch.Payloads.CreateBoxRequest;
import com.boxdispatch.Payloads.LoadItemsRequest;
import com.boxdispatch.Responses.BatteryResponse;
import com.boxdispatch.Responses.BoxResponse;
import com.boxdispatch.Responses.ItemResponse;
import com.boxdispatch.Responses.LoadItemsResponse;

public interface IBoxService {
    /**
     * Creates a new box with the provided configuration.
     *
     * @param request the creation request
     * @return the persisted box response
     */
    BoxResponse createBox(CreateBoxRequest request);
 
    /**
     * Loads items into a box identified by txref.
     * This operation is transactional and enforces all business rules.
     *
     * @param txref   the box identifier
     * @param request the items to load
     * @return summary of the loading operation
     */
    LoadItemsResponse loadItems(String txref, LoadItemsRequest request);
 
    /**
     * Returns all items currently loaded in the specified box.
     *
     * @param txref the box identifier
     * @return list of loaded items
     */
    List<ItemResponse> getLoadedItems(String txref);
 
    /**
     * Returns all boxes currently available for loading.
     * Availability criteria: battery >= 25% AND state is IDLE or LOADING.
     *
     * @return list of available boxes
     */
    List<BoxResponse> getAvailableBoxes();
 
    /**
     * Returns the battery level and status of a box.
     *
     * @param txref the box identifier
     * @return battery information
     */
    BatteryResponse getBatteryLevel(String txref);
}
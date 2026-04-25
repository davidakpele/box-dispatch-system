package com.boxdispatch.Components.Mapper;

import org.springframework.stereotype.Component;
import com.boxdispatch.Models.Box;
import com.boxdispatch.Models.Item;
import com.boxdispatch.Payloads.CreateBoxRequest;
import com.boxdispatch.Payloads.ItemRequest;
import com.boxdispatch.Responses.BatteryResponse;
import com.boxdispatch.Responses.BoxResponse;
import com.boxdispatch.Responses.ItemResponse;
import java.util.List;
 
@Component
public class BoxDispatchMapper {
 
    public Box toBoxEntity(CreateBoxRequest request) {
        return Box.builder()
                .txref(request.getTxref())
                .weightLimit(request.getWeightLimit())
                .batteryCapacity(request.getBatteryCapacity())
                .build();
    }
 
    public BoxResponse toBoxResponse(Box box) {
        return BoxResponse.builder()
                .id(box.getId())
                .txref(box.getTxref())
                .weightLimit(box.getWeightLimit())
                .batteryCapacity(box.getBatteryCapacity())
                .state(box.getState())
                .totalLoadedWeight(box.getTotalItemWeight())
                .remainingCapacity(box.getRemainingCapacity())
                .itemCount(box.getItems().size())
                .createdAt(box.getCreatedAt())
                .updatedAt(box.getUpdatedAt())
                .build();
    }
 
    public Item toItemEntity(ItemRequest request, Box box) {
        return Item.builder()
                .name(request.getName())
                .weight(request.getWeight())
                .code(request.getCode())
                .box(box)
                .build();
    }
 
    public ItemResponse toItemResponse(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .weight(item.getWeight())
                .code(item.getCode())
                .boxTxref(item.getBox().getTxref())
                .createdAt(item.getCreatedAt())
                .build();
    }
 
    public List<ItemResponse> toItemResponseList(List<Item> items) {
        return items.stream()
                .map(this::toItemResponse)
                .toList();
    }
 
    public BatteryResponse toBatteryResponse(Box box) {
        int battery = box.getBatteryCapacity();
        String status;
        if (battery <= 10) {
            status = "CRITICAL";
        } else if (battery < 25) {
            status = "LOW";
        } else if (battery < 75) {
            status = "NORMAL";
        } else {
            status = "FULL";
        }
        return BatteryResponse.builder()
                .txref(box.getTxref())
                .batteryCapacity(battery)
                .status(status)
                .build();
    }
}
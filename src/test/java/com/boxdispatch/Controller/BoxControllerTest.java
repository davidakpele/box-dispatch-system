// package com.boxdispatch.Controller;

// import com.boxdispatch.Controllers.BoxController;
// import com.boxdispatch.Enums.BoxState;
// import com.boxdispatch.Exceptions.BusinessRuleViolationException;
// import com.boxdispatch.Exceptions.DuplicateResourceException;
// import com.boxdispatch.Exceptions.ResourceNotFoundException;
// import com.boxdispatch.Interface.IBoxService;
// import com.boxdispatch.Responses.BatteryResponse;
// import com.boxdispatch.Responses.BoxResponse;
// import com.boxdispatch.Responses.ItemResponse;
// import com.boxdispatch.Responses.LoadItemsResponse;
// import com.fasterxml.jackson.databind.ObjectMapper;

// import lombok.experimental.Wither;

// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Nested;
// import org.junit.jupiter.api.Test;
// import org.mockito.Mock;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.http.MediaType;
// import org.springframework.security.test.context.support.WithMockUser;
// import org.springframework.test.web.servlet.MockMvc;

// import java.math.BigDecimal;
// import java.util.List;
// import java.util.Map;

// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.eq;
// import static org.mockito.Mockito.when;
// import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest(BoxController.class)
// @DisplayName("BoxController MockMvc Tests")
// public class BoxControllerTest {

//     @Autowired private MockMvc       mockMvc;
//     @Autowired private ObjectMapper  objectMapper;
//     @Mock  private IBoxService   boxService;

//     // ─── POST /api/boxes ───────────────────────────────────────────────────────

//     @Nested
//     @DisplayName("POST /api/boxes")
//     class CreateBoxTests {

//         @Test
//         @Wither(roles = "USER")
//         @DisplayName("should return 201 when box is created successfully")
//         void createBox_validRequest_returns201() throws Exception {
//             BoxResponse response = new BoxResponse();
//             response.setTxref("BOX-NEW");
//             response.setState(BoxState.IDLE);

//             when(boxService.createBox(any())).thenReturn(response);

//             mockMvc.perform(post("/api/boxes")
//                             .with(csrf())
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(Map.of(
//                                     "txref", "BOX-NEW",
//                                     "weightLimit", 300.000,
//                                     "batteryCapacity", 90
//                             ))))
//                     .andExpect(status().isCreated())
//                     .andExpect(jsonPath("$.success").value(true))
//                     .andExpect(jsonPath("$.data.txref").value("BOX-NEW"));
//         }

//         @Test
//         @Wither(roles = "USER")
//         @DisplayName("should return 400 when txref is missing")
//         void createBox_missingTxref_returns400() throws Exception {
//             mockMvc.perform(post("/api/boxes")
//                             .with(csrf())
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(Map.of(
//                                     "weightLimit", 300.000,
//                                     "batteryCapacity", 90
//                             ))))
//                     .andExpect(status().isBadRequest());
//         }
        

//         @Test
//         @Wither(roles = "USER")
//         @DisplayName("should return 400 when txref exceeds 20 characters")
//         void createBox_txrefTooLong_returns400() throws Exception {
//             mockMvc.perform(post("/api/boxes")
//                             .with(csrf())
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(Map.of(
//                                     "txref", "THIS-TXREF-IS-WAY-TOO-LONG-FOR-THE-SPEC",
//                                     "weightLimit", 300.000,
//                                     "batteryCapacity", 90
//                             ))))
//                     .andExpect(status().isBadRequest());
//         }

//         @Test
//         @WithMockUser(roles = "USER")
//         @DisplayName("should return 400 when battery capacity is out of range")
//         void createBox_batteryOutOfRange_returns400() throws Exception {
//             mockMvc.perform(post("/api/boxes")
//                             .with(csrf())
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(Map.of(
//                                     "txref", "BOX-NEW",
//                                     "weightLimit", 300.000,
//                                     "batteryCapacity", 150
//                             ))))
//                     .andExpect(status().isBadRequest());
//         }

//         @Test
//         @WithMockUser(roles = "USER")
//         @DisplayName("should return 409 when txref already exists")
//         void createBox_duplicateTxref_returns409() throws Exception {
//             when(boxService.createBox(any()))
//                     .thenThrow(new DuplicateResourceException("Box with txref 'BOX-001' already exists"));

//             mockMvc.perform(post("/api/boxes")
//                             .with(csrf())
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(Map.of(
//                                     "txref", "BOX-001",
//                                     "weightLimit", 300.000,
//                                     "batteryCapacity", 90
//                             ))))
//                     .andExpect(status().isConflict());
//         }

//         @Test
//         @DisplayName("should return 401 when request is unauthenticated")
//         void createBox_unauthenticated_returns401() throws Exception {
//             mockMvc.perform(post("/api/boxes")
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content("{}"))
//                     .andExpect(status().isUnauthorized());
//         }
//     }

//     // ─── POST /api/boxes/{txref}/load ──────────────────────────────────────────

//     @Nested
//     @DisplayName("POST /api/boxes/{txref}/load")
//     class LoadItemsTests {

//         private String validLoadRequest() throws Exception {
//             return objectMapper.writeValueAsString(Map.of(
//                     "items", List.of(Map.of(
//                             "name", "Parcel-X",
//                             "weight", 50.000,
//                             "code", "PARCEL_X_001"
//                     ))
//             ));
//         }

//         @Test
//         @WithMockUser(roles = "USER")
//         @DisplayName("should return 201 on successful load")
//         void loadItems_success_returns201() throws Exception {
//             LoadItemsResponse response = LoadItemsResponse.builder()
//                     .boxTxref("BOX-001")
//                     .boxState(BoxState.LOADED)
//                     .itemsLoaded(1)
//                     .idempotent(false)
//                     .build();

//             when(boxService.loadItems(eq("BOX-001"), any())).thenReturn(response);

//             mockMvc.perform(post("/api/boxes/BOX-001/load")
//                             .with(csrf())
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(validLoadRequest()))
//                     .andExpect(status().isCreated())
//                     .andExpect(jsonPath("$.data.boxState").value("LOADED"))
//                     .andExpect(jsonPath("$.data.idempotent").value(false));
//         }

//         @Test
//         @WithMockUser(roles = "USER")
//         @DisplayName("should return 200 on idempotent replay")
//         void loadItems_idempotentReplay_returns200() throws Exception {
//             LoadItemsResponse response = LoadItemsResponse.builder()
//                     .boxTxref("BOX-001")
//                     .boxState(BoxState.LOADED)
//                     .itemsLoaded(1)
//                     .idempotent(true)
//                     .build();

//             when(boxService.loadItems(eq("BOX-001"), any())).thenReturn(response);

//             mockMvc.perform(post("/api/boxes/BOX-001/load")
//                             .with(csrf())
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(validLoadRequest()))
//                     .andExpect(status().isOk())
//                     .andExpect(jsonPath("$.data.idempotent").value(true));
//         }

//         @Test
//         @WithMockUser(roles = "USER")
//         @DisplayName("should return 404 when box does not exist")
//         void loadItems_boxNotFound_returns404() throws Exception {
//             when(boxService.loadItems(eq("GHOST"), any()))
//                     .thenThrow(ResourceNotFoundException.box("GHOST"));

//             mockMvc.perform(post("/api/boxes/GHOST/load")
//                             .with(csrf())
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(validLoadRequest()))
//                     .andExpect(status().isNotFound());
//         }

//         @Test
//         @WithMockUser(roles = "USER")
//         @DisplayName("should return 422 when battery is too low")
//         void loadItems_lowBattery_returns422() throws Exception {
//             when(boxService.loadItems(eq("BOX-LOW-BAT"), any()))
//                     .thenThrow(BusinessRuleViolationException.lowBattery(20));

//             mockMvc.perform(post("/api/boxes/BOX-LOW-BAT/load")
//                             .with(csrf())
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(validLoadRequest()))
//                     .andExpect(status().isUnprocessableEntity());
//         }

//         @Test
//         @WithMockUser(roles = "USER")
//         @DisplayName("should return 400 when item code contains lowercase letters")
//         void loadItems_invalidItemCode_returns400() throws Exception {
//             mockMvc.perform(post("/api/boxes/BOX-001/load")
//                             .with(csrf())
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(Map.of(
//                                     "items", List.of(Map.of(
//                                             "name", "Parcel-X",
//                                             "weight", 50.000,
//                                             "code", "parcel_lowercase"   // ← invalid: lowercase
//                                     ))
//                             ))))
//                     .andExpect(status().isBadRequest());
//         }

//         @Test
//         @WithMockUser(roles = "USER")
//         @DisplayName("should return 400 when item name contains invalid characters")
//         void loadItems_invalidItemName_returns400() throws Exception {
//             mockMvc.perform(post("/api/boxes/BOX-001/load")
//                             .with(csrf())
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(Map.of(
//                                     "items", List.of(Map.of(
//                                             "name", "Parcel@#$",          // ← invalid chars
//                                             "weight", 50.000,
//                                             "code", "PARCEL_001"
//                                     ))
//                             ))))
//                     .andExpect(status().isBadRequest());
//         }

//         @Test
//         @WithMockUser(roles = "USER")
//         @DisplayName("should return 400 when item weight is zero")
//         void loadItems_zeroWeight_returns400() throws Exception {
//             mockMvc.perform(post("/api/boxes/BOX-001/load")
//                             .with(csrf())
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(Map.of(
//                                     "items", List.of(Map.of(
//                                             "name", "Parcel-X",
//                                             "weight", 0.000,             // ← invalid
//                                             "code", "PARCEL_001"
//                                     ))
//                             ))))
//                     .andExpect(status().isBadRequest());
//         }
//     }

//     // ─── GET /api/boxes/{txref}/items ──────────────────────────────────────────

//     @Nested
//     @DisplayName("GET /api/boxes/{txref}/items")
//     class GetLoadedItemsTests {

//         @Test
//         @WithMockUser(roles = "USER")
//         @DisplayName("should return 200 with item list")
//         void getLoadedItems_validBox_returns200() throws Exception {
//             ItemResponse item = new ItemResponse();
//             item.setCode("PARCEL_A_001");
//             item.setName("Parcel-A");

//             when(boxService.getLoadedItems("BOX-LOADED")).thenReturn(List.of(item));

//             mockMvc.perform(get("/api/boxes/BOX-LOADED/items"))
//                     .andExpect(status().isOk())
//                     .andExpect(jsonPath("$.data[0].code").value("PARCEL_A_001"));
//         }

//         @Test
//         @WithMockUser(roles = "USER")
//         @DisplayName("should return 200 with empty list when box has no items")
//         void getLoadedItems_noItems_returnsEmptyList() throws Exception {
//             when(boxService.getLoadedItems("BOX-001")).thenReturn(List.of());

//             mockMvc.perform(get("/api/boxes/BOX-001/items"))
//                     .andExpect(status().isOk())
//                     .andExpect(jsonPath("$.data").isEmpty());
//         }

//         @Test
//         @WithMockUser(roles = "USER")
//         @DisplayName("should return 404 when box does not exist")
//         void getLoadedItems_boxNotFound_returns404() throws Exception {
//             when(boxService.getLoadedItems("GHOST"))
//                     .thenThrow(ResourceNotFoundException.box("GHOST"));

//             mockMvc.perform(get("/api/boxes/GHOST/items"))
//                     .andExpect(status().isNotFound());
//         }

//         @Test
//         @DisplayName("should return 401 when unauthenticated")
//         void getLoadedItems_unauthenticated_returns401() throws Exception {
//             mockMvc.perform(get("/api/boxes/BOX-001/items"))
//                     .andExpect(status().isUnauthorized());
//         }
//     }

//     // ─── GET /api/boxes/available ──────────────────────────────────────────────

//     @Nested
//     @DisplayName("GET /api/boxes/available")
//     class GetAvailableBoxesTests {

//         @Test
//         @WithMockUser(roles = "USER")
//         @DisplayName("should return 200 with available boxes")
//         void getAvailableBoxes_returnsBoxes() throws Exception {
//             BoxResponse box = new BoxResponse();
//             box.setTxref("BOX-001");
//             box.setState(BoxState.IDLE);

//             when(boxService.getAvailableBoxes()).thenReturn(List.of(box));

//             mockMvc.perform(get("/api/boxes/available"))
//                     .andExpect(status().isOk())
//                     .andExpect(jsonPath("$.data[0].txref").value("BOX-001"));
//         }

//         @Test
//         @WithMockUser(roles = "USER")
//         @DisplayName("should return 200 with empty list when no boxes available")
//         void getAvailableBoxes_noBoxes_returnsEmpty() throws Exception {
//             when(boxService.getAvailableBoxes()).thenReturn(List.of());

//             mockMvc.perform(get("/api/boxes/available"))
//                     .andExpect(status().isOk())
//                     .andExpect(jsonPath("$.data").isEmpty());
//         }

//         @Test
//         @DisplayName("should return 401 when unauthenticated")
//         void getAvailableBoxes_unauthenticated_returns401() throws Exception {
//             mockMvc.perform(get("/api/boxes/available"))
//                     .andExpect(status().isUnauthorized());
//         }
//     }

//     // ─── GET /api/boxes/{txref}/battery ───────────────────────────────────────

//     @Nested
//     @DisplayName("GET /api/boxes/{txref}/battery")
//     class GetBatteryTests {

//         @Test
//         @WithMockUser(roles = "USER")
//         @DisplayName("should return 200 with battery level")
//         void getBattery_validBox_returns200() throws Exception {
//             BatteryResponse battery = new BatteryResponse();
//             battery.setTxref("BOX-001");
//             battery.setBatteryCapacity(95);

//             when(boxService.getBatteryLevel("BOX-001")).thenReturn(battery);

//             mockMvc.perform(get("/api/boxes/BOX-001/battery"))
//                     .andExpect(status().isOk())
//                     .andExpect(jsonPath("$.data.batteryCapacity").value(95));
//         }

//         @Test
//         @WithMockUser(roles = "USER")
//         @DisplayName("should return 404 when box does not exist")
//         void getBattery_boxNotFound_returns404() throws Exception {
//             when(boxService.getBatteryLevel("GHOST"))
//                     .thenThrow(ResourceNotFoundException.box("GHOST"));

//             mockMvc.perform(get("/api/boxes/GHOST/battery"))
//                     .andExpect(status().isNotFound());
//         }

//         @Test
//         @DisplayName("should return 401 when unauthenticated")
//         void getBattery_unauthenticated_returns401() throws Exception {
//             mockMvc.perform(get("/api/boxes/BOX-001/battery"))
//                     .andExpect(status().isUnauthorized());
//         }
//     }
// }
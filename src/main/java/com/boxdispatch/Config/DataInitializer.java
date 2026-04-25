package com.boxdispatch.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.boxdispatch.Enums.BoxState;
import com.boxdispatch.Models.Box;
import com.boxdispatch.Models.Item;
import com.boxdispatch.Repositories.BoxRepository;

import java.math.BigDecimal;

/**
 * Seeds initial data if the database is empty (safe to run repeatedly).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final BoxRepository boxRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (boxRepository.count() > 0) {
            log.info("Database already seeded — skipping data initialization.");
            return;
        }

        log.info("Seeding initial data...");

        boxRepository.save(Box.builder()
                .txref("BOX-001")
                .weightLimit(new BigDecimal("500.000"))
                .batteryCapacity(95)
                .state(BoxState.IDLE)
                .build());

        boxRepository.save(Box.builder()
                .txref("BOX-002")
                .weightLimit(new BigDecimal("300.000"))
                .batteryCapacity(80)
                .state(BoxState.IDLE)
                .build());

        boxRepository.save(Box.builder()
                .txref("BOX-LOW-BAT")
                .weightLimit(new BigDecimal("500.000"))
                .batteryCapacity(20)
                .state(BoxState.IDLE)
                .build());

        boxRepository.save(Box.builder()
                .txref("BOX-DELIVERING")
                .weightLimit(new BigDecimal("500.000"))
                .batteryCapacity(60)
                .state(BoxState.DELIVERING)
                .build());

        // A pre-loaded box with items
        Box loaded = Box.builder()
                .txref("BOX-LOADED")
                .weightLimit(new BigDecimal("200.000"))
                .batteryCapacity(70)
                .state(BoxState.LOADED)
                .build();

        Item item1 = Item.builder()
                .name("Parcel-A")
                .weight(new BigDecimal("80.000"))
                .code("PARCEL_A_001")
                .box(loaded)
                .build();

        Item item2 = Item.builder()
                .name("Parcel-B")
                .weight(new BigDecimal("50.000"))
                .code("PARCEL_B_002")
                .box(loaded)
                .build();

        loaded.getItems().add(item1);
        loaded.getItems().add(item2);
        boxRepository.save(loaded);

        log.info("Seed complete: {} boxes created.", boxRepository.count());
    }
}

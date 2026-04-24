package com.boxdispatch.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.boxdispatch.Enums.BoxState;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
 
@Entity
@Table(
    name = "boxes",
    indexes = {
        @Index(name = "idx_box_txref", columnList = "txref", unique = true),
        @Index(name = "idx_box_state", columnList = "state"),
        @Index(name = "idx_box_battery", columnList = "battery_capacity")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Box {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(name = "txref", nullable = false, unique = true, length = 20)
    private String txref;
 
    @Column(name = "weight_limit", nullable = false, precision = 8, scale = 3)
    private BigDecimal weightLimit;
 
    @Column(name = "battery_capacity", nullable = false)
    private Integer batteryCapacity;
 
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    @Builder.Default
    private BoxState state = BoxState.IDLE;
 
    @OneToMany(mappedBy = "box", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Item> items = new ArrayList<>();
 
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
 
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
 
    @Version
    @Column(name = "version")
    private Long version;
 
    /**
     * Calculates total weight of currently loaded items.
     */
    public BigDecimal getTotalItemWeight() {
        return items.stream()
                .map(Item::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
 
    /**
     * Calculates remaining capacity.
     */
    public BigDecimal getRemainingCapacity() {
        return weightLimit.subtract(getTotalItemWeight());
    }
}

package com.boxdispatch.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
 
import java.math.BigDecimal;
import java.time.Instant;
 
@Entity
@Table(
    name = "items",
    indexes = {
        @Index(name = "idx_item_code", columnList = "code", unique = true),
        @Index(name = "idx_item_box_id", columnList = "box_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(name = "name", nullable = false, length = 100)
    private String name;
 
    @Column(name = "weight", nullable = false, precision = 8, scale = 3)
    private BigDecimal weight;
 
    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_id", nullable = false)
    private Box box;
 
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
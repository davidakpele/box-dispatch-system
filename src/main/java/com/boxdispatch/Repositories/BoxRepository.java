package com.boxdispatch.Repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.boxdispatch.Enums.BoxState;
import com.boxdispatch.Models.Box;

import java.util.List;
import java.util.Optional;
 
@Repository
public interface BoxRepository extends JpaRepository<Box, Long> {
 
    Optional<Box> findByTxref(String txref);
 
    boolean existsByTxref(String txref);
 
    /**
     * Returns boxes that are eligible for loading:
     * battery >= 25% AND state is IDLE or LOADING.
     */
    @Query("""
        SELECT b FROM Box b
        WHERE b.batteryCapacity >= :minBattery
        AND b.state IN :loadableStates
        ORDER BY b.batteryCapacity DESC
    """)
    List<Box> findAvailableForLoading(
        @Param("minBattery") int minBattery,
        @Param("loadableStates") List<BoxState> loadableStates
    );
 
    @Query("SELECT b FROM Box b LEFT JOIN FETCH b.items WHERE b.txref = :txref")
    Optional<Box> findByTxrefWithItems(@Param("txref") String txref);
}
 
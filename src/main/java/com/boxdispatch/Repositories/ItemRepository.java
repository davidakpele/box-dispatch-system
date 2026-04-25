package com.boxdispatch.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.boxdispatch.Models.Item;
import java.util.List;
import java.util.Set;
 
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
 
    List<Item> findByBoxTxref(String boxTxref);
 
    boolean existsByCode(String code);
 
    /**
     * Check which of the provided codes already exist in the database.
     */
    @Query("SELECT i.code FROM Item i WHERE i.code IN :codes")
    Set<String> findExistingCodes(@Param("codes") Set<String> codes);
}
 
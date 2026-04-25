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

    @Query("SELECT i FROM Item i WHERE i.box.txref = :boxTxref")
    List<Item> findByBoxTxref(@Param("boxTxref") String boxTxref);

    @Query("SELECT COUNT(i) > 0 FROM Item i WHERE i.code = :code")
    boolean existsByCode(@Param("code") String code);

    @Query("SELECT i.code FROM Item i WHERE i.code IN :codes")
    Set<String> findExistingCodes(@Param("codes") Set<String> codes);
}
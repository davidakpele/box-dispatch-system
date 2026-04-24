package com.boxdispatch.Repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.boxdispatch.Models.Users;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {

    @Query("SELECT u FROM Users u WHERE u.username = :username")
    Optional<Users> findByUsername(@Param("username") String username);

    Optional<Users> findByEmail(String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM Users u WHERE u.id IN :ids")
    void deleteUserByIds(@Param("ids") List<Long> ids);



}

package com.assetmgmt.repository;

import com.assetmgmt.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    List<User> findByEnabled(boolean enabled);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<User> search(@org.springframework.data.repository.query.Param("q") String q);
}

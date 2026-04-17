package com.smartpos.backend.users;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            SELECT u FROM UserEntity u
            WHERE (LOWER(u.name) LIKE CONCAT('%', :query, '%')
                    OR LOWER(u.email) LIKE CONCAT('%', :query, '%'))
            """)
    Page<UserEntity> search(@Param("query") String query, Pageable pageable);
}

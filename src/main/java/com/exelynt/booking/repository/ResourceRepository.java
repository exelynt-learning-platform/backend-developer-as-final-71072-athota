package com.exelynt.booking.repository;

import com.exelynt.booking.entity.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    Page<Resource> findByAvailableTrue(Pageable pageable);

    @Query("SELECT r FROM Resource r WHERE " +
           "(:type IS NULL OR LOWER(r.type) = LOWER(:type)) AND " +
           "(:available IS NULL OR r.available = :available)")
    Page<Resource> searchResources(@Param("type") String type, 
                                   @Param("available") Boolean available, 
                                   Pageable pageable);
}

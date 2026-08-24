package com.example.travel.domain.region.repository;

import com.example.travel.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {
    @Query("""
            select r
            from Region r
            where r.id = :id
              and r.active = true
            """)
    Optional<Region> findActiveById(@Param("id") Long id);

    @Query(value = """
            select *
            from regions
            where name = :name
              and is_active = true
            order by id asc
            limit 1
            """, nativeQuery = true)
    Optional<Region> findActiveByName(@Param("name") String name);
}

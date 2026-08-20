package com.example.travel.domain.travel.repository;

import com.example.travel.domain.travel.entity.TravelGuideRouteSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TravelGuideRouteSegmentRepository extends JpaRepository<TravelGuideRouteSegment, Long> {
    List<TravelGuideRouteSegment> findByGuideIdOrderByDayNumberAscSegmentOrderAsc(Long guideId);
}

package com.example.travel.domain.travel.repository;

import com.example.travel.domain.travel.entity.TravelGuideItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TravelGuideItemRepository extends JpaRepository<TravelGuideItem, Long> {
    List<TravelGuideItem> findByGuideIdOrderByDayNumberAscItemOrderAsc(Long guideId);
}

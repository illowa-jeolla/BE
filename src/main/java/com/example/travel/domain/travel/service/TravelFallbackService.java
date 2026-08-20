package com.example.travel.domain.travel.service;

import com.example.travel.domain.travel.ai.dto.AiTravelGuideResult;
import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.entity.TravelRecommendationRequest;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class TravelFallbackService {
    public AiTravelGuideResult create(TravelRecommendationRequest request,
                                      List<TravelCandidateItem> candidates) {
        int tripDays = (int) ChronoUnit.DAYS.between(
                request.getStartsOn(), request.getEndsOn()) + 1;
        Integer[] dailyPlaceCounts = request.getDailyPlaceCounts();
        List<AiTravelGuideResult.Day> days = new ArrayList<>();
        int candidateIndex = 0;
        for (int dayNumber = 1; dayNumber <= tripDays; dayNumber++) {
            List<AiTravelGuideResult.Item> items = new ArrayList<>();
            LocalTime time = LocalTime.of(10, 0);
            int placesForDay = dailyPlaceCounts[dayNumber - 1];
            for (int order = 1;
                 order <= placesForDay && candidateIndex < candidates.size();
                 order++, candidateIndex++) {
                TravelCandidateItem candidate = candidates.get(candidateIndex);
                items.add(new AiTravelGuideResult.Item(candidate.contentId(), order,
                        time.toString(), 90,
                        "숙소와의 거리와 선택 조건을 기준으로 추천한 장소입니다."));
                time = time.plusHours(2);
            }
            days.add(new AiTravelGuideResult.Day(dayNumber,
                    dayNumber + "일차 추천 코스", List.copyOf(items)));
        }
        return new AiTravelGuideResult(
                request.getRegionName() + ", 이렇게 둘러보세요",
                "숙소 주변 관광지를 거리와 선택 조건에 따라 구성한 기본 추천 코스입니다.",
                List.copyOf(days),
                "장소별 운영 여부를 확인하고 이동시간에 여유를 두고 출발해 주세요.");
    }
}

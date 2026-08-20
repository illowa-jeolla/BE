package com.example.travel.domain.travel.ai.dto;

import java.util.List;

public record AiTravelGuideResult(
        String title,
        String summary,
        List<Day> days,
        String travelTip
) {
    public record Day(int dayNumber, String title, List<Item> items) {
    }

    public record Item(
            String contentId,
            int order,
            String recommendedTime,
            int stayMinutes,
            String reason
    ) {
    }
}

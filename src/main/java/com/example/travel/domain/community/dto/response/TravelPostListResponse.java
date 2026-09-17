package com.example.travel.domain.community.dto.response;

import java.util.List;

public record TravelPostListResponse(List<TravelPostListItem> content, int page, int size,
                                     long totalElements, boolean hasNext) {
}

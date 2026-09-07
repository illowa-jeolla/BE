package com.example.travel.domain.job.dto;

import java.util.List;

public record JobFavoriteListResponse(List<JobFavoriteItem> content, int page, int size,
                                      long totalElements, boolean hasNext) {}

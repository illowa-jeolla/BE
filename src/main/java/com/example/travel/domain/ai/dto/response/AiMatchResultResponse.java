package com.example.travel.domain.ai.dto.response;

import com.example.travel.domain.ai.enums.AiRequestStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AiMatchResultResponse(
        UUID requestId,
        AiRequestStatus status,
        List<Result> results
) {
    public record Result(int rank, Region region, Scores scores, String summary,
                         SectionStatus regionStatus, SectionStatus jobStatus,
                         SectionStatus tourismStatus, List<Job> jobs, List<Place> places) {}
    public record SectionStatus(SectionState status, String message) {}
    public enum SectionState { SUCCESS, REPLACED, FAILED }
    public record Region(Long regionId, String name) {}
    public record Scores(int overall, int region, int job, int tourism, int housing, int community) {}
    public record Job(String source, String externalId, String title, Region region, String companyName,
                      String address, LocalDate deadline, String sourceUrl,
                      int matchScore, String reason) {}
    public record Place(String contentId, String name, String category, String address,
                        String imageUrl, int matchScore, String reason) {}

    public static AiMatchResultResponse processing(UUID requestId, AiRequestStatus status) {
        return new AiMatchResultResponse(requestId, status, List.of());
    }
}

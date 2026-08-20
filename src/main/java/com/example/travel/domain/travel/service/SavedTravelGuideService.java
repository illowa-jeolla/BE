package com.example.travel.domain.travel.service;

import com.example.travel.domain.travel.dto.response.SavedTravelGuideResponse;
import com.example.travel.domain.travel.dto.response.DeletedSavedTravelGuideResponse;
import com.example.travel.domain.travel.dto.response.TravelGuideSaveResponse;
import com.example.travel.domain.travel.entity.SavedTravelGuide;
import com.example.travel.domain.travel.entity.TravelGuide;
import com.example.travel.domain.travel.entity.id.SavedTravelGuideId;
import com.example.travel.domain.travel.exception.TravelRecommendationErrorCode;
import com.example.travel.domain.travel.exception.TravelRecommendationException;
import com.example.travel.domain.travel.repository.SavedTravelGuideRepository;
import com.example.travel.domain.travel.repository.TravelGuideRepository;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SavedTravelGuideService {
    private final SavedTravelGuideRepository savedGuideRepository;
    private final TravelGuideRepository guideRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final TravelGuideDraftCacheService draftCacheService;
    private final TravelRecommendationPersistenceService persistenceService;

    public SavedTravelGuideService(SavedTravelGuideRepository savedGuideRepository,
                                   TravelGuideRepository guideRepository,
                                   UserRepository userRepository,
                                   Clock clock,
                                   TravelGuideDraftCacheService draftCacheService,
                                   TravelRecommendationPersistenceService persistenceService) {
        this.savedGuideRepository = savedGuideRepository;
        this.guideRepository = guideRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.draftCacheService = draftCacheService;
        this.persistenceService = persistenceService;
    }

    @Transactional
    public TravelGuideSaveResponse restore(Long userId, Long guideId) {
        SavedTravelGuideId id = new SavedTravelGuideId(userId, guideId);
        ownedGuide(userId, guideId);
        SavedTravelGuide saved = savedGuideRepository.findById(id)
                .orElseThrow(() -> new TravelRecommendationException(
                        TravelRecommendationErrorCode.SAVED_GUIDE_NOT_FOUND));
        if (saved.getDeletedAt() == null) {
            throw new TravelRecommendationException(
                    TravelRecommendationErrorCode.GUIDE_ALREADY_SAVED);
        }
        saved.restore();
        return new TravelGuideSaveResponse(guideId, true);
    }

    @Transactional
    public TravelGuideSaveResponse saveDraft(Long userId, Long draftId) {
        if (guideRepository.findBySourceRequestIdAndUserId(draftId, userId).isPresent()) {
            throw new TravelRecommendationException(
                    TravelRecommendationErrorCode.GUIDE_ALREADY_SAVED);
        }
        var draft = draftCacheService.find(draftId);
        if (!draft.userId().equals(userId)) {
            throw new TravelRecommendationException(
                    TravelRecommendationErrorCode.DRAFT_NOT_FOUND);
        }
        Long guideId = persistenceService.saveGuide(draft.request(), draft.result(),
                draft.candidates(), draft.routes(), draft.generatedByAi());
        TravelGuide guide = guideRepository.findByIdAndUserId(guideId, userId)
                .orElseThrow(() -> new TravelRecommendationException(
                        TravelRecommendationErrorCode.GUIDE_NOT_FOUND));
        saveRelation(userId, guide);
        draftCacheService.delete(draftId);
        return new TravelGuideSaveResponse(guideId, true);
    }

    @Transactional
    public TravelGuideSaveResponse cancel(Long userId, Long guideId) {
        SavedTravelGuideId id = new SavedTravelGuideId(userId, guideId);
        ownedGuide(userId, guideId);
        SavedTravelGuide saved = savedGuideRepository.findById(id)
                .orElseThrow(() -> new TravelRecommendationException(
                        TravelRecommendationErrorCode.SAVED_GUIDE_NOT_FOUND));
        if (saved.getDeletedAt() != null) {
            throw new TravelRecommendationException(
                    TravelRecommendationErrorCode.GUIDE_ALREADY_DELETED);
        }
        saved.delete(OffsetDateTime.now(clock));
        return new TravelGuideSaveResponse(guideId, false);
    }

    public List<SavedTravelGuideResponse> findAll(Long userId) {
        return savedGuideRepository.findByUserIdAndDeletedAtIsNullOrderBySavedAtDesc(userId).stream()
                .map(saved -> {
                    TravelGuide guide = saved.getGuide();
                    return new SavedTravelGuideResponse(guide.getId(), guide.getTitle(),
                            guide.getSummary(), guide.getCondition().getRegionName(),
                            guide.getCondition().getStartsOn(), guide.getCondition().getEndsOn(),
                            guide.isGeneratedByAi(), saved.getSavedAt());
                })
                .toList();
    }

    public List<DeletedSavedTravelGuideResponse> findDeleted(Long userId) {
        return savedGuideRepository
                .findByUserIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(userId).stream()
                .map(saved -> {
                    TravelGuide guide = saved.getGuide();
                    return new DeletedSavedTravelGuideResponse(guide.getId(), guide.getTitle(),
                            guide.getSummary(), guide.getCondition().getRegionName(),
                            guide.getCondition().getStartsOn(), guide.getCondition().getEndsOn(),
                            guide.isGeneratedByAi(), saved.getDeletedAt());
                })
                .toList();
    }

    private TravelGuide ownedGuide(Long userId, Long guideId) {
        return guideRepository.findByIdAndUserId(guideId, userId)
                .orElseThrow(() -> new TravelRecommendationException(
                        TravelRecommendationErrorCode.GUIDE_NOT_FOUND));
    }

    private void saveRelation(Long userId, TravelGuide guide) {
        SavedTravelGuideId id = new SavedTravelGuideId(userId, guide.getId());
        var existing = savedGuideRepository.findById(id);
        if (existing.isPresent()) {
            throw new TravelRecommendationException(
                    TravelRecommendationErrorCode.GUIDE_ALREADY_SAVED);
        }
        User user = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new TravelRecommendationException(
                        TravelRecommendationErrorCode.USER_NOT_FOUND));
        savedGuideRepository.save(SavedTravelGuide.create(user, guide));
    }
}

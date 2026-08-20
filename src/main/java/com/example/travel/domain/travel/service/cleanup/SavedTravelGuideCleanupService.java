package com.example.travel.domain.travel.service.cleanup;

import com.example.travel.domain.travel.repository.SavedTravelGuideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavedTravelGuideCleanupService {
    private final SavedTravelGuideRepository savedGuideRepository;

    public SavedTravelGuideCleanupService(SavedTravelGuideRepository savedGuideRepository) {
        this.savedGuideRepository = savedGuideRepository;
    }

    @Transactional
    public int deleteSoftDeleted() {
        return savedGuideRepository.deleteAllSoftDeleted();
    }
}

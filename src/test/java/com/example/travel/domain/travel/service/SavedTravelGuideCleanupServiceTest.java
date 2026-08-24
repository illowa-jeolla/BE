package com.example.travel.domain.travel.service;

import com.example.travel.domain.travel.repository.SavedTravelGuideRepository;
import com.example.travel.domain.travel.service.cleanup.SavedTravelGuideCleanupService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SavedTravelGuideCleanupServiceTest {
    @Test
    void physicallyDeletesOnlySoftDeletedSavedGuides() {
        SavedTravelGuideRepository repository = mock(SavedTravelGuideRepository.class);
        SavedTravelGuideCleanupService service = new SavedTravelGuideCleanupService(repository);
        when(repository.deleteAllSoftDeleted()).thenReturn(3);

        int deleted = service.deleteSoftDeleted();

        assertThat(deleted).isEqualTo(3);
        verify(repository).deleteAllSoftDeleted();
    }
}

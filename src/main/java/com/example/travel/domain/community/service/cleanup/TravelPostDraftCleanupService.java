package com.example.travel.domain.community.service.cleanup;

import com.example.travel.domain.community.enums.TravelPostStatus;
import com.example.travel.domain.community.repository.TravelPostImageRepository;
import com.example.travel.domain.community.repository.TravelPostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TravelPostDraftCleanupService {
    private final TravelPostRepository postRepository;
    private final TravelPostImageRepository imageRepository;
    private final S3ObjectDeletionQueue deletionQueue;

    public TravelPostDraftCleanupService(TravelPostRepository postRepository,
                                         TravelPostImageRepository imageRepository,
                                         S3ObjectDeletionQueue deletionQueue) {
        this.postRepository = postRepository;
        this.imageRepository = imageRepository;
        this.deletionQueue = deletionQueue;
    }

    @Transactional
    public int deleteExpired(OffsetDateTime cutoff) {
        var drafts = postRepository.findAllByStatusAndUpdatedAtBefore(
                TravelPostStatus.DRAFT, cutoff);
        List<String> keys = new ArrayList<>();
        drafts.forEach(draft -> {
            keys.addAll(imageRepository.findAllByPostIdOrderByDisplayOrderAsc(draft.getId())
                    .stream().map(image -> image.getObjectKey()).toList());
            imageRepository.deleteAllByPostId(draft.getId());
        });
        postRepository.deleteAll(drafts);
        keys.forEach(deletionQueue::schedule);
        postRepository.flush();
        return drafts.size();
    }
}

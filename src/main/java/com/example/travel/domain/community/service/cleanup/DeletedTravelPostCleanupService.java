package com.example.travel.domain.community.service.cleanup;

import com.example.travel.domain.community.enums.TravelPostStatus;
import com.example.travel.domain.community.repository.TravelPostCommentRepository;
import com.example.travel.domain.community.repository.TravelPostImageRepository;
import com.example.travel.domain.community.repository.TravelPostLikeRepository;
import com.example.travel.domain.community.repository.TravelPostRepository;
import com.example.travel.domain.community.repository.TravelPostTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeletedTravelPostCleanupService {
    private final TravelPostRepository postRepository;
    private final TravelPostImageRepository imageRepository;
    private final TravelPostCommentRepository commentRepository;
    private final TravelPostLikeRepository likeRepository;
    private final TravelPostTagRepository tagRepository;
    private final S3ObjectDeletionQueue deletionQueue;

    public DeletedTravelPostCleanupService(TravelPostRepository postRepository,
                                           TravelPostImageRepository imageRepository,
                                           TravelPostCommentRepository commentRepository,
                                           TravelPostLikeRepository likeRepository,
                                           TravelPostTagRepository tagRepository,
                                           S3ObjectDeletionQueue deletionQueue) {
        this.postRepository = postRepository;
        this.imageRepository = imageRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.tagRepository = tagRepository;
        this.deletionQueue = deletionQueue;
    }

    @Transactional
    public int deleteExpired(OffsetDateTime cutoff) {
        var posts = postRepository.findAllByStatusAndUpdatedAtBefore(
                TravelPostStatus.DELETED, cutoff);
        List<String> objectKeys = new ArrayList<>();

        posts.forEach(post -> {
            Long postId = post.getId();
            objectKeys.addAll(imageRepository.findAllByPostIdOrderByDisplayOrderAsc(postId)
                    .stream().map(image -> image.getObjectKey()).toList());
            commentRepository.deleteAllByPostId(postId);
            likeRepository.deleteAllByPostId(postId);
            tagRepository.deleteAllByPostId(postId);
            imageRepository.deleteAllByPostId(postId);
        });

        postRepository.deleteAll(posts);
        objectKeys.forEach(deletionQueue::schedule);
        postRepository.flush();
        return posts.size();
    }
}

package com.example.travel.domain.community.dto.response;

import com.example.travel.domain.community.entity.TravelPost;

import java.time.OffsetDateTime;
import java.util.List;

public record TravelPostDetailResponse(
        Long postId, String title, String concept, String content,
        Long regionId, String regionName,
        Long authorId, String authorNickname, String authorAvatarUrl,
        OffsetDateTime createdAt, OffsetDateTime updatedAt, int viewCount,
        long commentCount, boolean editable, boolean deletable,
        List<PostImageItem> images
) {
    public static TravelPostDetailResponse from(TravelPost post, Long viewerId,
                                                long commentCount, List<PostImageItem> images) {
        boolean owner = post.getAuthor().getId().equals(viewerId);
        return new TravelPostDetailResponse(post.getId(), post.getTitle(), post.getConcept(),
                post.getContent(), post.getRegion().getId(), post.getRegion().getName(),
                post.getAuthor().getId(), post.getAuthor().getNickname(),
                post.getAuthor().getAvatarUrl(), post.getCreatedAt(), post.getUpdatedAt(),
                post.getViewCount(), commentCount, owner, owner, images);
    }

    public record PostImageItem(Long imageId, String imageUrl, String altText,
                                short displayOrder) {
    }
}

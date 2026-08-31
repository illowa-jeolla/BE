package com.example.travel.domain.community.dto.response;

import com.example.travel.domain.community.entity.TravelPost;

import java.time.OffsetDateTime;

public record TravelPostListItem(
        Long postId, String title, String contentPreview,
        Long regionId, String regionName, Long authorId, String authorNickname,
        OffsetDateTime createdAt, int viewCount, long commentCount,
        String thumbnailUrl
) {
    public static TravelPostListItem from(TravelPost post, long commentCount, String thumbnailUrl) {
        String content = post.getContent();
        String preview = content == null ? null
                : content.substring(0, Math.min(content.length(), 150));
        return new TravelPostListItem(post.getId(), post.getTitle(), preview,
                post.getRegion().getId(), post.getRegion().getName(),
                post.getAuthor().getId(), post.getAuthor().getNickname(), post.getCreatedAt(),
                post.getViewCount(), commentCount, thumbnailUrl);
    }
}

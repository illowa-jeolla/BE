package com.example.travel.domain.community.dto.response;

import com.example.travel.domain.community.entity.TravelPostComment;
import com.example.travel.domain.community.enums.CommentStatus;

import java.time.OffsetDateTime;

public record TravelPostCommentResponse(
        Long commentId, Long authorId, String authorNickname,
        String content, boolean secret, boolean contentVisible,
        boolean editable, boolean deletable,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static TravelPostCommentResponse from(TravelPostComment comment, Long viewerId) {
        boolean owner = comment.getAuthor().getId().equals(viewerId);
        boolean postOwner = comment.getPost().getAuthor().getId().equals(viewerId);
        boolean secret = comment.getStatus() == CommentStatus.HIDDEN;
        boolean visible = !secret || owner || postOwner;
        return new TravelPostCommentResponse(comment.getId(), comment.getAuthor().getId(),
                comment.getAuthor().getNickname(), visible ? comment.getContent() : null,
                secret, visible, owner, owner, comment.getCreatedAt(), comment.getUpdatedAt());
    }
}

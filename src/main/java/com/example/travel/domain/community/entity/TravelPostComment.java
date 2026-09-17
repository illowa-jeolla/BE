package com.example.travel.domain.community.entity;

import com.example.travel.domain.community.enums.CommentStatus;
import com.example.travel.domain.user.entity.User;
import com.example.travel.global.persistence.UpdatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "travel_post_comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelPostComment extends UpdatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private TravelPost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private TravelPostComment parentComment;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommentStatus status = CommentStatus.VISIBLE;

    private TravelPostComment(TravelPost post, User author, String content, boolean secret) {
        this.post = post;
        this.author = author;
        this.content = content;
        this.status = secret ? CommentStatus.HIDDEN : CommentStatus.VISIBLE;
    }

    public static TravelPostComment create(TravelPost post, User author,
                                           String content, boolean secret) {
        return new TravelPostComment(post, author, content, secret);
    }

    public void update(String content, boolean secret) {
        if (status == CommentStatus.DELETED) {
            throw new IllegalStateException("Deleted comments cannot be updated.");
        }
        this.content = content;
        this.status = secret ? CommentStatus.HIDDEN : CommentStatus.VISIBLE;
        touchUpdatedAt();
    }

    public void softDelete() {
        status = CommentStatus.DELETED;
        touchUpdatedAt();
    }
}

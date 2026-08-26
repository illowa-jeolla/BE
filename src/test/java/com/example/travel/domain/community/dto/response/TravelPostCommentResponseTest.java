package com.example.travel.domain.community.dto.response;

import com.example.travel.domain.community.entity.TravelPost;
import com.example.travel.domain.community.entity.TravelPostComment;
import com.example.travel.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TravelPostCommentResponseTest {
    @Test
    void hidesSecretContentFromUnrelatedViewer() {
        User postAuthor = mock(User.class);
        User commentAuthor = mock(User.class);
        TravelPost post = mock(TravelPost.class);
        when(postAuthor.getId()).thenReturn(1L);
        when(commentAuthor.getId()).thenReturn(2L);
        when(commentAuthor.getNickname()).thenReturn("댓글작성자");
        when(post.getAuthor()).thenReturn(postAuthor);
        TravelPostComment comment = TravelPostComment.create(
                post, commentAuthor, "비밀 내용", true);

        var hidden = TravelPostCommentResponse.from(comment, 3L);
        var visibleToAuthor = TravelPostCommentResponse.from(comment, 2L);
        var visibleToPostAuthor = TravelPostCommentResponse.from(comment, 1L);

        assertThat(hidden.secret()).isTrue();
        assertThat(hidden.contentVisible()).isFalse();
        assertThat(hidden.content()).isNull();
        assertThat(visibleToAuthor.content()).isEqualTo("비밀 내용");
        assertThat(visibleToPostAuthor.content()).isEqualTo("비밀 내용");
    }
}

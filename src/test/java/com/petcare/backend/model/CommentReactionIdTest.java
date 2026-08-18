package com.petcare.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommentReactionIdTest {
    @Test
    void constructorsAccessorsEqualityAndHashCode_HandleEqualDifferentAndNullValues() {
        CommentReactionId empty = new CommentReactionId();
        empty.setCommentId(null); empty.setUserId(null);
        CommentReactionId same = new CommentReactionId(1L, 2L);
        CommentReactionId equal = new CommentReactionId(1L, 2L);
        CommentReactionId differentComment = new CommentReactionId(3L, 2L);
        CommentReactionId differentUser = new CommentReactionId(1L, 3L);

        assertThat(empty.getCommentId()).isNull(); assertThat(empty.getUserId()).isNull();
        assertThat(same).isEqualTo(same).isEqualTo(equal).hasSameHashCodeAs(equal);
        assertThat(same).isNotEqualTo(differentComment).isNotEqualTo(differentUser).isNotEqualTo(null).isNotEqualTo("id");
    }
}

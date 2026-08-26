package com.petcare.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PostReactionIdTest {
    @Test
    void constructorsAccessorsEqualityAndHashCode_HandleEqualDifferentAndNullValues() {
        PostReactionId empty = new PostReactionId();
        empty.setPostId(null); empty.setUserId(null);
        PostReactionId same = new PostReactionId(1L, 2L);
        PostReactionId equal = new PostReactionId(1L, 2L);
        PostReactionId differentPost = new PostReactionId(3L, 2L);
        PostReactionId differentUser = new PostReactionId(1L, 3L);

        assertThat(empty.getPostId()).isNull(); assertThat(empty.getUserId()).isNull();
        assertThat(same).isEqualTo(same).isEqualTo(equal).hasSameHashCodeAs(equal);
        assertThat(same).isNotEqualTo(differentPost).isNotEqualTo(differentUser).isNotEqualTo(null).isNotEqualTo("id");
    }
}

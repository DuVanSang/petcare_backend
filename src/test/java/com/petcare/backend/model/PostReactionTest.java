package com.petcare.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.petcare.backend.model.enums.ReactionType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PostReactionTest {
    private User user(long id) { User user = new User(); user.setId(id); return user; }
    private Post post(long id) { Post post = new Post(); post.setId(id); return post; }

    @Test
    void constructorsBuilderAccessorsAndLifecycle_CreateCompositeIdOnlyWhenRelationsExist() {
        PostReaction empty = new PostReaction();
        assertThat(empty.getReactionType()).isEqualTo(ReactionType.LIKE);
        assertThat(PostReaction.builder().build().getReactionType()).isEqualTo(ReactionType.LIKE);
        empty.prePersist(); assertThat(empty.getId()).isNull(); assertThat(empty.getCreatedAt()).isNotNull();
        PostReaction withoutUser = new PostReaction(); withoutUser.setPost(post(11L)); withoutUser.prePersist();
        assertThat(withoutUser.getId()).isNull();

        PostReaction reaction = PostReaction.builder().post(post(10L)).user(user(20L)).reactionType(ReactionType.LOVE).build();
        reaction.prePersist();
        assertThat(reaction.getId()).isEqualTo(new PostReactionId(10L, 20L));
        assertThat(reaction.getReactionType()).isEqualTo(ReactionType.LOVE);
        LocalDateTime initialUpdate = reaction.getUpdatedAt(); reaction.preUpdate();
        assertThat(reaction.getUpdatedAt()).isAfterOrEqualTo(initialUpdate);

        PostReaction existing = new PostReaction(new PostReactionId(1L, 2L), post(9L), user(8L), ReactionType.WOW, null, null);
        existing.prePersist();
        assertThat(existing.getId()).isEqualTo(new PostReactionId(1L, 2L));
        existing.setPost(null); existing.setUser(null); existing.setReactionType(ReactionType.SAD);
        assertThat(existing.getPost()).isNull(); assertThat(existing.getUser()).isNull(); assertThat(existing.getReactionType()).isEqualTo(ReactionType.SAD);
    }
}

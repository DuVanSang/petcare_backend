package com.petcare.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.petcare.backend.model.enums.ReactionType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CommentReactionTest {
    private User user(long id) { User user = new User(); user.setId(id); return user; }
    private PostComment comment(long id) { PostComment comment = new PostComment(); comment.setId(id); return comment; }

    @Test
    void constructorsBuilderAccessorsAndLifecycle_CreateCompositeIdOnlyWhenRelationsExist() {
        CommentReaction empty = new CommentReaction();
        assertThat(empty.getReactionType()).isEqualTo(ReactionType.LIKE);
        assertThat(CommentReaction.builder().build().getReactionType()).isEqualTo(ReactionType.LIKE);
        empty.prePersist(); assertThat(empty.getId()).isNull(); assertThat(empty.getCreatedAt()).isNotNull();
        CommentReaction withoutUser = new CommentReaction(); withoutUser.setComment(comment(11L)); withoutUser.prePersist();
        assertThat(withoutUser.getId()).isNull();

        CommentReaction reaction = CommentReaction.builder().comment(comment(10L)).user(user(20L)).reactionType(ReactionType.ANGRY).build();
        reaction.prePersist();
        assertThat(reaction.getId()).isEqualTo(new CommentReactionId(10L, 20L));
        assertThat(reaction.getReactionType()).isEqualTo(ReactionType.ANGRY);
        LocalDateTime initialUpdate = reaction.getUpdatedAt(); reaction.preUpdate();
        assertThat(reaction.getUpdatedAt()).isAfterOrEqualTo(initialUpdate);

        CommentReaction existing = new CommentReaction(new CommentReactionId(1L, 2L), comment(9L), user(8L), ReactionType.HAHA, null, null);
        existing.prePersist();
        assertThat(existing.getId()).isEqualTo(new CommentReactionId(1L, 2L));
        existing.setComment(null); existing.setUser(null); existing.setReactionType(ReactionType.SAD);
        assertThat(existing.getComment()).isNull(); assertThat(existing.getUser()).isNull(); assertThat(existing.getReactionType()).isEqualTo(ReactionType.SAD);
    }
}

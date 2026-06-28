package com.petcare.backend.service;

import com.petcare.backend.model.Post;
import com.petcare.backend.model.PostComment;
import com.petcare.backend.model.User;

public interface SocialNotificationService {
    void notifyPostReaction(Post post, User actor, String reactionType);

    void notifyPostComment(Post post, PostComment comment, User actor);

    void notifyCommentReaction(PostComment comment, User actor, String reactionType);

    void notifyCommentReply(PostComment parentComment, PostComment replyComment, User actor);
}

package com.petcare.backend.service.impl;

import com.petcare.backend.model.Notification;
import com.petcare.backend.model.Post;
import com.petcare.backend.model.PostComment;
import com.petcare.backend.model.User;
import com.petcare.backend.model.enums.ReactionType;
import com.petcare.backend.repository.NotificationRepository;
import com.petcare.backend.service.SocialNotificationService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocialNotificationServiceImpl implements SocialNotificationService {
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void notifyPostReaction(Post post, User actor, String reactionType) {
        if (post == null || post.getUser() == null || actor == null) {
            return;
        }
        String type = ReactionType.LIKE.getValue().equalsIgnoreCase(reactionType) ? "post_like" : "post_reaction";
        createNotificationIfNotSelf(
                post.getUser(),
                actor,
                "Tương tác mới",
                actorName(actor) + " đã thích bài viết của bạn.",
                type,
                post.getId()
        );
    }

    @Override
    @Transactional
    public void notifyPostComment(Post post, PostComment comment, User actor) {
        if (post == null || post.getUser() == null || comment == null || actor == null) {
            return;
        }
        createNotificationIfNotSelf(
                post.getUser(),
                actor,
                "Bình luận mới",
                actorName(actor) + " đã bình luận về bài viết của bạn.",
                "post_comment",
                comment.getId()
        );
    }

    @Override
    @Transactional
    public void notifyCommentReaction(PostComment comment, User actor, String reactionType) {
        if (comment == null || comment.getUser() == null || comment.getPost() == null || actor == null) {
            return;
        }
        String type = ReactionType.LIKE.getValue().equalsIgnoreCase(reactionType) ? "comment_like" : "comment_reaction";
        createNotificationIfNotSelf(
                comment.getUser(),
                actor,
                "Tương tác mới",
                actorName(actor) + " đã thích bình luận của bạn.",
                type,
                comment.getId()
        );
    }

    @Override
    @Transactional
    public void notifyCommentReply(PostComment parentComment, PostComment replyComment, User actor) {
        if (parentComment == null || parentComment.getUser() == null
                || parentComment.getPost() == null || replyComment == null || actor == null) {
            return;
        }
        createNotificationIfNotSelf(
                parentComment.getUser(),
                actor,
                "Phản hồi mới",
                actorName(actor) + " đã phản hồi bình luận của bạn.",
                "comment_reply",
                replyComment.getId()
        );
    }

    private void createNotificationIfNotSelf(
            User receiver,
            User actor,
            String title,
            String body,
            String type,
            Long referenceId
    ) {
        if (receiver == null || actor == null || receiver.getId() == null || actor.getId() == null) {
            return;
        }
        if (receiver.getId().equals(actor.getId())) {
            return;
        }

        String data = "{\"referenceId\":" + referenceId + "}";
        if (notificationRepository.existsByReceiver_IdAndSender_IdAndTypeAndData(
                receiver.getId(),
                actor.getId(),
                type,
                data
        )) {
            return;
        }

        Notification notification = new Notification();
        notification.setReceiver(receiver);
        notification.setSender(actor);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setType(type);
        notification.setData(data);
        notification.setStatus("sent");
        notification.setIsRead(false);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private String actorName(User actor) {
        if (actor.getFullName() == null || actor.getFullName().isBlank()) {
            return "Ai đó";
        }
        return actor.getFullName();
    }
}

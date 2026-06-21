package com.petcare.backend.service.impl;

import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.friend.request.SendFriendRequestRequest;
import com.petcare.backend.dto.friend.response.FriendCountResponse;
import com.petcare.backend.dto.friend.response.FriendRequestResponse;
import com.petcare.backend.dto.friend.response.FriendResponse;
import com.petcare.backend.dto.friend.response.FriendshipStatusResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ConflictException;
import com.petcare.backend.exception.ForbiddenException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.FriendRequest;
import com.petcare.backend.model.Friendship;
import com.petcare.backend.model.Notification;
import com.petcare.backend.model.User;
import com.petcare.backend.model.enums.FriendRequestStatus;
import com.petcare.backend.repository.FriendRequestRepository;
import com.petcare.backend.repository.FriendshipRepository;
import com.petcare.backend.repository.NotificationRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.service.FriendMapper;
import com.petcare.backend.service.FriendService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {
    private static final int MAX_PAGE_SIZE = 50;

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final FriendMapper friendMapper;

    @Override
    @Transactional
    public FriendRequestResponse sendFriendRequest(Long currentUserId, SendFriendRequestRequest request) {
        User sender = getActiveUserOrThrow(currentUserId);
        if (request == null || request.getReceiverId() == null) {
            throw new BadRequestException("Receiver id is required");
        }

        Long receiverId = request.getReceiverId();
        validatePositiveId(receiverId, "Receiver id");
        if (currentUserId.equals(receiverId)) {
            throw new BadRequestException("You cannot send a friend request to yourself");
        }

        User receiver = getActiveUserOrThrow(receiverId);
        if (friendshipRepository.existsFriendshipBetween(currentUserId, receiverId)) {
            throw new ConflictException("Users are already friends");
        }

        FriendRequest incomingRequest = friendRequestRepository
                .findBySender_IdAndReceiver_IdAndStatus(
                        receiverId,
                        currentUserId,
                        FriendRequestStatus.PENDING
                )
                .orElse(null);
        if (incomingRequest != null) {
            throw new ConflictException("Incoming friend request already exists. Please accept it.");
        }

        FriendRequest existingRequest = friendRequestRepository
                .findBySender_IdAndReceiver_Id(currentUserId, receiverId)
                .orElse(null);
        if (existingRequest != null) {
            if (FriendRequestStatus.PENDING.equals(existingRequest.getStatus())) {
                return friendMapper.toFriendRequestResponse(existingRequest);
            }
            if (FriendRequestStatus.ACCEPTED.equals(existingRequest.getStatus())) {
                throw new ConflictException("Friend request was already accepted");
            }

            existingRequest.setStatus(FriendRequestStatus.PENDING);
            existingRequest.setRespondedAt(null);
            FriendRequest savedRequest = friendRequestRepository.save(existingRequest);
            createFriendRequestNotification(receiver, sender, savedRequest);
            return friendMapper.toFriendRequestResponse(savedRequest);
        }

        FriendRequest friendRequest = FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .build();
        FriendRequest savedRequest = friendRequestRepository.save(friendRequest);
        createFriendRequestNotification(receiver, sender, savedRequest);
        return friendMapper.toFriendRequestResponse(savedRequest);
    }

    @Override
    @Transactional
    public FriendRequestResponse acceptFriendRequest(Long currentUserId, Long requestId) {
        User receiver = getActiveUserOrThrow(currentUserId);
        FriendRequest request = getFriendRequestOrThrow(requestId);
        checkReceiver(currentUserId, request);
        checkPending(request);
        User sender = getActiveUserOrThrow(request.getSender().getId());

        request.setStatus(FriendRequestStatus.ACCEPTED);
        request.setRespondedAt(LocalDateTime.now());
        FriendRequest savedRequest = friendRequestRepository.save(request);

        if (!friendshipRepository.existsFriendshipBetween(sender.getId(), receiver.getId())) {
            friendshipRepository.save(createOrderedFriendship(sender, receiver));
        }

        createFriendAcceptedNotification(sender, receiver, savedRequest);
        return friendMapper.toFriendRequestResponse(savedRequest);
    }

    @Override
    @Transactional
    public FriendRequestResponse declineFriendRequest(Long currentUserId, Long requestId) {
        getActiveUserOrThrow(currentUserId);
        FriendRequest request = getFriendRequestOrThrow(requestId);
        checkReceiver(currentUserId, request);
        checkPending(request);
        request.setStatus(FriendRequestStatus.DECLINED);
        request.setRespondedAt(LocalDateTime.now());
        return friendMapper.toFriendRequestResponse(friendRequestRepository.save(request));
    }

    @Override
    @Transactional
    public void cancelFriendRequest(Long currentUserId, Long requestId) {
        getActiveUserOrThrow(currentUserId);
        FriendRequest request = getFriendRequestOrThrow(requestId);
        Long senderId = request.getSender() == null ? null : request.getSender().getId();
        if (!currentUserId.equals(senderId)) {
            throw new ForbiddenException("Only the sender can cancel this friend request");
        }
        checkPending(request);
        request.setStatus(FriendRequestStatus.CANCELLED);
        request.setRespondedAt(LocalDateTime.now());
        friendRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void unfriend(Long currentUserId, Long friendId) {
        getActiveUserOrThrow(currentUserId);
        validatePositiveId(friendId, "Friend id");
        if (currentUserId.equals(friendId)) {
            throw new BadRequestException("Friend id must be different from current user id");
        }
        if (!userRepository.existsById(friendId)) {
            throw new ResourceNotFoundException("Friend user not found");
        }
        friendshipRepository.deleteFriendshipBetween(currentUserId, friendId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FriendRequestResponse> getIncomingRequests(Long currentUserId, int page, int size) {
        getActiveUserOrThrow(currentUserId);
        Page<FriendRequest> requests = friendRequestRepository
                .findByReceiver_IdAndStatusOrderByCreatedAtDesc(
                        currentUserId,
                        FriendRequestStatus.PENDING,
                        buildPageable(page, size)
                );
        return mapFriendRequestPage(requests);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FriendRequestResponse> getOutgoingRequests(Long currentUserId, int page, int size) {
        getActiveUserOrThrow(currentUserId);
        Page<FriendRequest> requests = friendRequestRepository
                .findBySender_IdAndStatusOrderByCreatedAtDesc(
                        currentUserId,
                        FriendRequestStatus.PENDING,
                        buildPageable(page, size)
                );
        return mapFriendRequestPage(requests);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FriendResponse> getMyFriends(Long currentUserId, int page, int size) {
        getActiveUserOrThrow(currentUserId);
        return mapFriendshipPage(
                friendshipRepository.findFriendshipsOfUser(currentUserId, buildPageable(page, size)),
                currentUserId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FriendResponse> getUserFriends(Long currentUserId, Long userId, int page, int size) {
        getActiveUserOrThrow(currentUserId);
        validatePositiveId(userId, "User id");
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
        return mapFriendshipPage(
                friendshipRepository.findFriendshipsOfUser(userId, buildPageable(page, size)),
                userId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public FriendshipStatusResponse getFriendshipStatus(Long currentUserId, Long targetUserId) {
        getActiveUserOrThrow(currentUserId);
        validatePositiveId(targetUserId, "Target user id");
        if (!userRepository.existsById(targetUserId)) {
            throw new ResourceNotFoundException("Target user not found");
        }
        if (currentUserId.equals(targetUserId)) {
            return friendMapper.toFriendshipStatusResponse(currentUserId, targetUserId, "self", null);
        }
        if (friendshipRepository.existsFriendshipBetween(currentUserId, targetUserId)) {
            return friendMapper.toFriendshipStatusResponse(currentUserId, targetUserId, "friends", null);
        }

        FriendRequest outgoing = friendRequestRepository
                .findBySender_IdAndReceiver_IdAndStatus(
                        currentUserId,
                        targetUserId,
                        FriendRequestStatus.PENDING
                )
                .orElse(null);
        if (outgoing != null) {
            return friendMapper.toFriendshipStatusResponse(
                    currentUserId,
                    targetUserId,
                    "outgoing_pending",
                    outgoing.getId()
            );
        }

        FriendRequest incoming = friendRequestRepository
                .findBySender_IdAndReceiver_IdAndStatus(
                        targetUserId,
                        currentUserId,
                        FriendRequestStatus.PENDING
                )
                .orElse(null);
        if (incoming != null) {
            return friendMapper.toFriendshipStatusResponse(
                    currentUserId,
                    targetUserId,
                    "incoming_pending",
                    incoming.getId()
            );
        }

        List<FriendRequest> history = friendRequestRepository
                .findBetweenOrderByUpdatedAtDesc(currentUserId, targetUserId);
        if (!history.isEmpty()) {
            FriendRequest latest = history.get(0);
            if (FriendRequestStatus.DECLINED.equals(latest.getStatus())) {
                return friendMapper.toFriendshipStatusResponse(
                        currentUserId,
                        targetUserId,
                        "declined",
                        latest.getId()
                );
            }
            if (FriendRequestStatus.CANCELLED.equals(latest.getStatus())) {
                return friendMapper.toFriendshipStatusResponse(
                        currentUserId,
                        targetUserId,
                        "cancelled",
                        latest.getId()
                );
            }
        }
        return friendMapper.toFriendshipStatusResponse(currentUserId, targetUserId, "none", null);
    }

    @Override
    @Transactional(readOnly = true)
    public FriendCountResponse getMyFriendCounts(Long currentUserId) {
        getActiveUserOrThrow(currentUserId);
        return FriendCountResponse.builder()
                .friendsCount(friendshipRepository.countFriendsOfUser(currentUserId))
                .incomingRequestsCount(friendRequestRepository.countByReceiver_IdAndStatus(
                        currentUserId,
                        FriendRequestStatus.PENDING
                ))
                .outgoingRequestsCount(friendRequestRepository.countBySender_IdAndStatus(
                        currentUserId,
                        FriendRequestStatus.PENDING
                ))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean areFriends(Long userId1, Long userId2) {
        if (userId1 == null || userId2 == null || userId1.equals(userId2)) {
            return false;
        }
        return friendshipRepository.existsFriendshipBetween(userId1, userId2);
    }

    private User getActiveUserOrThrow(Long userId) {
        validatePositiveId(userId, "User id");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new ForbiddenException("User account is not active");
        }
        return user;
    }

    private FriendRequest getFriendRequestOrThrow(Long requestId) {
        validatePositiveId(requestId, "Friend request id");
        return friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));
    }

    private void checkReceiver(Long currentUserId, FriendRequest request) {
        Long receiverId = request.getReceiver() == null ? null : request.getReceiver().getId();
        if (!currentUserId.equals(receiverId)) {
            throw new ForbiddenException("Only the receiver can respond to this friend request");
        }
    }

    private void checkPending(FriendRequest request) {
        if (!FriendRequestStatus.PENDING.equals(request.getStatus())) {
            throw new ConflictException("Friend request is no longer pending");
        }
    }

    private Friendship createOrderedFriendship(User firstUser, User secondUser) {
        User user1 = firstUser.getId() < secondUser.getId() ? firstUser : secondUser;
        User user2 = firstUser.getId() < secondUser.getId() ? secondUser : firstUser;
        return Friendship.builder()
                .user1(user1)
                .user2(user2)
                .build();
    }

    private Pageable buildPageable(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page must not be negative");
        }
        if (size <= 0) {
            throw new BadRequestException("Size must be greater than 0");
        }
        return PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
    }

    private PageResponse<FriendRequestResponse> mapFriendRequestPage(Page<FriendRequest> requests) {
        return PageResponse.<FriendRequestResponse>builder()
                .content(requests.getContent().stream()
                        .map(friendMapper::toFriendRequestResponse)
                        .toList())
                .page(requests.getNumber())
                .size(requests.getSize())
                .totalElements(requests.getTotalElements())
                .totalPages(requests.getTotalPages())
                .first(requests.isFirst())
                .last(requests.isLast())
                .build();
    }

    private PageResponse<FriendResponse> mapFriendshipPage(
            Page<Friendship> friendships,
            Long profileUserId
    ) {
        return PageResponse.<FriendResponse>builder()
                .content(friendships.getContent().stream()
                        .map(friendship -> friendMapper.toFriendResponse(friendship, profileUserId))
                        .toList())
                .page(friendships.getNumber())
                .size(friendships.getSize())
                .totalElements(friendships.getTotalElements())
                .totalPages(friendships.getTotalPages())
                .first(friendships.isFirst())
                .last(friendships.isLast())
                .build();
    }

    private void createFriendRequestNotification(User receiver, User sender, FriendRequest request) {
        Notification notification = new Notification();
        notification.setUser(receiver);
        notification.setTitle("Lời mời kết bạn");
        notification.setBody(resolveName(sender) + " đã gửi cho bạn lời mời kết bạn.");
        notification.setType("friend_request");
        notification.setData("{\"friendRequestId\":" + request.getId() + "}");
        notification.setStatus("pending");
        notificationRepository.save(notification);
    }

    private void createFriendAcceptedNotification(User sender, User receiver, FriendRequest request) {
        Notification notification = new Notification();
        notification.setUser(sender);
        notification.setTitle("Lời mời kết bạn đã được chấp nhận");
        notification.setBody(resolveName(receiver) + " đã chấp nhận lời mời kết bạn của bạn.");
        notification.setType("friend_request_accepted");
        notification.setData("{\"friendRequestId\":" + request.getId() + "}");
        notification.setStatus("pending");
        notificationRepository.save(notification);
    }

    private String resolveName(User user) {
        if (StringUtils.hasText(user.getFullName())) {
            return user.getFullName();
        }
        if (StringUtils.hasText(user.getEmail())) {
            return user.getEmail();
        }
        return "User " + user.getId();
    }

    private void validatePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new BadRequestException(fieldName + " must be greater than 0");
        }
    }
}

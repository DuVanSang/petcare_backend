package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Notification;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AdminNotificationLogServiceImplTest {
    @Mock NotificationRepository repository;
    private AdminNotificationLogServiceImpl service;
    @BeforeEach void setUp() { service = new AdminNotificationLogServiceImpl(repository); }
    private User user(long id, String name, String email) { User user = new User(); user.setId(id); user.setFullName(name); user.setEmail(email); return user; }
    private Notification notification() {
        Notification notification = new Notification(); notification.setId(3L); notification.setReceiver(user(1L, "Receiver", "r@pet.test")); notification.setSender(user(2L, "Actor", "a@pet.test"));
        notification.setTitle("Title"); notification.setBody("Body"); notification.setType("post"); notification.setData("{\"referenceId\":9}"); notification.setStatus("sent"); notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0)); notification.setSentAt(LocalDateTime.of(2025, 1, 1, 10, 1)); return notification;
    }

    @Test void listValidatesTimePageAndSizeBeforeRepository() {
        LocalDateTime from = LocalDateTime.now(), to = from.minusMinutes(1);
        assertThatThrownBy(() -> service.getNotificationLogs(null, null, null, null, from, to, 0, 1)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.getNotificationLogs(null, null, null, null, null, null, -1, 1)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.getNotificationLogs(null, null, null, null, null, null, 0, 0)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(repository);
    }

    @Test void listAcceptsFromOnlyAndToOnlyTimeBounds() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        LocalDateTime now = LocalDateTime.now();
        assertThat(service.getNotificationLogs(null, null, null, null, now, null, 0, 1).getContent()).isEmpty();
        assertThat(service.getNotificationLogs(null, null, null, null, null, now, 0, 1).getContent()).isEmpty();
        verify(repository, times(2)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test void listMapsEmptyAndFullNotificationPageAndCapsSize() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()), new PageImpl<>(List.of(notification())));
        assertThat(service.getNotificationLogs(null, null, null, null, null, null, 0, 1).getContent()).isEmpty();
        var result = service.getNotificationLogs(1L, " POST ", " SENT ", true, LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 2, 0, 0), 2, 101);
        assertThat(result.getContent()).singleElement().satisfies(response -> {
            assertThat(response.getReceiverId()).isEqualTo(1L); assertThat(response.getSenderId()).isEqualTo(2L); assertThat(response.getType()).isEqualTo("post"); assertThat(response.getRead()).isFalse();
        });
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class); verify(repository, times(2)).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getAllValues().get(1).getPageNumber()).isEqualTo(2); assertThat(pageable.getAllValues().get(1).getPageSize()).isEqualTo(100);
    }

    @Test void detailMapsOptionalFieldsAndThrowsWhenMissing() {
        Notification notification = notification(); notification.setReceiver(null); notification.setSender(null); notification.setData(null); notification.setIsRead(null); notification.setReadAt(null);
        when(repository.findById(3L)).thenReturn(Optional.of(notification));
        var response = service.getNotificationLogDetail(3L);
        assertThat(response.getId()).isEqualTo(3L); assertThat(response.getReceiverId()).isNull(); assertThat(response.getSenderId()).isNull(); assertThat(response.getData()).isNull(); assertThat(response.getRead()).isFalse();
        when(repository.findById(4L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getNotificationLogDetail(4L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test void specificationExecutesAllPresentAndAbsentFilterBranches() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        service.getNotificationLogs(1L, " type ", " sent ", true, LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 2, 0, 0), 0, 20);
        service.getNotificationLogs(null, "  ", "", false, null, null, 0, 20);
        service.getNotificationLogs(null, null, null, null, null, null, 0, 20);
        ArgumentCaptor<Specification> specs = ArgumentCaptor.forClass(Specification.class); verify(repository, times(3)).findAll(specs.capture(), any(Pageable.class));
        for (Specification<Notification> specification : (List<Specification<Notification>>) (List) specs.getAllValues()) executeSpecification(specification);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void executeSpecification(Specification<Notification> specification) {
        Root root = mock(Root.class); CriteriaQuery query = mock(CriteriaQuery.class); CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path receiver = mock(Path.class), receiverId = mock(Path.class), type = mock(Path.class), status = mock(Path.class), read = mock(Path.class), created = mock(Path.class);
        Expression lowerType = mock(Expression.class), lowerStatus = mock(Expression.class); Predicate predicate = mock(Predicate.class);
        when(root.get("receiver")).thenReturn(receiver); when(receiver.get("id")).thenReturn(receiverId); when(root.get("type")).thenReturn(type); when(root.get("status")).thenReturn(status); when(root.get("isRead")).thenReturn(read); when(root.get("createdAt")).thenReturn(created);
        when(cb.lower(type)).thenReturn(lowerType); when(cb.lower(status)).thenReturn(lowerStatus); doReturn(predicate).when(cb).equal(any(Expression.class), any(Object.class)); when(cb.isFalse(read)).thenReturn(predicate); when(cb.isTrue(read)).thenReturn(predicate); when(cb.greaterThanOrEqualTo(any(Expression.class), any(LocalDateTime.class))).thenReturn(predicate); when(cb.lessThanOrEqualTo(any(Expression.class), any(LocalDateTime.class))).thenReturn(predicate); when(cb.and(any(Predicate[].class))).thenReturn(predicate);
        assertThat(specification.toPredicate(root, query, cb)).isSameAs(predicate);
    }
}

package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.friend.request.SendFriendRequestRequest;
import com.petcare.backend.dto.friend.response.*;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.FriendService;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness=org.mockito.quality.Strictness.LENIENT)
class FriendControllerTest {
 @Mock FriendService service;@Mock UserPrincipal principal;FriendController c;
 @BeforeEach void init(){c=new FriendController(service);when(principal.getId()).thenReturn(1L);}
 @Test void allEndpointsDelegate(){var req=new SendFriendRequestRequest();var fr=mock(FriendRequestResponse.class);var fp=PageResponse.<FriendRequestResponse>builder().content(List.of()).build();var friends=PageResponse.<FriendResponse>builder().content(List.of()).build();var status=mock(FriendshipStatusResponse.class);var counts=mock(FriendCountResponse.class);when(service.sendFriendRequest(1L,req)).thenReturn(fr);when(service.acceptFriendRequest(1L,2L)).thenReturn(fr);when(service.declineFriendRequest(1L,2L)).thenReturn(fr);when(service.getIncomingRequests(1L,0,20)).thenReturn(fp);when(service.getOutgoingRequests(1L,0,20)).thenReturn(fp);when(service.getMyFriends(1L,0,20)).thenReturn(friends);when(service.getUserFriends(1L,2L,0,20)).thenReturn(friends);when(service.getFriendshipStatus(1L,2L)).thenReturn(status);when(service.getMyFriendCounts(1L)).thenReturn(counts);assertThat(c.sendFriendRequest(principal,req).getBody().getData()).isSameAs(fr);assertThat(c.acceptFriendRequest(principal,2L).getBody().getData()).isSameAs(fr);assertThat(c.declineFriendRequest(principal,2L).getBody().getData()).isSameAs(fr);assertThat(c.getIncomingRequests(principal,0,20).getBody().getData()).isSameAs(fp);assertThat(c.getOutgoingRequests(principal,0,20).getBody().getData()).isSameAs(fp);assertThat(c.getMyFriends(principal,0,20).getBody().getData()).isSameAs(friends);assertThat(c.getUserFriends(principal,2L,0,20).getBody().getData()).isSameAs(friends);assertThat(c.getFriendshipStatus(principal,2L).getBody().getData()).isSameAs(status);assertThat(c.getMyFriendCounts(principal).getBody().getData()).isSameAs(counts);c.cancelFriendRequest(principal,2L);c.unfriend(principal,2L);verify(service).cancelFriendRequest(1L,2L);verify(service).unfriend(1L,2L);}
}

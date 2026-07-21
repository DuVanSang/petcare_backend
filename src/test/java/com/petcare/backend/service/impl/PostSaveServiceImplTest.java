package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.post.response.PostResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Post;
import com.petcare.backend.model.PostSave;
import com.petcare.backend.model.User;
import com.petcare.backend.model.enums.PostStatus;
import com.petcare.backend.repository.PostRepository;
import com.petcare.backend.repository.PostSaveRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.PostService;
import com.petcare.backend.service.SocialPermissionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostSaveServiceImplTest {
    @Mock PostRepository postRepository; @Mock PostSaveRepository postSaveRepository; @Mock UserRepository userRepository; @Mock SocialPermissionService permissions; @Mock PostService postService; @Mock UserPrincipal principal;
    PostSaveServiceImpl service; static final long USER=7L, POST=11L;
    @BeforeEach void setUp(){service=new PostSaveServiceImpl(postRepository,postSaveRepository,userRepository,permissions,postService);when(principal.getId()).thenReturn(USER);}
    private Post post(){return Post.builder().id(POST).status(PostStatus.PUBLISHED).build();}
    @Test void savePost_NewAndDuplicate_StoresOnlyNewSave(){Post p=post();when(postRepository.findById(POST)).thenReturn(Optional.of(p));when(userRepository.findById(USER)).thenReturn(Optional.of(new User()));when(postService.buildPostResponse(p,USER)).thenReturn(new PostResponse());service.savePost(principal,POST);verify(postSaveRepository).save(any());when(postSaveRepository.existsByPost_IdAndUser_Id(POST,USER)).thenReturn(true);service.savePost(principal,POST);verify(postService,org.mockito.Mockito.times(2)).buildPostResponse(p,USER);}
    @Test void savePost_InvalidIdMissingPostUserAndStatus_AreRejected(){assertThatThrownBy(()->service.savePost(principal,0L)).isInstanceOf(BadRequestException.class);when(postRepository.findById(POST)).thenReturn(Optional.empty());assertThatThrownBy(()->service.savePost(principal,POST)).isInstanceOf(ResourceNotFoundException.class);Post hidden=post();hidden.setStatus(PostStatus.HIDDEN);when(postRepository.findById(POST)).thenReturn(Optional.of(hidden));assertThatThrownBy(()->service.savePost(principal,POST)).isInstanceOf(BadRequestException.class);Post p=post();when(postRepository.findById(POST)).thenReturn(Optional.of(p));when(userRepository.findById(USER)).thenReturn(Optional.empty());assertThatThrownBy(()->service.savePost(principal,POST)).isInstanceOf(ResourceNotFoundException.class);}
    @Test void unsavePost_ExistingAndMissingSave_AreHandled(){Post p=post();PostSave save=new PostSave();when(postRepository.findById(POST)).thenReturn(Optional.of(p));when(postSaveRepository.findByPost_IdAndUser_Id(POST,USER)).thenReturn(Optional.of(save));service.unsavePost(principal,POST);verify(postSaveRepository).delete(save);when(postSaveRepository.findByPost_IdAndUser_Id(POST,USER)).thenReturn(Optional.empty());assertThatThrownBy(()->service.unsavePost(principal,POST)).isInstanceOf(ResourceNotFoundException.class);}
    @Test void getSavedPosts_EmptyPageAndBounds_AreHandled(){when(postSaveRepository.findVisibleSavedPosts(eq(USER),any(),any(),any(),any())).thenReturn(new PageImpl<>(List.of()));assertThat(service.getSavedPosts(principal,0,50).getContent()).isEmpty();assertThatThrownBy(()->service.getSavedPosts(principal,-1,1)).isInstanceOf(BadRequestException.class);assertThatThrownBy(()->service.getSavedPosts(principal,0,0)).isInstanceOf(BadRequestException.class);}
}

package com.petcare.backend.repository;

import com.petcare.backend.model.CommentMedia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentMediaRepository extends JpaRepository<CommentMedia, Long> {
    List<CommentMedia> findByComment_IdOrderByDisplayOrderAsc(Long commentId);

    List<CommentMedia> findByComment_IdInOrderByDisplayOrderAsc(List<Long> commentIds);

    void deleteByComment_Id(Long commentId);
}

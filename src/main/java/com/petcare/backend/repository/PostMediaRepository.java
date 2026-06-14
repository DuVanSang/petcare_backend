package com.petcare.backend.repository;

import com.petcare.backend.model.PostMedia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {
    List<PostMedia> findByPost_IdOrderByDisplayOrderAsc(Long postId);

    void deleteByPost_Id(Long postId);
}

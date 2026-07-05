package com.petcare.backend.repository;

import com.petcare.backend.model.BlogSave;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogSaveRepository extends JpaRepository<BlogSave, Long> {
    boolean existsByBlog_IdAndUser_Id(Long blogId, Long userId);

    Optional<BlogSave> findByBlog_IdAndUser_Id(Long blogId, Long userId);

    Page<BlogSave> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

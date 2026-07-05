package com.petcare.backend.repository;

import com.petcare.backend.model.Blog;
import com.petcare.backend.model.Blog.BlogCategory;
import com.petcare.backend.model.Blog.BlogStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlogRepository extends JpaRepository<Blog, Long> {
    boolean existsBySlug(String slug);

    Optional<Blog> findByIdAndStatus(Long id, BlogStatus status);

    @Query("""
            SELECT blog FROM Blog blog
            WHERE blog.status = :status
              AND (:category IS NULL OR blog.category = :category)
              AND (
                    :keyword IS NULL
                    OR LOWER(blog.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(blog.summary) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY blog.publishedAt DESC, blog.createdAt DESC
            """)
    Page<Blog> searchPublished(
            @Param("status") BlogStatus status,
            @Param("keyword") String keyword,
            @Param("category") BlogCategory category,
            Pageable pageable
    );

    @Query("""
            SELECT blog FROM Blog blog
            WHERE (:status IS NULL OR blog.status = :status)
              AND (:category IS NULL OR blog.category = :category)
              AND (
                    :keyword IS NULL
                    OR LOWER(blog.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(blog.summary) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY blog.createdAt DESC
            """)
    Page<Blog> searchAdmin(
            @Param("keyword") String keyword,
            @Param("category") BlogCategory category,
            @Param("status") BlogStatus status,
            Pageable pageable
    );
}

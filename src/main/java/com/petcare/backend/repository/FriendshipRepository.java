package com.petcare.backend.repository;

import com.petcare.backend.model.Friendship;
import com.petcare.backend.model.FriendshipId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, FriendshipId> {
    @Query("""
            SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
            FROM Friendship f
            WHERE (f.user1.id = :userId1 AND f.user2.id = :userId2)
               OR (f.user1.id = :userId2 AND f.user2.id = :userId1)
            """)
    boolean existsFriendshipBetween(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2
    );

    @Query("""
            SELECT f
            FROM Friendship f
            WHERE f.user1.id = :userId OR f.user2.id = :userId
            ORDER BY f.createdAt DESC
            """)
    Page<Friendship> findFriendshipsOfUser(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Modifying
    @Query("""
            DELETE FROM Friendship f
            WHERE (f.user1.id = :userId1 AND f.user2.id = :userId2)
               OR (f.user1.id = :userId2 AND f.user2.id = :userId1)
            """)
    void deleteFriendshipBetween(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2
    );

    @Query("""
            SELECT COUNT(f)
            FROM Friendship f
            WHERE f.user1.id = :userId OR f.user2.id = :userId
            """)
    long countFriendsOfUser(@Param("userId") Long userId);
}

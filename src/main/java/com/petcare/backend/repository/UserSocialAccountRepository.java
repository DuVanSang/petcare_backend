package com.petcare.backend.repository;

import com.petcare.backend.model.UserSocialAccount;
import com.petcare.backend.model.enums.SocialProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {
    Optional<UserSocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}

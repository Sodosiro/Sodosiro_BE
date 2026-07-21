package com.sodosiro.domain.user.repository;

import com.sodosiro.domain.user.constants.Provider;
import com.sodosiro.domain.user.entity.SocialAccounts;
import com.sodosiro.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocialRepository extends JpaRepository<SocialAccounts,Long> {

    Optional<SocialAccounts> findByProviderAndProviderId(Provider provider, String providerId);

    List<SocialAccounts> findAllByUser(User user);
}
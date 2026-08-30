package com.sodosiro.domain.user.repository;

import com.sodosiro.domain.user.entity.User;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByNickName(String nickName);

    boolean existsByNickNameAndUserIdNot(String nickName, Long userId);

    List<User> findByWithdrawnAtLessThanEqualOrderByWithdrawnAtAsc(LocalDateTime threshold, Limit limit);
}
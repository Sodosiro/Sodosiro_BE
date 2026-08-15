package com.sodosiro.domain.travel.repository;

import com.sodosiro.domain.travel.entity.SpotEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotEmbeddingRepository extends JpaRepository<SpotEmbedding, Long>, SpotEmbeddingQueryRepository {
}

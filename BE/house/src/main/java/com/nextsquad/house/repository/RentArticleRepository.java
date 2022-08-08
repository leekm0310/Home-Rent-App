package com.nextsquad.house.repository;

import com.nextsquad.house.domain.house.RentArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RentArticleRepository extends JpaRepository<RentArticle, Long> {
    @Query("select r from RentArticle r where r.isDeleted = false and r.isCompleted = false")
    List<RentArticle> findAllAvailable();
}

package com.project.hsf.repository;

import com.project.hsf.entity.SeafoodProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeafoodProductRepository
        extends JpaRepository<SeafoodProduct, Long>, JpaSpecificationExecutor<SeafoodProduct> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE SeafoodProduct s SET s.stockQuantity = s.stockQuantity - :qty WHERE s.id = :id AND s.stockQuantity >= :qty AND s.active = true")
    int deductStock(@Param("id") Long id, @Param("qty") int qty);

}

package com.project.hsf.repository;

import com.project.hsf.entity.SeafoodProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SeafoodProductRepository extends JpaRepository<SeafoodProduct, Long>, JpaSpecificationExecutor<SeafoodProduct> {
}

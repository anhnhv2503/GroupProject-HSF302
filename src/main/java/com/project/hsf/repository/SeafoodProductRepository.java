package com.project.hsf.repository;

import com.project.hsf.entity.SeafoodProduct;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeafoodProductRepository
        extends JpaRepository<SeafoodProduct, Long>, JpaSpecificationExecutor<SeafoodProduct> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE SeafoodProduct s SET s.stockQuantity = s.stockQuantity - :qty WHERE s.id = :id AND s.stockQuantity >= :qty AND s.active = true")
    int deductStock(@Param("id") Long id, @Param("qty") int qty);

    /**
     * Returns stock when an order is cancelled or its payment window expires.
     *
     * There is no guard condition here because this is a compensating step: the quantity being
     * returned is always a quantity that was successfully deducted earlier, read from order_items.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE SeafoodProduct s SET s.stockQuantity = s.stockQuantity + :qty WHERE s.id = :id")
    int restoreStock(@Param("id") Long id, @Param("qty") int qty);

    /**
     * The product list page reads product.category.name and product.images on every row.
     * Without an entity graph Hibernate issues 1 + 2N queries (1 for products, N for categories,
     * N for images). Category is fetch-joined here; images are batched via @BatchSize on the
     * collection itself, because fetch-joining both would produce a cartesian product.
     */
    @Override
    @EntityGraph(attributePaths = { "category" })
    List<SeafoodProduct> findAll(Specification<SeafoodProduct> spec, Sort sort);

    @Override
    @EntityGraph(attributePaths = { "category" })
    List<SeafoodProduct> findAll();

    List<SeafoodProduct> findTop4ByOrderByImportedDateDesc();

}

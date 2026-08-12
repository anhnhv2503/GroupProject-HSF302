package com.project.hsf.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.project.hsf.entity.Category;
import com.project.hsf.entity.ProductImage;
import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.specification.SeafoodProductSpecification;

import jakarta.persistence.EntityManager;

/**
 * Measures how many queries the product list page actually issues.
 *
 * The admin page product-manage.html reads {@code product.category.name} and {@code product.images}
 * on every row. Both associations are LAZY, so a plain {@code findAll()} followed by Thymeleaf
 * touching them makes Hibernate fire an extra query per row - the query count grows with the number
 * of products.
 *
 * This test pins the result down with a number: after adding the entity graph and @BatchSize, the
 * query count no longer depends on how many products there are.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ProductListQueryCountTest {

    private static final int PRODUCT_COUNT = 30;
    private static final int IMAGES_PER_PRODUCT = 2;

    @Autowired
    private SeafoodProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setName("Ca bien");
        category.setActive(true);
        Category savedCategory = categoryRepository.save(category);

        for (int i = 0; i < PRODUCT_COUNT; i++) {
            SeafoodProduct product = new SeafoodProduct();
            product.setCategory(savedCategory);
            product.setName("San pham " + i);
            product.setPrice(new BigDecimal("100000"));
            product.setStockQuantity(10);
            product.setFreshnessStatus("FRESH");
            product.setSoldCount(0);
            product.setActive(true);
            product.setUnit("kg");
            for (int j = 0; j < IMAGES_PER_PRODUCT; j++) {
                ProductImage image = new ProductImage();
                image.setProduct(product);
                image.setImageUrl("http://example.test/" + i + "-" + j + ".jpg");
                image.setIsPrimary(j == 0);
                product.getImages().add(image);
            }
            productRepository.save(product);
        }

        entityManager.flush();
        entityManager.clear();

        statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    @DisplayName("Product list: query count does not grow with the number of rows")
    void productList_queryCountDoesNotGrowWithRowCount() {
        List<SeafoodProduct> products = productRepository.findAll(
                SeafoodProductSpecification.filter(null, null, null, null),
                Sort.by(Sort.Direction.ASC, "id"));

        // Do exactly what the template does on each row.
        for (SeafoodProduct product : products) {
            product.getCategory().getName();
            product.getImages().size();
        }

        long queries = statistics.getPrepareStatementCount();

        assertThat(products).hasSize(PRODUCT_COUNT);
        // 1 query for products (category fetch-joined) + 1 batched query for images via
        // @BatchSize(32). Left fully lazy, 30 products would cost 1 + 30 + 30 = 61 queries.
        assertThat(queries)
                .as("reading %d products with category and images takes a constant 2 queries, not 1+2N",
                        PRODUCT_COUNT)
                .isEqualTo(2);
    }

    @Test
    @DisplayName("Without an entity graph, category causes a genuine N+1")
    void withoutEntityGraph_categoryCausesNPlusOne() {
        // Plain JPQL with no entity graph - this is the state before the fix.
        List<SeafoodProduct> products = entityManager
                .createQuery("SELECT p FROM SeafoodProduct p ORDER BY p.id", SeafoodProduct.class)
                .getResultList();

        long afterSelect = statistics.getPrepareStatementCount();
        for (SeafoodProduct product : products) {
            product.getCategory().getName();
        }
        long afterTouchingCategory = statistics.getPrepareStatementCount();

        // Merely touching category adds queries. That is exactly the N+1 removed by
        // @EntityGraph(attributePaths = "category") on the repository.
        assertThat(afterTouchingCategory)
                .as("touching category per row adds queries when there is no fetch join")
                .isGreaterThan(afterSelect);
    }
}

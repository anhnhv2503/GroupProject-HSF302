package com.project.hsf.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.project.hsf.entity.Category;
import com.project.hsf.entity.SeafoodProduct;

/**
 * Verifies that stock deduction stays correct when several customers buy the same product at once.
 *
 * This is the classic lost update problem: two requests both read stock = 1, both conclude the item
 * is available, both write 0, and the shop sells two crabs when it only had one.
 *
 * {@code @Transactional(propagation = NOT_SUPPORTED)} is required: by default @DataJpaTest wraps the
 * whole test in one transaction and rolls it back, so the threads would not see each other's data
 * and the test would prove nothing. Here each unit of work opens its own transaction through
 * TransactionTemplate and really commits.
 */
// Replace.NONE so the datasource declared in application-test.properties is used as written,
// rather than being swapped for an auto-configured embedded one.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class StockDeductionConcurrencyTest {

    private static final int INITIAL_STOCK = 5;
    private static final int CONCURRENT_BUYERS = 20;

    @Autowired
    private SeafoodProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionTemplate txTemplate;

    private Long productId;

    @BeforeEach
    void setUp() {
        txTemplate.executeWithoutResult(tx -> {
            productRepository.deleteAll();
            categoryRepository.deleteAll();

            Category category = new Category();
            category.setName("Cua");
            category.setActive(true);
            Category savedCategory = categoryRepository.save(category);

            SeafoodProduct product = new SeafoodProduct();
            product.setCategory(savedCategory);
            product.setName("Cua hoang de");
            product.setPrice(new BigDecimal("1500000"));
            product.setStockQuantity(INITIAL_STOCK);
            product.setFreshnessStatus("FRESH");
            product.setSoldCount(0);
            product.setActive(true);
            product.setUnit("con");
            productId = productRepository.save(product).getId();
        });
    }

    @Test
    @DisplayName("20 buyers race for 5 units: exactly 5 succeed, stock lands at 0, never negative")
    void deductStock_neverOversells_underConcurrency() throws Exception {
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        runConcurrently(CONCURRENT_BUYERS, () -> {
            try {
                Integer affected = txTemplate.execute(tx -> productRepository.deductStock(productId, 1));
                if (affected != null && affected > 0) {
                    succeeded.incrementAndGet();
                } else {
                    rejected.incrementAndGet();
                }
            } catch (Exception e) {
                // H2/SQL Server may raise a locking error when two transactions contend for the same
                // row. Rejected by a lock error or by affected = 0 is the same thing in business
                // terms: the sale did not happen.
                rejected.incrementAndGet();
            }
            return null;
        });

        int finalStock = currentStock();

        // Everything real is sold and nothing more: no overselling, no phantom shortage either.
        assertThat(succeeded.get())
                .as("exactly %d purchases succeed, the other %d are rejected", INITIAL_STOCK,
                        CONCURRENT_BUYERS - INITIAL_STOCK)
                .isEqualTo(INITIAL_STOCK);
        assertThat(finalStock).as("stock lands exactly at 0 and is never negative").isZero();
        assertThat(succeeded.get() + finalStock)
                .as("sold + remaining must equal the initial stock")
                .isEqualTo(INITIAL_STOCK);
        assertThat(succeeded.get() + rejected.get()).isEqualTo(CONCURRENT_BUYERS);
    }

    @Test
    @DisplayName("One unit left, 10 buyers racing: exactly one wins")
    void deductStock_lastItem_onlyOneBuyerWins() throws Exception {
        txTemplate.executeWithoutResult(tx -> {
            SeafoodProduct product = productRepository.findById(productId).orElseThrow();
            product.setStockQuantity(1);
            productRepository.save(product);
        });

        AtomicInteger succeeded = new AtomicInteger();

        runConcurrently(10, () -> {
            try {
                Integer affected = txTemplate.execute(tx -> productRepository.deductStock(productId, 1));
                if (affected != null && affected > 0) {
                    succeeded.incrementAndGet();
                }
            } catch (Exception ignored) {
                // Blocked by a lock means no sale, which does not count as success.
            }
            return null;
        });

        assertThat(succeeded.get()).as("only one customer gets the last crab").isEqualTo(1);
        assertThat(currentStock()).isZero();
    }

    @Test
    @DisplayName("A single request cannot buy more than the remaining stock")
    void deductStock_rejectsQuantityAboveStock() {
        Integer affected = txTemplate.execute(tx -> productRepository.deductStock(productId, INITIAL_STOCK + 1));

        assertThat(affected).isZero();
        assertThat(currentStock()).isEqualTo(INITIAL_STOCK);
    }

    @Test
    @DisplayName("Stock cannot be deducted from a deactivated product")
    void deductStock_rejectsInactiveProduct() {
        txTemplate.executeWithoutResult(tx -> {
            SeafoodProduct product = productRepository.findById(productId).orElseThrow();
            product.setActive(false);
            productRepository.save(product);
        });

        Integer affected = txTemplate.execute(tx -> productRepository.deductStock(productId, 1));

        assertThat(affected).isZero();
        assertThat(currentStock()).isEqualTo(INITIAL_STOCK);
    }

    /**
     * Demonstrates why a conditional UPDATE is needed instead of read-then-write.
     *
     * Both transactions read the same stock value before either one writes, each computes
     * "5 - 1 = 4" on its own, and they overwrite each other. The result is two orders but stock down
     * by only one: one sale that never deducted anything. That is a lost update.
     */
    @Test
    @DisplayName("Read-then-write loses an update: 2 sales but stock drops by only 1")
    void readThenWrite_losesUpdate_provingWhyConditionalUpdateIsNeeded() throws Exception {
        CountDownLatch bothHaveRead = new CountDownLatch(2);

        Callable<Void> naiveBuy = () -> {
            txTemplate.executeWithoutResult(tx -> {
                // Read step
                SeafoodProduct product = productRepository.findById(productId).orElseThrow();
                int stockSeen = product.getStockQuantity();

                // Hold both threads until both have read, forcing the race to happen instead of
                // depending on the luck of the scheduler.
                bothHaveRead.countDown();
                try {
                    bothHaveRead.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Write step, based on the value read earlier - which may now be stale
                product.setStockQuantity(stockSeen - 1);
                productRepository.saveAndFlush(product);
            });
            return null;
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Void>> futures = List.of(pool.submit(naiveBuy), pool.submit(naiveBuy));
            for (Future<Void> future : futures) {
                try {
                    future.get(15, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                    // One side may fail on a lock; only the final state matters here.
                }
            }
        } finally {
            pool.shutdownNow();
        }

        int finalStock = currentStock();

        // Selling 2 units should leave stock at 3. Read-then-write leaves 4 - one update was lost.
        assertThat(finalStock)
                .as("read-then-write leaves the wrong stock: 2 sold but only 1 deducted")
                .isGreaterThan(INITIAL_STOCK - 2);
    }

    private void runConcurrently(int threads, Callable<Void> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    // Everyone waits at the same starting line so the calls really do overlap.
                    startGate.await(10, TimeUnit.SECONDS);
                    return task.call();
                }));
            }
            startGate.countDown();
            for (Future<Void> future : futures) {
                future.get(20, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private int currentStock() {
        Integer stock = txTemplate.execute(
                tx -> productRepository.findById(productId).orElseThrow().getStockQuantity());
        return stock != null ? stock : -1;
    }
}

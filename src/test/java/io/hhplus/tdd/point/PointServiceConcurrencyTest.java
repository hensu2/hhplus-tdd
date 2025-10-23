package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("포인트 서비스 동시성 테스트")
public class PointServiceConcurrencyTest {

    private final UserPointTable userPointTable = new UserPointTable();
    private final PointHistoryTable pointHistoryTable = new PointHistoryTable();
    private final PointService pointService = new PointService(userPointTable, pointHistoryTable);

    @Test
    @DisplayName("동시에 여러 스레드가 같은 사용자의 포인트를 충전해도 정확한 금액이 반영된다")
    void concurrentChargeTest() throws InterruptedException {
        // given
        long userId = 1L;
        int threadCount = 10;
        long chargeAmount = 100L;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // when: 10개 스레드가 동시에 100포인트씩 충전
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePoint(userId, chargeAmount);
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // then: 1000 포인트가 정확히 충전되어야 함
        UserPoint result = pointService.getUserPoint(userId);
        assertThat(result.point()).isEqualTo(1000L);
        assertThat(successCount.get()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("동시에 충전과 사용이 발생해도 정확한 금액이 반영된다")
    void concurrentChargeAndUseTest() throws InterruptedException {
        // given
        long userId = 2L;
        long initialAmount = 10000L;
        int threadCount = 20;

        // 초기 포인트 충전
        pointService.chargePoint(userId, initialAmount);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // when: 10개 스레드는 충전(+100), 10개 스레드는 사용(-100)
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    if (index % 2 == 0) {
                        pointService.chargePoint(userId, 100L);
                    } else {
                        pointService.usePoint(userId, 100L);
                    }
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // then: 초기 금액 10000이 유지되어야 함 (10번 충전 +1000, 10번 사용 -1000)
        UserPoint result = pointService.getUserPoint(userId);
        assertThat(result.point()).isEqualTo(initialAmount);
        assertThat(successCount.get()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("잔액이 부족한 상황에서 동시에 여러 스레드가 사용을 시도하면 일부만 성공한다")
    void concurrentUseWithInsufficientBalance() throws InterruptedException {
        // given
        long userId = 3L;
        long initialAmount = 1000L;
        int threadCount = 20;
        long useAmount = 100L;

        // 초기 포인트 충전 (1000 포인트)
        pointService.chargePoint(userId, initialAmount);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 20개 스레드가 동시에 100포인트씩 사용 시도 (총 2000 포인트 필요하지만 1000만 보유)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.usePoint(userId, useAmount);
                    successCount.incrementAndGet();
                } catch (IllegalArgumentException e) {
                    // 잔액 부족으로 실패
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // then: 10번만 성공하고 10번은 실패해야 함
        UserPoint result = pointService.getUserPoint(userId);
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(10);
        assertThat(result.point()).isEqualTo(0L); // 1000 - (10 * 100) = 0
    }

    @Test
    @DisplayName("동시에 여러 스레드가 0원 이하 금액으로 충전을 시도하면 모두 실패한다")
    void concurrentChargeWithInvalidAmount() throws InterruptedException {
        // given
        long userId = 4L;
        int threadCount = 10;
        long invalidAmount = -100L;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 10개 스레드가 동시에 -100 포인트 충전 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePoint(userId, invalidAmount);
                } catch (IllegalArgumentException e) {
                    // 음수 금액으로 실패
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // then: 모두 실패하고 포인트는 0으로 유지
        UserPoint result = pointService.getUserPoint(userId);
        assertThat(failCount.get()).isEqualTo(threadCount);
        assertThat(result.point()).isEqualTo(0L);
    }
}
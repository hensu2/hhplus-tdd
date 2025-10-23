package io.hhplus.tdd.point;


import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService pointService;

    /*
        순서
        1. 조회
        2. 충전
        3. 사용
        4. 충전/이용 내역
     */

    @Test
    @DisplayName("유저의 포인트를 조회한다")
    void getUserPoint() {
        // given
        // 파라미터 설정
        long userId = 1L;
        // 리턴 데이터 설정
        UserPoint expectedUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(expectedUserPoint);
        // when(userPointTable.selectById(userId)) 실행 시
        // .thenReturn(expectedUserPoint);  expectedUserPoint 리턴

        // when 실행
        UserPoint result = pointService.getUserPoint(userId);
        // pointService.getUserPoint 함수가 없어서 실패

        // then
        assertThat(result).isNotNull(); // null이 아닌지
        assertThat(result.id()).isEqualTo(userId); // 같은지
        assertThat(result.point()).isEqualTo(1000L); // 같은지
    }

    @Test
    @DisplayName("포인트 조회 후 없을 시 포인트가 0 인 UserPoint객체 리턴")
    void getUserPoint_NotUser() {
        /*
            테스트 사유
           UserPointTable selectById 리턴값이  map.getOrDefault(id, UserPoint.empty(id))
           이라서 테스트
         */

        // given
        // 파라미터 설정
        long userId = 999L;

        when(userPointTable.selectById(userId)).thenReturn(UserPoint.empty(userId));
        // when(userPointTable.selectById(userId)) 실행 시
        // thenReturn(UserPoint.empty(userId));  UserPoint.empty(userId) 리턴

        // when 실행
        UserPoint result = pointService.getUserPoint(userId);
        // pointService.getUserPoint 함수가 없어서 실패

        // then
        assertThat(result).isNotNull(); // null이 아닌지
        assertThat(result.id()).isEqualTo(userId); // 같은지
        assertThat(result.point()).isEqualTo(0); // 같은지
    }

    @Test
    @DisplayName("포인트를 충전한다")
    void chargePoint() {
        // given
        long userId = 1L;
        long chargeAmount = 500L;
        UserPoint currentPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        UserPoint updatedPoint = new UserPoint(userId, 1500L, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(currentPoint);
        when(userPointTable.insertOrUpdate(eq(userId), eq(1500L))).thenReturn(updatedPoint);

        // when
        UserPoint result = pointService.chargePoint(userId, chargeAmount);

        // then
        assertThat(result.point()).isEqualTo(1500L);
        verify(pointHistoryTable).insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), eq(result.updateMillis()));
        // insert 호출 되었는지 매개변수 eq는 매개변수랑 같은지 검증
    }

    @Test
    @DisplayName("포인트 충전 시 포인트가 0면 에러 발생")
    void chargePoint_InvalidAmount() {
        // given
        long userId = 1L;
        long chargeAmount = 0;   // 충전금액

        // when & then
        assertThatThrownBy(() -> pointService.chargePoint(userId, chargeAmount)) //
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("충전 금액이 음수면 예외가 발생한다")
    void chargePoint_NegativeAmount() {
        // given
        long userId = 1L;
        long chargeAmount = -100;   // 충전금액

        // when & then
        assertThatThrownBy(() -> pointService.chargePoint(userId, chargeAmount)) //
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("포인트를 사용한다")
    void usePoint() {
        // given
        long userId = 1L;
        long useAmount = 100;

        UserPoint currentPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        UserPoint updatedPoint = new UserPoint(userId, 500L, System.currentTimeMillis());

        //when
        UserPoint result = pointService.usePoint(userId, useAmount);

        // then
        assertThat(result.point()).isEqualTo(500L);
        verify(pointHistoryTable).insert(eq(userId), eq(useAmount), eq(TransactionType.CHARGE), eq(result.updateMillis()));

    }

}

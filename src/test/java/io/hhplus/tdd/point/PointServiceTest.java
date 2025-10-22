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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

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
}

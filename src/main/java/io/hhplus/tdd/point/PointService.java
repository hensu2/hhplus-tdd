package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

@Service
public class PointService {
    private final UserPointTable userPointTable;

    public PointService(UserPointTable userPointTable) {
        this.userPointTable = userPointTable;
    }

    public UserPoint getUserPoint(long userId) {
        return userPointTable.selectById(userId);
    }

    public UserPoint chargePoint(long userId, long chargeAmount) {
        if (chargeAmount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        UserPoint userpoint = userPointTable.selectById(userId);
        return userPointTable.insertOrUpdate(userId,userpoint.point()+chargeAmount);
    }
}

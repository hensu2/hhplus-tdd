package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

@Service
public class PointService {
    private final UserPointTable userPointTable;
    private  final PointHistoryTable pointHistoryTable;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public UserPoint getUserPoint(long userId) {
        return userPointTable.selectById(userId);
    }

    public UserPoint chargePoint(long userId, long chargeAmount) {
        checkAmount(chargeAmount,"충전 금액은 0보다 커야 합니다.");
        UserPoint userPoint = userPointTable.selectById(userId);
        long updatePoint =  userPoint.point() + chargeAmount;
        UserPoint updateUserData = userPointTable.insertOrUpdate(userId,updatePoint);
        pointHistoryTable.insert(userId,chargeAmount,TransactionType.CHARGE,updateUserData.updateMillis());
        return updateUserData;
    }

    public UserPoint usePoint(long userId, long useAmount) {
        checkAmount(useAmount,"사용 금액은 0보다 커야 합니다.");

        UserPoint userPoint = userPointTable.selectById(userId);

        if(userPoint.point() < useAmount){
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
        long updatePoint = userPoint.point() - useAmount;
        UserPoint updateUserData = userPointTable.insertOrUpdate(userId,updatePoint);
        pointHistoryTable.insert(userId,useAmount,TransactionType.USE,updateUserData.updateMillis());
        return updateUserData;
    }

    private void checkAmount(long amount,String comment){
        if(amount < 1){
            throw new IllegalArgumentException(comment);
        }
    }
}

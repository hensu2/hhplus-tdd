package io.hhplus.tdd.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("포인트 통합 테스트")
class PointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("존재하지 않는 사용자의 포인트를 조회하면 0 포인트가 반환된다")
    void getPointForNonExistentUser() throws Exception {
        // given
        long nonExistentUserId = 99999L;

        // when & then
        mockMvc.perform(get("/point/{id}", nonExistentUserId))  // url 호출
                .andExpect(status().isOk()) // 정상작동일 때
                .andExpect(jsonPath("$.id").value(nonExistentUserId))   // id == nonExistentUserId
                .andExpect(jsonPath("$.point").value(0));   // point == 0
    }

    @Test
    @DisplayName("포인트 충전 후 조회하면 충전된 금액이 반영된다")
    void chargeAndGetPoint() throws Exception {
        // given
        long userId = 1L;
        long chargeAmount = 1000L;

        // when: 포인트 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)    // PatchMapping 으로 설정
                        .content(String.valueOf(chargeAmount))) // RequestBody 설정
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(greaterThanOrEqualTo((int) chargeAmount)));    // point == chargeAmount

        // then: 포인트 조회
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(greaterThanOrEqualTo((int) chargeAmount)));
    }

    @Test
    @DisplayName("포인트 충전 후 사용하면 잔액이 차감된다")
    void chargeAndUsePoint() throws Exception {
        // given
        long userId = 2L;
        long chargeAmount = 5000L;
        long useAmount = 3000L;
        long expectedBalance = chargeAmount - useAmount;

        // when: 포인트 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)    // PatchMapping 으로 설정
                        .content(String.valueOf(chargeAmount))) // RequestBody 설정
                .andExpect(status().isOk());
                //  point == 5000L
        // when: 포인트 사용
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)    // PatchMapping 으로 설정
                        .content(String.valueOf(useAmount)))    // RequestBody 설정
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(greaterThanOrEqualTo((int) expectedBalance)));
        //  point == 2000L

        // then: 포인트 조회
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(greaterThanOrEqualTo((int) expectedBalance)));
        //  point == 2000L
    }

    @Test
    @DisplayName("0 이하의 금액으로 충전을 시도하면 실패한다")
    void chargeWithNegativeAmount() throws Exception {
        // given
        long userId = 5L;
        long negativeAmount = -100L;

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(negativeAmount)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("금액을 0 으로 충전을 시도하면 실패한다")
    void chargeWithInvalidAmount() throws Exception {
        // given
        long userId = 5L;
        long negativeAmount = 0;

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(negativeAmount)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("0 이하의 금액으로 사용을 시도하면 실패한다")
    void useWithNegativeAmount() throws Exception {
        // given
        long userId = 6L;
        long chargeAmount = 1000L;
        long negativeAmount = -100L;

        // when: 포인트 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk());

        // then: 음수 금액 사용 시도 - 실패해야 함
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(negativeAmount)))
                .andExpect(status().is4xxClientError());
    }
}
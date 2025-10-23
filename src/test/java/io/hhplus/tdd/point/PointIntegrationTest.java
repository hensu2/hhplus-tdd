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

}
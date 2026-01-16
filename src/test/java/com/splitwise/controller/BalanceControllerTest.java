package com.splitwise.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.splitwise.dto.BalanceResponseDTO;
import com.splitwise.service.UserBalanceService;

@WebMvcTest(BalanceController.class)
class BalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserBalanceService userBalanceService;

    @Test
    void testGetUserBalance() throws Exception {
        BalanceResponseDTO response = BalanceResponseDTO.builder()
                .owedToUser(Collections.emptyList())
                .owedByUser(Collections.emptyList())
                .build();

        Mockito.when(userBalanceService.getUserBalance(1L)).thenReturn(response);

        mockMvc.perform(get("/api/balances/user/1"))
                .andExpect(status().isOk());
    }
}

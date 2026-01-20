package com.splitwise.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitwise.dto.CreateExpenseRequest;
import com.splitwise.entity.Group;
import com.splitwise.entity.User;
import com.splitwise.enums.SplitType;
import com.splitwise.repository.ExpenseRepository;
import com.splitwise.repository.ExpenseShareRepository;
import com.splitwise.repository.GroupRepository;
import com.splitwise.repository.UserBalanceRepository;
import com.splitwise.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
public class HistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseShareRepository expenseShareRepository;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    private User alice;
    private User bob;
    private Group group;

    @BeforeEach
    void setUp() {
        expenseShareRepository.deleteAll();
        expenseRepository.deleteAll();
        userBalanceRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();

        alice = userRepository.save(User.builder().name("Alice").email("alice@test.com").build());
        bob = userRepository.save(User.builder().name("Bob").email("bob@test.com").build());

        group = Group.builder().name("Trip").createdBy(alice).build();
        group.addMember(alice);
        group.addMember(bob);
        group = groupRepository.save(group);
    }

    @Test
    @DisplayName("Should retrieve expense history for a specific group")
    void testGroupExpenseHistory() throws Exception {
        // Create 2 expenses in the group
        createExpense("Taxi", "20.00", group.getId(), alice, List.of(alice.getId(), bob.getId()));
        createExpense("Hotel", "200.00", group.getId(), bob, List.of(alice.getId(), bob.getId()));

        mockMvc.perform(get("/api/expenses/group/" + group.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].description").value("Hotel"))
                .andExpect(jsonPath("$[1].description").value("Taxi"));
    }

    @Test
    @DisplayName("Should retrieve expense history for a specific user")
    void testUserExpenseHistory() throws Exception {
        // Alice pays 1 group expense
        createExpense("Taxi", "20.00", group.getId(), alice, List.of(alice.getId(), bob.getId()));
        // Bob pays 1 group expense (Alice is participant)
        createExpense("Hotel", "200.00", group.getId(), bob, List.of(alice.getId(), bob.getId()));
        // Alice pays 1 non-group expense (Private dinner)
        createExpense("Dinner", "50.00", null, alice, List.of(alice.getId(), bob.getId()));

        // Alice should see all 3 expenses
        mockMvc.perform(get("/api/expenses/user/" + alice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].description").value("Dinner"))
                .andExpect(jsonPath("$[1].description").value("Hotel"))
                .andExpect(jsonPath("$[2].description").value("Taxi"));
    }

    private void createExpense(String desc, String amount, Long groupId, User paidBy, List<Long> participants) throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setDescription(desc);
        request.setAmount(new BigDecimal(amount));
        request.setGroupId(groupId);
        request.setPaidById(paidBy.getId());
        request.setSplitType(SplitType.EQUAL);
        request.setParticipantIds(participants);

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}

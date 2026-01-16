package com.splitwise.strategy;

import com.splitwise.entity.Expense;
import com.splitwise.entity.ExpenseShare;
import com.splitwise.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Strategy implementation for splitting expenses equally among participants.
 * Handles remainder cents by distributing them to the first participants.
 *
 * <p>Example: $100 split among 3 people = $33.34, $33.33, $33.33</p>
 */
@Component
@Slf4j
public class EqualSplitStrategy implements SplitStrategy {

    private static final int SCALE = 4;
    private static final int DISPLAY_SCALE = 2;

    @Override
    public List<ExpenseShare> split(
            Expense expense,
            List<User> participants,
            Map<Long, BigDecimal> exactAmounts
    ) {
        validate(expense, participants, exactAmounts);

        List<ExpenseShare> shares = new ArrayList<>();
        BigDecimal totalAmount = expense.getAmount();
        int participantCount = participants.size();
        
        log.debug("Splitting expense amount {} equally among {} participants", totalAmount, participantCount);

        // Calculate base share (rounded down to 2 decimal places)
        BigDecimal baseShare = totalAmount
                .divide(BigDecimal.valueOf(participantCount), SCALE, RoundingMode.DOWN)
                .setScale(DISPLAY_SCALE, RoundingMode.DOWN);

        // Calculate remainder to distribute
        BigDecimal totalDistributed = baseShare.multiply(BigDecimal.valueOf(participantCount));
        BigDecimal remainder = totalAmount.subtract(totalDistributed);
        
        log.debug("Base share calculated: {}. Total distributed so far: {}. Remainder: {}", 
                baseShare, totalDistributed, remainder);

        // Convert remainder to cents for distribution
        BigDecimal oneCent = new BigDecimal("0.01");
        int remainderCents = remainder.divide(oneCent, 0, RoundingMode.HALF_UP).intValue();
        
        if (remainderCents > 0) {
            log.info("Distributing {} remainder cents to first {} participants", remainderCents, remainderCents);
        }

        for (int i = 0; i < participantCount; i++) {
            User participant = participants.get(i);
            BigDecimal shareAmount = baseShare;

            // Distribute remainder cents to first participants
            if (i < remainderCents) {
                shareAmount = shareAmount.add(oneCent);
                log.trace("Added 1 cent to participant {}", participant.getId());
            }

            ExpenseShare share = ExpenseShare.builder()
                    .expense(expense)
                    .user(participant)
                    .amount(shareAmount)
                    .settled(false)
                    .build();

            shares.add(share);
        }

        return shares;
    }
}

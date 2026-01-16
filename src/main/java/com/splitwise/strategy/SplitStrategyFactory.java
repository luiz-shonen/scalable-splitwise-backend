package com.splitwise.strategy;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.splitwise.enums.SplitType;

/**
 * Factory class for obtaining the appropriate SplitStrategy based on SplitType.
 * This allows for easy extensibility when adding new split types.
 */
@Component
public class SplitStrategyFactory {

    private final Map<SplitType, SplitStrategy> strategies;

    /**
     * Constructor that initializes the strategy map with available implementations.
     *
     * @param equalSplitStrategy  the equal split strategy implementation
     * @param exactAmountStrategy the exact amount strategy implementation
     */
    public SplitStrategyFactory(
            EqualSplitStrategy equalSplitStrategy,
            ExactAmountStrategy exactAmountStrategy,
            PercentageSplitStrategy percentageSplitStrategy
    ) {
        this.strategies = new EnumMap<>(SplitType.class);
        this.strategies.put(SplitType.EQUAL, equalSplitStrategy);
        this.strategies.put(SplitType.EXACT, exactAmountStrategy);
        this.strategies.put(SplitType.PERCENTAGE, percentageSplitStrategy);
    }

    /**
     * Gets the appropriate strategy for the given split type.
     *
     * @param splitType the type of split to perform
     * @return the corresponding SplitStrategy implementation
     * @throws UnsupportedOperationException if no strategy exists for the given type
     */
    public SplitStrategy getStrategy(SplitType splitType) {
        SplitStrategy strategy = strategies.get(splitType);

        if (strategy == null) {
            throw new UnsupportedOperationException(
                    "Split type not supported: " + splitType
            );
        }

        return strategy;
    }

    /**
     * Checks if a strategy exists for the given split type.
     *
     * @param splitType the type to check
     * @return true if a strategy exists, false otherwise
     */
    public boolean hasStrategy(SplitType splitType) {
        return strategies.containsKey(splitType);
    }
}

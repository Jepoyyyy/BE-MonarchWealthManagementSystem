package com.indivaragroup.jdt17wms.constants;

import static com.indivaragroup.jdt17wms.constants.GoalConstants.CUSTOM_GOAL;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProductConstants {
    private ProductConstants() {
      // Prevent Instantiation
    }

    public static final int SUMMARY_COUNT = 5;

    public static final String MONEY_MARKET = "money_market";
    public static final String DEPOSIT = "deposit";
    public static final String BOND = "bond";
    public static final String SUKUK = "sukuk";
    public static final String MUTUAL_FUND = "mutual_fund";
    public static final String BALANCED_FUND = "balanced_fund";
    public static final String STOCK = "stock";

    public static final Set<String> LIQUID_PRODUCT_TYPES = Set.of(MONEY_MARKET, DEPOSIT);
    public static final List<String> ALL_PRODUCT_TYPES = List.of(
            MONEY_MARKET, DEPOSIT, BOND, SUKUK, MUTUAL_FUND, BALANCED_FUND, STOCK
    );
    public static final Map<String, List<String>> GOAL_PRODUCT_TYPES = Map.of(
            "emergency_fund", List.of(MONEY_MARKET, DEPOSIT, BALANCED_FUND),
            "vacation", List.of(MONEY_MARKET, DEPOSIT, BALANCED_FUND),
            "vehicle_purchase", List.of(MONEY_MARKET, DEPOSIT, BALANCED_FUND, BOND, SUKUK),
            "property", List.of(BALANCED_FUND, BOND, SUKUK, STOCK),
            "retirement", List.of(STOCK, BOND, SUKUK, BALANCED_FUND, MONEY_MARKET, DEPOSIT),
            CUSTOM_GOAL, List.of(STOCK, MONEY_MARKET, BALANCED_FUND, BOND, SUKUK, DEPOSIT)
    );
    public static final Map<String, String> TYPE_LABELS = Map.of(
            MONEY_MARKET, "Money Market",
            DEPOSIT, "Deposit",
            BALANCED_FUND, "Balanced Fund",
            MUTUAL_FUND, "Mutual Fund",
            BOND, "Bond",
            SUKUK, "Sukuk",
            STOCK, "Stock"
    );
    public static final BigDecimal SURPLUS_THRESHOLD = BigDecimal.valueOf(100000);
}

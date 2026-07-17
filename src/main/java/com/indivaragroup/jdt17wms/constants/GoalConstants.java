package com.indivaragroup.jdt17wms.constants;

import java.util.Map;

public final class GoalConstants {
    private GoalConstants() {
      // Prevent Instantiation
    }

    public static final String CUSTOM_GOAL = "custom";

    public static final Map<String, Integer> GOAL_MAX_MONTHS = Map.of(
            "savings", 18,
            "vacation", 12,
            "vehicle", 48,
            "property", 120,
            "retirement", 420,
            CUSTOM_GOAL, 60
    );
}

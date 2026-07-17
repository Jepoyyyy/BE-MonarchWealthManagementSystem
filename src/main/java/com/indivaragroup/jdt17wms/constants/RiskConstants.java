package com.indivaragroup.jdt17wms.constants;

import java.util.Map;

public final class RiskConstants {
    private RiskConstants() {
      // Prevent Instantiation
    }

  public static final String RISK_AVERSE = "risk_averse";
  public static final String MODERATE = "moderate";
  public static final String RISK_TAKER = "risk_taker";


  public static final int DEFAULT_QUESTIONNAIRE_SIZE = 5;
    public static final int RISK_AVERSE_THRESHOLD = 3;
    public static final int RISK_MODERATE_THRESHOLD = 7;
    public static final int RISK_SCALING_FACTOR = 10;
    public static final int MIN_ANSWER_SCORE = 0;
    public static final int MAX_ANSWER_SCORE = 2;

    public static final Map<String, Integer> MAX_RISK_LEVELS = Map.of(
            RISK_AVERSE, 2,
            MODERATE, 4,
            RISK_TAKER, 5
    );
    public static final Map<String, Double> RISK_TARGETS = Map.of(
            RISK_AVERSE, 1.5,
            MODERATE, 2.5,
            RISK_TAKER, 4.0
    );
}

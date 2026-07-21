package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.constants.RiskConstants;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.utils.SecurityUtils;
import com.indivaragroup.jdt17wms.aspects.RiskProfileAssessmentRequired;
import com.indivaragroup.jdt17wms.dto.response.*;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Asset;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.enums.UserRole;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import com.indivaragroup.jdt17wms.repositories.AuditLogRepository;
import com.indivaragroup.jdt17wms.repositories.ProductPriceRepository;
import com.indivaragroup.jdt17wms.services.PnLCalculationService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final ProductRepository productRepository;
    private final AuditLogRepository auditLogRepository;
    private final ProductPriceRepository productPriceRepository;
    private final PnLCalculationService pnLCalculationService;

    public DashboardService(UserRepository userRepository,
                            AssetRepository assetRepository,
                            ProductRepository productRepository,
                            AuditLogRepository auditLogRepository,
                            ProductPriceRepository productPriceRepository,
                            PnLCalculationService pnLCalculationService) {
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
        this.productRepository = productRepository;
        this.auditLogRepository = auditLogRepository;
        this.productPriceRepository = productPriceRepository;
        this.pnLCalculationService = pnLCalculationService;
    }

    public AdminDashboardDTO getAdminDashboard() {
        Map<String, Long> riskMap = userRepository.countByRiskProfile().stream()
                .collect(Collectors.toMap(
                        UserRepository.RiskProfileCount::getRiskProfile,
                        UserRepository.RiskProfileCount::getCount));

        RiskProfilesDTO riskProfiles = RiskProfilesDTO.builder()
                .riskAverse(riskMap.getOrDefault(RiskConstants.RISK_AVERSE, 0L).intValue())
                .moderate(riskMap.getOrDefault(RiskConstants.MODERATE, 0L).intValue())
                .riskTaker(riskMap.getOrDefault(RiskConstants.RISK_TAKER, 0L).intValue())
                .build();

        return AdminDashboardDTO.builder()
                .aum(assetRepository.sumTotalAmount())
                .userCount(userRepository.count())
                .activeUserCount(userRepository.countByStatusAndRole("active", UserRole.USER.name().toLowerCase()))
                .productCount(productRepository.count())
                .activeProductCount(productRepository.countByVisible(true))
                .totalAuditEvents(auditLogRepository.count())
                .riskProfiles(riskProfiles)
                .aumTrend(createAumTrend())
                .build();
    }

  public List<AumTrendDTO> createAumTrend() {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    int maxMonth = today.getMonthValue(); // e.g., July = 7
    int currentYear = today.getYear();

    // Dynamically query assets purchased since Jan 1st of the current year
    Instant startOfYear = LocalDate.of(currentYear, Month.JANUARY, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
    List<Asset> assets = assetRepository.findAllByPurchaseDateGreaterThanEqual(startOfYear);
    LocalDate lastSnapshotDate = LocalDate.of(currentYear, maxMonth, 1)
      .with(TemporalAdjusters.lastDayOfMonth());

    Set<UUID> productIds = assets.stream()
      .map(Asset::getProductId)
      .collect(Collectors.toSet());

    Map<UUID, TreeMap<LocalDate, BigDecimal>> pricesByProduct = new HashMap<>();
    if (!productIds.isEmpty()) {
      for (com.indivaragroup.jdt17wms.models.ProductPrice pp :
        productPriceRepository.findAllByProductIdInAndRecordedDateLessThanEqual(productIds, lastSnapshotDate)) {
        pricesByProduct
          .computeIfAbsent(pp.getProductId(), k -> new TreeMap<>())
          .put(pp.getRecordedDate(), pp.getPrice());
      }
    }
    List<AumTrendDTO> trend = new ArrayList<>();

    for (int m = 1; m <= maxMonth; m++) {
      LocalDate snapshotDate = LocalDate.of(currentYear, m, 1).with(TemporalAdjusters.lastDayOfMonth());
      Instant snapshotInstant = snapshotDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);

      BigDecimal monthlyAum = BigDecimal.ZERO;

      for (Asset asset : assets) {
        if (asset.getPurchaseDate().isAfter(snapshotInstant)) {
          continue;
        }

        TreeMap<LocalDate, BigDecimal> history = pricesByProduct.get(asset.getProductId());
        Map.Entry<LocalDate, BigDecimal> priceEntry = history != null ? history.floorEntry(snapshotDate) : null;
        BigDecimal price = priceEntry != null ? priceEntry.getValue() : BigDecimal.ZERO;

        monthlyAum = monthlyAum.add(asset.getUnits().multiply(price));
      }

      trend.add(AumTrendDTO.builder()
        .month(m)
        .value(monthlyAum)
        .build());
    }

    return trend;
  }

  @RiskProfileAssessmentRequired
  public UserDashboardDTO getUserDashboard() {
    User user = userRepository.findById(SecurityUtils.getCurrentUserId())
      .orElseThrow(() -> new CoreThrowHandler(ApiError.USER_NOT_FOUND));

    List<Asset> assetList = assetRepository.findAllByUserId(user.getId());
    BigDecimal totalValue = BigDecimal.ZERO;
    BigDecimal totalInvested = BigDecimal.ZERO;
    List<PortfolioItemDTO> portfolioItemDTOList = new ArrayList<>();

    for (Asset asset : assetList) {
      // Use PnLCalculationService for accurate remaining units calculation
      AssetsPnLResponseDTO pnl = pnLCalculationService.computePnLForAsset(asset);
      
      BigDecimal assetValue = pnl.getCurrentValue(); // remaining units × current price
      totalValue = totalValue.add(assetValue);
      totalInvested = totalInvested.add(asset.getAmount());

      PortfolioItemDTO portfolioItemDTO = PortfolioItemDTO.builder()
        .name(pnl.getProductName())
        .value(assetValue)
        .build();
      portfolioItemDTOList.add(portfolioItemDTO);
    }

    PortfolioDTO portfolioDTO = PortfolioDTO.builder()
      .value(totalValue.toString())
      .invested(totalInvested.toString())
      .holdings(portfolioItemDTOList.size())
      .items(portfolioItemDTOList)
      .build();

    List<PerformanceDTO> performance = createUserPerformanceTrend(user.getId());

    return UserDashboardDTO.builder()
      .portofolio(portfolioDTO)
      .performance(performance)
      .build();
  }

  private List<PerformanceDTO> createUserPerformanceTrend(java.util.UUID userId) {
    List<Asset> assets = assetRepository.findAllByUserId(userId);

    LocalDate today0 = LocalDate.now(ZoneOffset.UTC);
    LocalDate lastSnapshotDate = LocalDate.of(today0.getYear(), today0.getMonthValue(), 1)
      .with(TemporalAdjusters.lastDayOfMonth());

    Set<UUID> productIds = assets.stream()
      .map(Asset::getProductId)
      .collect(Collectors.toSet());

    Map<UUID, TreeMap<LocalDate, BigDecimal>> pricesByProduct = new HashMap<>();
    if (!productIds.isEmpty()) {
      for (com.indivaragroup.jdt17wms.models.ProductPrice pp :
        productPriceRepository.findAllByProductIdInAndRecordedDateLessThanEqual(productIds, lastSnapshotDate)) {
        pricesByProduct
          .computeIfAbsent(pp.getProductId(), k -> new TreeMap<>())
          .put(pp.getRecordedDate(), pp.getPrice());
      }
    }

    List<PerformanceDTO> trend = new ArrayList<>();
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    int maxMonth = today.getMonthValue();
    int currentYear = today.getYear();

    for (int m = 1; m <= maxMonth; m++) {
      LocalDate snapshotDate = LocalDate.of(currentYear, m, 1).with(TemporalAdjusters.lastDayOfMonth());
      Instant snapshotInstant = snapshotDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
      BigDecimal monthlyValue = BigDecimal.ZERO;

      for (Asset asset : assets) {
        if (asset.getPurchaseDate().isAfter(snapshotInstant)) {
          continue;
        }

        TreeMap<LocalDate, BigDecimal> history = pricesByProduct.get(asset.getProductId());
        Map.Entry<LocalDate, BigDecimal> priceEntry = history != null ? history.floorEntry(snapshotDate) : null;
        BigDecimal price = priceEntry != null ? priceEntry.getValue() : BigDecimal.ZERO;

        monthlyValue = monthlyValue.add(asset.getUnits().multiply(price));
      }

      trend.add(PerformanceDTO.builder()
        .month(m)
        .value(monthlyValue)
        .build());
    }

    return trend;
  }
}

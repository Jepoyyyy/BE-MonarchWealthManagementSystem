package com.indivaragroup.jdt17wms.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trx_expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "financial_profile_id", nullable = false)
    private FinancialProfile financialProfile;

    @Column(name = "housing", precision = 18, scale = 4,  nullable = false)
    private BigDecimal housing;

    @Column(name = "food", precision = 18, scale = 4,   nullable = false)
    private BigDecimal food;

    @Column(name = "transport", precision = 18, scale = 4,   nullable = false)
    private BigDecimal transport;

    @Column(name = "utilities", precision = 18, scale = 4,   nullable = false)
    private BigDecimal utilities;

    @Column(name = "healthcare", precision = 18, scale = 4,   nullable = false)
    private BigDecimal healthcare;

    @Column(name = "entertainment", precision = 18, scale = 4,   nullable = false)
    private BigDecimal entertainment;

    @Column(name = "insurance", precision = 18, scale = 4,   nullable = false)
    private BigDecimal insurance;

    @Column(name = "other", precision = 18, scale = 4,   nullable = false)
    private BigDecimal other;

    @Column(name = "total_expenses", precision = 18, scale = 4,   nullable = false)
    private BigDecimal totalExpenses;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

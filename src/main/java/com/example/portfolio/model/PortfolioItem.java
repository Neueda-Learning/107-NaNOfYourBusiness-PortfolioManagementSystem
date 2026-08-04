package com.example.portfolio.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioItem {
	private Long id;

	@NotNull(message = "type is required")
	private AssetType type;

	@NotBlank(message = "symbolOrName is required")
	@Size(max = 100, message = "symbolOrName must be at most 100 characters")
	private String symbolOrName;

	@NotNull(message = "quantity is required")
	@DecimalMin(value = "0.0001", inclusive = true, message = "quantity must be greater than 0")
	private BigDecimal quantity;

	@NotNull(message = "purchasePrice is required")
	@DecimalMin(value = "0.0001", inclusive = true, message = "purchasePrice must be greater than 0")
	private BigDecimal purchasePrice;

	@NotNull(message = "purchaseDate is required")
	@PastOrPresent(message = "purchaseDate must not be in the future")
	private LocalDate purchaseDate;

	@DecimalMin(value = "0.0001", inclusive = true, message = "currentPrice must be greater than 0 when provided")
	private BigDecimal currentPrice;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}


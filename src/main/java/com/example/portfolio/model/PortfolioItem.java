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

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public AssetType getType() {
		return type;
	}

	public void setType(AssetType type) {
		this.type = type;
	}

	public String getSymbolOrName() {
		return symbolOrName;
	}

	public void setSymbolOrName(String symbolOrName) {
		this.symbolOrName = symbolOrName;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getPurchasePrice() {
		return purchasePrice;
	}

	public void setPurchasePrice(BigDecimal purchasePrice) {
		this.purchasePrice = purchasePrice;
	}

	public LocalDate getPurchaseDate() {
		return purchaseDate;
	}

	public void setPurchaseDate(LocalDate purchaseDate) {
		this.purchaseDate = purchaseDate;
	}

	public BigDecimal getCurrentPrice() {
		return currentPrice;
	}

	public void setCurrentPrice(BigDecimal currentPrice) {
		this.currentPrice = currentPrice;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}


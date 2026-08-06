package com.example.portfolio.service;

import com.example.portfolio.client.MFAPIClient;
import com.example.portfolio.config.MutualFundCatalogue;
import com.example.portfolio.dto.BuyMutualFundRequest;
import com.example.portfolio.dto.SellMutualFundRequest;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.PortfolioItem;
import com.example.portfolio.repository.PortfolioItemRepository;
import com.example.portfolio.repository.PortfolioTradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MutualFundServiceTest {

    @Mock
    private PortfolioItemRepository portfolioItemRepository;

    @Mock
    private PortfolioTradeRepository portfolioTradeRepository;

    @Mock
    private MFAPIClient mfapiClient;

    @Mock
    private PortfolioTradeRepository portfolioTradeRepository;

    @Mock
    private WalletService walletService;

    private MutualFundCatalogue mutualFundCatalogue;

    private MutualFundService service;

    @BeforeEach
    void setUp() {
        mutualFundCatalogue = new MutualFundCatalogue();
        service = new MutualFundService(portfolioItemRepository, portfolioTradeRepository, mfapiClient, mutualFundCatalogue);
    }

    @Test
    void buyMutualFund_withValidRequest_calculatesUnitsCorrectly() {
        Integer schemeCode = 119551; // HDFC Flexi Cap Fund
        BigDecimal amount = new BigDecimal("10000.00");
        BigDecimal nav = new BigDecimal("650.25");

        BuyMutualFundRequest request = new BuyMutualFundRequest();
        request.setSchemeCode(schemeCode);
        request.setAmount(amount);

        Map<String, Object> mfapiResponse = Map.of(
                "meta", Map.of("scheme_name", "HDFC Flexi Cap Fund"),
                "data", java.util.List.of(Map.of("nav", "650.25", "date", "2026-08-05"))
        );

        when(mfapiClient.getMutualFundDetails(schemeCode)).thenReturn(mfapiResponse);
        when(mfapiClient.extractLatestNav(mfapiResponse)).thenReturn(nav);
        when(portfolioItemRepository.save(any(PortfolioItem.class))).thenAnswer(invocation -> {
            PortfolioItem item = invocation.getArgument(0);
            item.setId(1L);
            return item;
        });

        Map<String, Object> response = service.buyMutualFund(request);

        assertThat(response).containsKeys("message", "units", "portfolioItemId");
        assertThat(response.get("message")).isEqualTo("Mutual fund purchased successfully");
        // units = 10000 / 650.25 = 15.3787 (with HALF_UP and 4 decimals)
        assertThat(response.get("units")).isEqualTo(new BigDecimal("15.3787"));
        verify(portfolioItemRepository).save(any(PortfolioItem.class));
        verify(walletService).debitForBuy(
                new BigDecimal("10000.00"),
                AssetType.MUTUAL_FUND,
                1L,
                "HDFC Flexi Cap Fund");
    }

    @Test
    void buyMutualFund_withDefaultPurchaseDate_usesTodayDate() {
        Integer schemeCode = 119551;
        BigDecimal amount = new BigDecimal("10000.00");
        BigDecimal nav = new BigDecimal("650.25");

        BuyMutualFundRequest request = new BuyMutualFundRequest();
        request.setSchemeCode(schemeCode);
        request.setAmount(amount);
        // purchaseDate is null, should default to today

        Map<String, Object> mfapiResponse = Map.of(
                "meta", Map.of("scheme_name", "HDFC Flexi Cap Fund"),
                "data", java.util.List.of(Map.of("nav", "650.25"))
        );

        when(mfapiClient.getMutualFundDetails(schemeCode)).thenReturn(mfapiResponse);
        when(mfapiClient.extractLatestNav(mfapiResponse)).thenReturn(nav);
        when(portfolioItemRepository.save(any(PortfolioItem.class))).thenAnswer(invocation -> {
            PortfolioItem item = invocation.getArgument(0);
            item.setId(1L);
            return item;
        });

        service.buyMutualFund(request);

        verify(portfolioItemRepository).save(any(PortfolioItem.class));
        verify(walletService).debitForBuy(any(BigDecimal.class), any(AssetType.class), any(Long.class), any(String.class));
    }

    @Test
    void buyMutualFund_withUnsupportedSchemeCode_throwsResourceNotFoundException() {
        BuyMutualFundRequest request = new BuyMutualFundRequest();
        request.setSchemeCode(999999); // Invalid scheme code
        request.setAmount(new BigDecimal("10000.00"));

        assertThatThrownBy(() -> service.buyMutualFund(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Mutual fund is not supported");
    }

    @Test
    void sellMutualFund_withValidRequest_calculatesUnitsAndUpdatesHolding() {
        Long portfolioItemId = 1L;
        BigDecimal sellAmount = new BigDecimal("5000.00");
        BigDecimal currentNav = new BigDecimal("650.25");
        BigDecimal existingUnits = new BigDecimal("20.0000");

        PortfolioItem holding = new PortfolioItem();
        holding.setId(portfolioItemId);
        holding.setType(AssetType.MUTUAL_FUND);
        holding.setSymbolOrName("HDFC Flexi Cap Fund");
        holding.setQuantity(existingUnits);
        holding.setPurchasePrice(new BigDecimal("600.00"));
        holding.setCurrentPrice(new BigDecimal("650.00"));

        SellMutualFundRequest request = new SellMutualFundRequest();
        request.setPortfolioItemId(portfolioItemId);
        request.setAmount(sellAmount);

        Map<String, Object> mfapiResponse = Map.of(
                "meta", Map.of("scheme_name", "HDFC Flexi Cap Fund"),
                "data", java.util.List.of(Map.of("nav", "650.25"))
        );

        when(portfolioItemRepository.findById(portfolioItemId)).thenReturn(Optional.of(holding));
        when(mfapiClient.getMutualFundDetails(119551)).thenReturn(mfapiResponse);
        when(mfapiClient.extractLatestNav(mfapiResponse)).thenReturn(currentNav);
        when(portfolioItemRepository.update(any(PortfolioItem.class))).thenReturn(holding);

        Map<String, Object> response = service.sellMutualFund(request);

        assertThat(response).containsKeys("message", "unitsSold", "remainingUnits");
        assertThat(response.get("message")).isEqualTo("Mutual fund units sold successfully");
        // unitsToSell = 5000 / 650.25 = 7.6899
        // remainingUnits = 20.0000 - 7.6899 = 12.3101
        verify(portfolioItemRepository).update(any(PortfolioItem.class));
        verify(walletService).creditForSell(
                new BigDecimal("5000.00"),
                AssetType.MUTUAL_FUND,
                portfolioItemId,
                "HDFC Flexi Cap Fund");
    }

    @Test
    void sellMutualFund_whenAllUnitsSold_deletesHolding() {
        Long portfolioItemId = 1L;
        BigDecimal sellAmount = new BigDecimal("13005.00"); // 20 * 650.25 = 13005 to sell all units
        BigDecimal currentNav = new BigDecimal("650.25");
        BigDecimal existingUnits = new BigDecimal("20.0000");

        PortfolioItem holding = new PortfolioItem();
        holding.setId(portfolioItemId);
        holding.setType(AssetType.MUTUAL_FUND);
        holding.setSymbolOrName("HDFC Flexi Cap Fund");
        holding.setQuantity(existingUnits);

        SellMutualFundRequest request = new SellMutualFundRequest();
        request.setPortfolioItemId(portfolioItemId);
        request.setAmount(sellAmount);

        Map<String, Object> mfapiResponse = Map.of(
                "meta", Map.of("scheme_name", "HDFC Flexi Cap Fund"),
                "data", java.util.List.of(Map.of("nav", "650.25"))
        );

        when(portfolioItemRepository.findById(portfolioItemId)).thenReturn(Optional.of(holding));
        when(mfapiClient.getMutualFundDetails(119551)).thenReturn(mfapiResponse);
        when(mfapiClient.extractLatestNav(mfapiResponse)).thenReturn(currentNav);
        doNothing().when(portfolioItemRepository).deleteById(portfolioItemId);

        Map<String, Object> response = service.sellMutualFund(request);

        assertThat(response.get("message")).isEqualTo("Mutual fund holding closed");
        verify(portfolioItemRepository).deleteById(portfolioItemId);
        verify(walletService).creditForSell(
                new BigDecimal("13005.00"),
                AssetType.MUTUAL_FUND,
                portfolioItemId,
                "HDFC Flexi Cap Fund");
    }

    @Test
    void sellMutualFund_whenInsufficientUnits_throwsIllegalArgumentException() {
        Long portfolioItemId = 1L;
        BigDecimal sellAmount = new BigDecimal("15000.00");
        BigDecimal currentNav = new BigDecimal("650.25");
        BigDecimal existingUnits = new BigDecimal("10.0000"); // Only 10 units available

        PortfolioItem holding = new PortfolioItem();
        holding.setId(portfolioItemId);
        holding.setType(AssetType.MUTUAL_FUND);
        holding.setSymbolOrName("HDFC Flexi Cap Fund");
        holding.setQuantity(existingUnits);

        SellMutualFundRequest request = new SellMutualFundRequest();
        request.setPortfolioItemId(portfolioItemId);
        request.setAmount(sellAmount);

        Map<String, Object> mfapiResponse = Map.of(
                "meta", Map.of("scheme_name", "HDFC Flexi Cap Fund"),
                "data", java.util.List.of(Map.of("nav", "650.25"))
        );

        when(portfolioItemRepository.findById(portfolioItemId)).thenReturn(Optional.of(holding));
        when(mfapiClient.getMutualFundDetails(119551)).thenReturn(mfapiResponse);
        when(mfapiClient.extractLatestNav(mfapiResponse)).thenReturn(currentNav);

        assertThatThrownBy(() -> service.sellMutualFund(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not enough units available to sell");
    }

    @Test
    void sellMutualFund_withNonMutualFundHolding_throwsIllegalArgumentException() {
        Long portfolioItemId = 1L;

        PortfolioItem holding = new PortfolioItem();
        holding.setId(portfolioItemId);
        holding.setType(AssetType.STOCK); // Not a mutual fund
        holding.setSymbolOrName("TCS.NS");
        holding.setQuantity(new BigDecimal("10.0000"));

        SellMutualFundRequest request = new SellMutualFundRequest();
        request.setPortfolioItemId(portfolioItemId);
        request.setAmount(new BigDecimal("5000.00"));

        when(portfolioItemRepository.findById(portfolioItemId)).thenReturn(Optional.of(holding));

        assertThatThrownBy(() -> service.sellMutualFund(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not a mutual fund");
    }

    @Test
    void sellMutualFund_withNonExistentHolding_throwsResourceNotFoundException() {
        Long portfolioItemId = 999L;

        SellMutualFundRequest request = new SellMutualFundRequest();
        request.setPortfolioItemId(portfolioItemId);
        request.setAmount(new BigDecimal("5000.00"));

        when(portfolioItemRepository.findById(portfolioItemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sellMutualFund(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Portfolio item not found");
    }
}




package com.example.portfolio.service;

import com.example.portfolio.dto.PortfolioItemRequest;
import com.example.portfolio.dto.PortfolioItemResponse;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.mapper.PortfolioItemMapper;
import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.PortfolioItem;
import com.example.portfolio.model.TradeSide;
import com.example.portfolio.repository.PortfolioItemRepository;
import com.example.portfolio.repository.PortfolioTradeRepository;
import com.example.portfolio.service.portfolio.PortfolioItemTypeHandlerRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PortfolioItemService {

    private final PortfolioItemRepository repository;
    private final PortfolioTradeRepository tradeRepository;
    private final PortfolioItemMapper mapper;
    private final PortfolioItemTypeHandlerRegistry handlerRegistry;
    private final WalletService walletService;

    public PortfolioItemService(PortfolioItemRepository repository,
                                PortfolioTradeRepository tradeRepository,
                                PortfolioItemMapper mapper,
                                 PortfolioItemTypeHandlerRegistry handlerRegistry,
                                 WalletService walletService) {
        this.repository = repository;
        this.tradeRepository = tradeRepository;
        this.mapper = mapper;
        this.handlerRegistry = handlerRegistry;
        this.walletService = walletService;
    }

    public List<PortfolioItemResponse> findAll(AssetType type) {
        List<PortfolioItem> items = (type != null)
                ? repository.findByType(type)
                : repository.findAll();
        return items.stream().map(mapper::toResponse).toList();
    }

    public PortfolioItemResponse findById(Long id) {
        return mapper.toResponse(requireItem(id));
    }

    public PortfolioItemResponse create(PortfolioItemRequest request) {
        // For non-STOCK asset types the caller must supply purchasePrice explicitly,
        // since there is no market-data auto-fill for those types.
        if (request.getType() != AssetType.STOCK && request.getPurchasePrice() == null) {
            throw new IllegalArgumentException("purchasePrice is required for " + request.getType() + " holdings");
        }
        PortfolioItem item = mapper.toModel(request);
        handlerRegistry.resolve(item.getType()).applyCreateDefaults(item);
        return mapper.toResponse(repository.save(item));
    }

    public PortfolioItemResponse update(Long id, PortfolioItemRequest request) {
        requireItem(id); // ensure it exists before update
        PortfolioItem item = mapper.toModel(request);
        item.setId(id);
        return mapper.toResponse(repository.update(item));
    }

    public void delete(Long id) {
        requireItem(id);
        repository.deleteById(id);
    }

    public PortfolioItemResponse refreshPrice(Long id) {
        PortfolioItem item = requireItem(id);
        BigDecimal newPrice = handlerRegistry.resolve(item.getType()).resolveRefreshedPrice(item);
        repository.updateCurrentPrice(id, newPrice);
        item.setCurrentPrice(newPrice);
        return mapper.toResponse(item);
    }

    @Transactional
    public PortfolioItemResponse buy(Long id, BigDecimal quantity) {
        PortfolioItem item = requireStockHolding(id);
        BigDecimal executionPrice = resolveExecutionPrice(item);
        LocalDateTime executedAt = LocalDateTime.now();

        BigDecimal oldQuantity = item.getQuantity();
        BigDecimal newQuantity = oldQuantity.add(quantity);
        BigDecimal oldCost = oldQuantity.multiply(item.getPurchasePrice());
        BigDecimal newCost = quantity.multiply(executionPrice);
        BigDecimal averagePrice = oldCost.add(newCost)
                .divide(newQuantity, 4, RoundingMode.HALF_UP);
        BigDecimal purchaseAmount = quantity.multiply(executionPrice).setScale(4, RoundingMode.HALF_UP);

        walletService.debitForBuy(purchaseAmount, item.getType(), item.getId(), item.getSymbolOrName());

        repository.updateHoldingAfterTrade(item.getId(), newQuantity, averagePrice, executionPrice, executedAt);
        tradeRepository.saveTrade(item, TradeSide.BUY, quantity, executionPrice, executedAt);

        item.setQuantity(newQuantity);
        item.setPurchasePrice(averagePrice);
        item.setCurrentPrice(executionPrice);
        item.setUpdatedAt(executedAt);
        return mapper.toResponse(item);
    }

    @Transactional
    public PortfolioItemResponse sell(Long id, BigDecimal quantity) {
        PortfolioItem item = requireStockHolding(id);

        if (quantity.compareTo(item.getQuantity()) > 0) {
            throw new IllegalArgumentException("Sell quantity exceeds current holding");
        }

        BigDecimal executionPrice = resolveExecutionPrice(item);
        LocalDateTime executedAt = LocalDateTime.now();
        BigDecimal remainingQuantity = item.getQuantity().subtract(quantity);
        BigDecimal saleProceeds = quantity.multiply(executionPrice).setScale(4, RoundingMode.HALF_UP);
        tradeRepository.saveTrade(item, TradeSide.SELL, quantity, executionPrice, executedAt);
        walletService.creditForSell(saleProceeds, item.getType(), item.getId(), item.getSymbolOrName());

        if (remainingQuantity.compareTo(BigDecimal.ZERO) == 0) {
            repository.deleteById(item.getId());
            item.setQuantity(BigDecimal.ZERO);
            item.setCurrentPrice(executionPrice);
            item.setUpdatedAt(executedAt);
            return mapper.toResponse(item);
        }

        repository.updateHoldingAfterTrade(item.getId(), remainingQuantity, item.getPurchasePrice(), executionPrice, executedAt);
        item.setQuantity(remainingQuantity);
        item.setCurrentPrice(executionPrice);
        item.setUpdatedAt(executedAt);
        return mapper.toResponse(item);
    }

    private PortfolioItem requireStockHolding(Long id) {
        PortfolioItem item = requireItem(id);
        if (item.getType() != AssetType.STOCK) {
            throw new IllegalArgumentException("Buy/sell is currently supported for STOCK holdings only");
        }
        return item;
    }

    private BigDecimal resolveExecutionPrice(PortfolioItem item) {
        return handlerRegistry.resolve(item.getType()).resolveRefreshedPrice(item);
    }

    private PortfolioItem requireItem(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Portfolio item not found with id: " + id));
    }
}

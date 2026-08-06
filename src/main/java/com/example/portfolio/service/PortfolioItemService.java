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
import java.util.Optional;

@Service
public class PortfolioItemService {

    private final PortfolioItemRepository repository;
    private final PortfolioTradeRepository tradeRepository;
    private final PortfolioItemMapper mapper;
    private final PortfolioItemTypeHandlerRegistry handlerRegistry;

    public PortfolioItemService(PortfolioItemRepository repository,
                                PortfolioTradeRepository tradeRepository,
                                PortfolioItemMapper mapper,
                                PortfolioItemTypeHandlerRegistry handlerRegistry) {
        this.repository = repository;
        this.tradeRepository = tradeRepository;
        this.mapper = mapper;
        this.handlerRegistry = handlerRegistry;
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

    @Transactional
    public PortfolioItemResponse create(PortfolioItemRequest request) {
        // For non-STOCK asset types the caller must supply purchasePrice explicitly,
        // since there is no market-data auto-fill for those types.
        if (request.getType() != AssetType.STOCK && request.getPurchasePrice() == null) {
            throw new IllegalArgumentException("purchasePrice is required for " + request.getType() + " holdings");
        }
        PortfolioItem item = mapper.toModel(request);
        handlerRegistry.resolve(item.getType()).applyCreateDefaults(item);

        // For STOCK holdings, buying more of a symbol that is already held should
        // update the existing holding (weighted-average purchase price) instead
        // of creating a duplicate row in "My Holdings".
        if (item.getType() == AssetType.STOCK) {
            Optional<PortfolioItem> existing = repository.findByTypeAndSymbolOrName(
                    AssetType.STOCK, item.getSymbolOrName());
            if (existing.isPresent()) {
                return mergeIntoExistingStockHolding(existing.get(), item);
            }
        }

        return mapper.toResponse(repository.save(item));
    }

    /**
     * Merges an additional stock purchase into an already-existing holding:
     * quantities are summed and the purchase price becomes the weighted
     * average of the old and new costs (weighted by units bought).
     */
    private PortfolioItemResponse mergeIntoExistingStockHolding(PortfolioItem existing, PortfolioItem incoming) {
        BigDecimal oldQuantity = existing.getQuantity();
        BigDecimal addedQuantity = incoming.getQuantity();
        BigDecimal newQuantity = oldQuantity.add(addedQuantity);

        BigDecimal oldCost = oldQuantity.multiply(existing.getPurchasePrice());
        BigDecimal newCost = addedQuantity.multiply(incoming.getPurchasePrice());
        BigDecimal weightedAveragePrice = oldCost.add(newCost)
                .divide(newQuantity, 4, RoundingMode.HALF_UP);

        BigDecimal executionPrice = incoming.getCurrentPrice() != null
                ? incoming.getCurrentPrice()
                : incoming.getPurchasePrice();
        LocalDateTime executedAt = LocalDateTime.now();

        repository.updateHoldingAfterTrade(existing.getId(), newQuantity, weightedAveragePrice, executionPrice, executedAt);
        tradeRepository.saveTrade(existing, TradeSide.BUY, addedQuantity, executionPrice, executedAt);

        existing.setQuantity(newQuantity);
        existing.setPurchasePrice(weightedAveragePrice);
        existing.setCurrentPrice(executionPrice);
        existing.setUpdatedAt(executedAt);
        return mapper.toResponse(existing);
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
        tradeRepository.saveTrade(item, TradeSide.SELL, quantity, executionPrice, executedAt);

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

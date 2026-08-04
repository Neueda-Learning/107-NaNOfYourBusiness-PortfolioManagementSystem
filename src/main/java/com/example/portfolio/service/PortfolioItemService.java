package com.example.portfolio.service;

import com.example.portfolio.dto.PortfolioItemRequest;
import com.example.portfolio.dto.PortfolioItemResponse;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.mapper.PortfolioItemMapper;
import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.PortfolioItem;
import com.example.portfolio.repository.PortfolioItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class PortfolioItemService {

    private final PortfolioItemRepository repository;
    private final PortfolioItemMapper mapper;
    private final MarketDataService marketDataService;

    public PortfolioItemService(PortfolioItemRepository repository,
                                PortfolioItemMapper mapper,
                                MarketDataService marketDataService) {
        this.repository = repository;
        this.mapper = mapper;
        this.marketDataService = marketDataService;
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
        PortfolioItem item = mapper.toModel(request);

        // Auto-fetch price for stocks when caller didn't provide one
        if (item.getType() == AssetType.STOCK && item.getCurrentPrice() == null) {
            Optional<BigDecimal> price = marketDataService.fetchPrice(item.getSymbolOrName());
            price.ifPresent(item::setCurrentPrice);
        }

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
        BigDecimal newPrice = marketDataService.fetchPriceOrThrow(item.getSymbolOrName());
        repository.updateCurrentPrice(id, newPrice);
        item.setCurrentPrice(newPrice);
        return mapper.toResponse(item);
    }

    private PortfolioItem requireItem(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Portfolio item not found with id: " + id));
    }
}

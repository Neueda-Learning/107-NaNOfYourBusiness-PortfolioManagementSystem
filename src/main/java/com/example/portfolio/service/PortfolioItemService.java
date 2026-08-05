package com.example.portfolio.service;

import com.example.portfolio.dto.PortfolioItemRequest;
import com.example.portfolio.dto.PortfolioItemResponse;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.mapper.PortfolioItemMapper;
import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.PortfolioItem;
import com.example.portfolio.repository.PortfolioItemRepository;
import com.example.portfolio.service.portfolio.PortfolioItemTypeHandlerRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PortfolioItemService {

    private final PortfolioItemRepository repository;
    private final PortfolioItemMapper mapper;
    private final PortfolioItemTypeHandlerRegistry handlerRegistry;

    public PortfolioItemService(PortfolioItemRepository repository,
                                PortfolioItemMapper mapper,
                                PortfolioItemTypeHandlerRegistry handlerRegistry) {
        this.repository = repository;
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

    public PortfolioItemResponse create(PortfolioItemRequest request) {
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

    private PortfolioItem requireItem(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Portfolio item not found with id: " + id));
    }
}

package com.example.portfolio.service.portfolio;

import com.example.portfolio.model.AssetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Central resolver to keep PortfolioItemService stable while asset-specific
 * behavior is split into dedicated handler classes.
 */
@Component
public class PortfolioItemTypeHandlerRegistry {

    private static final Logger log = LoggerFactory.getLogger(PortfolioItemTypeHandlerRegistry.class);

    private final Map<AssetType, PortfolioItemTypeHandler> handlersByType;

    public PortfolioItemTypeHandlerRegistry(List<PortfolioItemTypeHandler> handlers) {
        EnumMap<AssetType, PortfolioItemTypeHandler> map = new EnumMap<>(AssetType.class);
        for (PortfolioItemTypeHandler handler : handlers) {
            AssetType type = handler.supportedType();
            if (map.put(type, handler) != null) {
                throw new IllegalStateException("Duplicate handler configured for asset type: " + type);
            }
        }

        List<AssetType> missing = Arrays.stream(AssetType.values())
                .filter(type -> !map.containsKey(type))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing portfolio item handlers for asset types: " + missing);
        }

        this.handlersByType = Map.copyOf(map);
        log.info("Initialized portfolio item type handler registry for asset types: {}", handlersByType.keySet());
    }

    public PortfolioItemTypeHandler resolve(AssetType type) {
        if (type == null) {
            throw new IllegalArgumentException("Asset type must not be null");
        }

        PortfolioItemTypeHandler handler = handlersByType.get(type);
        if (handler == null) {
            log.error("No handler found for asset type: {}", type);
            throw new IllegalStateException("No handler found for asset type: " + type);
        }
        return handler;
    }
}


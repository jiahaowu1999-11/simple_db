package com.db.base.core;

import java.util.HashMap;
import java.util.Map;

/**
 * 实体元数据缓存，避免重复解析注解
 */
public class EntityMetadataCache {
    private final Map<Class<?>, EntityMetadata> metadataMap = new HashMap<>();
    
    public EntityMetadata getMetadata(Class<?> entityClass) {
        EntityMetadata metadata = metadataMap.get(entityClass);
        if (metadata == null) {
            metadata = new EntityMetadata(entityClass);
            metadataMap.put(entityClass, metadata);
        }
        return metadata;
    }
}
    
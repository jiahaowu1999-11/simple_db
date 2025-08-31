package com.db.base.core;



import com.db.base.annotation.Column;
import com.db.base.annotation.Id;
import com.db.base.annotation.Table;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 实体类的元数据，存储实体与表的映射关系
 */
public class EntityMetadata {
    private Class<?> entityClass;
    private String tableName;
    private String idColumn;
    private String idField;
    private Map<String, String> columnToFieldMap = new HashMap<>();
    private Map<String, String> fieldToColumnMap = new HashMap<>();

    public EntityMetadata(Class<?> entityClass) {
        this.entityClass = entityClass;
        
        // 解析表名
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation != null) {
            this.tableName = tableAnnotation.value();
        } else {
            this.tableName = entityClass.getSimpleName().toLowerCase();
        }
        
        // 解析字段和列的映射关系
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            String columnName;
            
            // 检查是否有Column注解
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation != null) {
                columnName = columnAnnotation.value();
            } else {
                // 默认使用字段名作为列名（可以实现驼峰转下划线等规则）
                columnName = fieldName;
            }
            
            // 检查是否为主键
            if (field.isAnnotationPresent(Id.class)) {
                this.idColumn = columnName;
                this.idField = fieldName;
            }
            
            columnToFieldMap.put(columnName, fieldName);
            fieldToColumnMap.put(fieldName, columnName);
        }
    }

    // getter方法
    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getTableName() {
        return tableName;
    }

    public String getIdColumn() {
        return idColumn;
    }

    public String getIdField() {
        return idField;
    }

    public Map<String, String> getColumnToFieldMap() {
        return columnToFieldMap;
    }

    public Map<String, String> getFieldToColumnMap() {
        return fieldToColumnMap;
    }
}
    
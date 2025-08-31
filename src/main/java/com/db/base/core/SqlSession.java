package com.db.base.core;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 数据库会话，负责执行SQL操作
 */
public class SqlSession {
    private final DataSource dataSource;
    private final EntityMetadataCache metadataCache;

    public SqlSession(DataSource dataSource) {
        this.dataSource = dataSource;
        this.metadataCache = new EntityMetadataCache();
    }

    public <T> List<T> selectBySql(Class<T> entityClass, String sql, Object... params) {
        EntityMetadata metadata = metadataCache.getMetadata(entityClass);
        assert sql != null;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (params != null && params.length > 0) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return resultSetToEntityList(rs, entityClass, metadata);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 根据ID查询实体
     */
    public <T> T selectById(Class<T> entityClass, Object id) {
        EntityMetadata metadata = metadataCache.getMetadata(entityClass);
        String sql = "SELECT * FROM " + metadata.getTableName() +
                " WHERE " + metadata.getIdColumn() + " = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return resultSetToEntity(rs, entityClass, metadata);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 查询所有实体
     */
    public <T> List<T> selectAll(Class<T> entityClass) {
        EntityMetadata metadata = metadataCache.getMetadata(entityClass);
        String sql = "SELECT * FROM " + metadata.getTableName();

        List<T> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(resultSetToEntity(rs, entityClass, metadata));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 插入实体
     */
    public <T> void insert(T entity) {
        Class<?> entityClass = entity.getClass();
        EntityMetadata metadata = metadataCache.getMetadata(entityClass);

        // 构建INSERT SQL
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(metadata.getTableName()).append(" (");

        StringBuilder values = new StringBuilder(" VALUES (");
        List<Object> params = new ArrayList<>();

        // 处理所有字段（除了自增主键）
        for (String fieldName : metadata.getFieldToColumnMap().keySet()) {
            // 如果是自增主键且值为null，则不包含在INSERT语句中
            if (fieldName.equals(metadata.getIdField())) {
                try {
                    Field field = entityClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    if (value == null) {
                        value = UUID.randomUUID().toString().replace("-", "");
                        field.set(entity, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            sql.append(metadata.getFieldToColumnMap().get(fieldName)).append(", ");
            values.append("?, ");

            try {
                Field field = entityClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                params.add(field.get(entity));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 移除最后一个逗号和空格
        sql.setLength(sql.length() - 2);
        values.setLength(values.length() - 2);

        sql.append(")").append(values).append(")");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString(),
                     Statement.RETURN_GENERATED_KEYS)) {

            // 设置参数
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ps.executeUpdate();

            // 如果是自增主键，获取生成的ID并设置回实体
            if (metadata.getIdField() != null) {
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    Object id = generatedKeys.getObject(1);
                    Field idField = entityClass.getDeclaredField(metadata.getIdField());
                    idField.setAccessible(true);
                    idField.set(entity, id);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新实体
     */
    public <T> void update(T entity) {
        Class<?> entityClass = entity.getClass();
        EntityMetadata metadata = metadataCache.getMetadata(entityClass);

        // 构建UPDATE SQL
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(metadata.getTableName()).append(" SET ");

        List<Object> params = new ArrayList<>();
        Object idValue = null;

        // 处理所有字段
        for (String fieldName : metadata.getFieldToColumnMap().keySet()) {
            // 获取字段值
            try {
                Field field = entityClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(entity);

                // 记录主键值
                if (fieldName.equals(metadata.getIdField())) {
                    idValue = value;
                    continue; // 主键不参与UPDATE的SET部分
                }

                sql.append(metadata.getFieldToColumnMap().get(fieldName)).append(" = ?, ");
                params.add(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 移除最后一个逗号和空格
        sql.setLength(sql.length() - 2);

        // 添加WHERE条件
        sql.append(" WHERE ").append(metadata.getIdColumn()).append(" = ?");
        params.add(idValue);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            // 设置参数
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据ID删除实体
     */
    public <T> void deleteById(Class<T> entityClass, Object id) {
        EntityMetadata metadata = metadataCache.getMetadata(entityClass);
        String sql = "DELETE FROM " + metadata.getTableName() +
                " WHERE " + metadata.getIdColumn() + " = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private <T> List<T> resultSetToEntityList(ResultSet rs, Class<T> entityClass, EntityMetadata metadata) throws SQLException, InstantiationException, IllegalAccessException, NoSuchFieldException {
        // 5. 获取结果集元数据（包含列信息）
        ResultSetMetaData dbmetaData = rs.getMetaData();
        int columnCount = dbmetaData.getColumnCount(); // 列数量
        List<T> tList = new ArrayList<>();
        // 6. 遍历结果集，转换为Map
        while (rs.next()) {
            T entity = entityClass.newInstance();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = dbmetaData.getColumnName(i); // 获取列名
                Object columnValue = rs.getObject(i); // 获取列值
                String fieldName = metadata.getColumnToFieldMap().get(columnName);

                if (fieldName != null) {
                    Field field = entityClass.getDeclaredField(fieldName);
                    field.setAccessible(true);

                    if (columnValue != null) {
                        field.set(entity, columnValue);
                    }
                }
            }
            tList.add(entity);
        }

        return tList;

    }

    /**
     * 将ResultSet转换为实体对象
     */
    private <T> T resultSetToEntity(ResultSet rs, Class<T> entityClass, EntityMetadata metadata)
            throws Exception {
        T entity = entityClass.newInstance();
        ResultSetMetaData rsMetadata = rs.getMetaData();

        for (int i = 1; i <= rsMetadata.getColumnCount(); i++) {
            String columnName = rsMetadata.getColumnName(i);
            String fieldName = metadata.getColumnToFieldMap().get(columnName);

            if (fieldName != null) {
                Field field = entityClass.getDeclaredField(fieldName);
                field.setAccessible(true);

                // 简单处理类型转换
                Object value = rs.getObject(i);
                if (value != null) {
                    field.set(entity, value);
                }
            }
        }

        return entity;
    }
}
    
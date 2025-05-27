package com.mybatisflex.core.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

public class OptionalTypeHandler extends BaseTypeHandler<Optional<?>> {
    private static final ConcurrentHashMap<JdbcType, TypeHandler<?>> typeHandlerCache = new ConcurrentHashMap<>();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Optional<?> parameter, JdbcType jdbcType)
            throws SQLException {
        if (parameter.isPresent()) {
            TypeHandler<Object> handler = getTypeHandler(jdbcType, parameter.get().getClass());
            handler.setParameter(ps, i, parameter.get(), jdbcType);
        } else {
            ps.setNull(i, jdbcType.TYPE_CODE);
        }
    }

    @Override
    public Optional<?> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        // 通过ResultSetMetaData动态获取jdbcType
        JdbcType jdbcType = getJdbcTypeFromResultSet(rs, columnName);
        TypeHandler<Object> handler = getTypeHandler(jdbcType, null);
        Object value = handler.getResult(rs, columnName);
        return Optional.ofNullable(value);
    }

    @Override
    public Optional<?> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        // 通过ResultSetMetaData动态获取jdbcType
        JdbcType jdbcType = getJdbcTypeFromResultSet(rs, columnIndex);
        TypeHandler<Object> handler = getTypeHandler(jdbcType, null);
        Object value = handler.getResult(rs, columnIndex);
        return Optional.ofNullable(value);
    }

    @Override
    public Optional<?> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        // 对于CallableStatement，我们需要从参数元数据获取类型
        JdbcType jdbcType = getJdbcTypeFromCallableStatement(cs, columnIndex);
        TypeHandler<Object> handler = getTypeHandler(jdbcType, null);
        Object value = handler.getResult(cs, columnIndex);
        return Optional.ofNullable(value);
    }

    /**
     * 根据JdbcType动态获取对应的TypeHandler
     */
    @SuppressWarnings("unchecked")
    private TypeHandler<Object> getTypeHandler(JdbcType jdbcType, Class<?> javaType) {
        return (TypeHandler<Object>) typeHandlerCache.computeIfAbsent(jdbcType, key -> {
            // 如果没有TypeHandlerRegistry，使用默认的处理方式
            return getDefaultTypeHandler(key);
        });
    }

    /**
     * 从ResultSet获取JdbcType
     */
    private JdbcType getJdbcTypeFromResultSet(ResultSet rs, String columnName) throws SQLException {
        try {
            int columnIndex = rs.findColumn(columnName);
            return getJdbcTypeFromResultSet(rs, columnIndex);
        } catch (SQLException e) {
            return JdbcType.OTHER;
        }
    }

    /**
     * 从ResultSet获取JdbcType
     */
    private JdbcType getJdbcTypeFromResultSet(ResultSet rs, int columnIndex) throws SQLException {
        try {
            int sqlType = rs.getMetaData().getColumnType(columnIndex);
            return JdbcType.forCode(sqlType);
        } catch (SQLException e) {
            return JdbcType.OTHER;
        }
    }

    /**
     * 从CallableStatement获取JdbcType
     */
    private JdbcType getJdbcTypeFromCallableStatement(CallableStatement cs, int columnIndex) throws SQLException {
        try {
            int sqlType = cs.getParameterMetaData().getParameterType(columnIndex);
            return JdbcType.forCode(sqlType);
        } catch (SQLException e) {
            return JdbcType.OTHER;
        }
    }

    /**
     * 获取默认的TypeHandler
     */
    private TypeHandler<?> getDefaultTypeHandler(JdbcType jdbcType) {
        switch (jdbcType) {
            case VARCHAR:
            case CHAR:
            case LONGVARCHAR:
                return new org.apache.ibatis.type.StringTypeHandler();
            case INTEGER:
                return new org.apache.ibatis.type.IntegerTypeHandler();
            case BIGINT:
                return new org.apache.ibatis.type.LongTypeHandler();
            case DECIMAL:
            case NUMERIC:
                return new org.apache.ibatis.type.BigDecimalTypeHandler();
            case DOUBLE:
                return new org.apache.ibatis.type.DoubleTypeHandler();
            case FLOAT:
                return new org.apache.ibatis.type.FloatTypeHandler();
            case BOOLEAN:
            case BIT:
                return new org.apache.ibatis.type.BooleanTypeHandler();
            case DATE:
            case TIME:
            case TIMESTAMP:
                return new org.apache.ibatis.type.DateTypeHandler();
            default:
                return new org.apache.ibatis.type.ObjectTypeHandler();
        }
    }
}

package org.yuca.yuca.knowledge.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedJdbcTypes({JdbcType.OTHER})
@MappedTypes({Float[].class})
public class PGVectorTypeHandler extends BaseTypeHandler<Float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Float[] parameter, JdbcType jdbcType) throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("vector");
        pgObject.setValue(arrayToString(parameter));
        ps.setObject(i, pgObject);
    }

    @Override
    public Float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        if (value == null) {
            return null;
        }
        return stringToArray(value);
    }

    @Override
    public Float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        if (value == null) {
            return null;
        }
        return stringToArray(value);
    }

    @Override
    public Float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        if (value == null) {
            return null;
        }
        return stringToArray(value);
    }

    private String arrayToString(Float[] array) {
        if (array == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private Float[] stringToArray(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        // 移除首尾的方括号和空格
        value = value.trim();
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        String[] values = value.split(",");
        Float[] result = new Float[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = Float.parseFloat(values[i].trim());
        }
        return result;
    }
} 
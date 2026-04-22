package org.yuca.ai.history;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreSQL JSONB 类型处理器
 * 用于处理 String 到 JSONB 的类型转换
 */
@MappedTypes({String.class})
public class JsonbTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        PGobject jsonObject = new PGobject();
        jsonObject.setType("jsonb");
        jsonObject.setValue(parameter);
        ps.setObject(i, jsonObject);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        PGobject pGobject = (PGobject) rs.getObject(columnName);
        return pGobject != null ? pGobject.getValue() : null;
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        PGobject pGobject = (PGobject) rs.getObject(columnIndex);
        return pGobject != null ? pGobject.getValue() : null;
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        PGobject pGobject = (PGobject) cs.getObject(columnIndex);
        return pGobject != null ? pGobject.getValue() : null;
    }
}

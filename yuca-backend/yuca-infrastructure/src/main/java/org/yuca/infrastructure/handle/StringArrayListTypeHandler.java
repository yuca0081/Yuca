package org.yuca.infrastructure.handle;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.jdbc.PgConnection;
import org.postgresql.util.PGobject;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PostgreSQL VARCHAR[] ↔ Java List&lt;String&gt; 双向转换（#10 元数据过滤用）。
 *
 * <p>写入：用 {@link Connection#createArrayOf(String, Object)} 创建 {@code varchar[]}，
 * 绑定到 PreparedStatement。读取：把 ResultSet 的 Array 转 List。
 *
 * <p>另支持以字面量字符串 "{a,b,c}" 形式作为 SQL 参数传入——通过 {@link #toSqlLiteral}
 * 把 List 拼成 PG 数组字面量，便于在动态 SQL 中用 CAST(#{tags} AS varchar[])。
 *
 * @author Yuca
 * @since 2026-07-22
 */
@MappedJdbcTypes({JdbcType.OTHER})
@MappedTypes({List.class})
public class StringArrayListTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
            throws SQLException {
        // 优先用原生 Array 绑定（最稳）
        if (ps.getConnection().isWrapperFor(PgConnection.class)) {
            String[] arr = parameter.toArray(new String[0]);
            Array array = ps.getConnection().createArrayOf("varchar", arr);
            ps.setArray(i, array);
            return;
        }
        // 兜底：用 PGobject + 字面量（非 PG 连接场景）
        PGobject pgObject = new PGobject();
        pgObject.setType("varchar[]");
        pgObject.setValue(toSqlLiteral(parameter));
        ps.setObject(i, pgObject);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return arrayToList(rs.getArray(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return arrayToList(rs.getArray(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return arrayToList(cs.getArray(columnIndex));
    }

    private List<String> arrayToList(Array array) throws SQLException {
        if (array == null) {
            return null;
        }
        Object obj = array.getArray();
        if (obj instanceof Object[] arr) {
            List<String> result = new ArrayList<>(arr.length);
            for (Object o : arr) {
                result.add(o == null ? null : o.toString());
            }
            return result;
        }
        return null;
    }

    /**
     * 把 List 拼成 PG 数组字面量，形如 {tag1,tag2}（无外层引号，每个元素内部转义双引号）。
     * 主要用于 mapper.xml 里的 CAST(#{filter.tags} AS varchar[])，
     * 但实际写入优先走 setNonNullParameter 的 Array 绑定，本方法保留供调试 / 兜底使用。
     */
    public static String toSqlLiteral(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            String s = list.get(i) == null ? "" : list.get(i).replace("\"", "\\\"");
            sb.append('"').append(s).append('"');
        }
        sb.append('}');
        return sb.toString();
    }

    /** 字面量 → List（调试 / 单测用） */
    public static List<String> fromSqlLiteral(String literal) {
        if (literal == null || literal.isBlank()) {
            return new ArrayList<>();
        }
        String trimmed = literal.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (String s : trimmed.split(",")) {
            String t = s.trim();
            if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
                t = t.substring(1, t.length() - 1).replace("\\\"", "\"");
            }
            result.add(t);
        }
        return result;
    }
}

/**
 * $Id: SqlCondition.java,v 1.00 2015/02/27 19:23:29 dmorris Exp $
 */
package com.untangle.app.reports;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.untangle.uvm.util.Constants;
import com.untangle.uvm.util.SqlParseException;
import com.untangle.uvm.util.SqlUtil;

/**
 * A SQL condition (clause) for limiting results of a ReportEntry
 */
@SuppressWarnings("serial")
public class SqlCondition implements Serializable, JSONString
{
    private static final Logger logger = LogManager.getLogger(SqlCondition.class);

    private static final Set<String> IS_KEYWORDS = Set.of(
        Constants.NULL, Constants.NOT_NULL,
        Constants.TRUE.toLowerCase(), Constants.FALSE.toLowerCase(),
        Constants.UNKNOWN
    );

    private String column;
    private String value;
    private String operator;
    private String table;

    public SqlCondition() {}

    public SqlCondition( String column, String operator, String value )
    {
        this.column = column;
        this.operator = operator;
        this.value = value;
    }

    public SqlCondition( String column, String operator, String value, String table)
    {
        this.column = column;
        this.operator = operator;
        this.value = value;
        this.table = table;
    }

    public String getColumn() { return this.column; }
    public void setColumn( String newValue ) { this.column = newValue; }

    public String getValue() { return this.value; }
    public void setValue( String newValue ) { this.value = newValue; }

    public String getOperator() { return this.operator; }
    public void setOperator( String newValue )
    {
        String lowerValue = newValue.toLowerCase();

        switch ( lowerValue ) {
        case "=":
        case "!=":
        case "<>":
        case ">":
        case "<":
        case ">=":
        case "<=":
        case "between":
        case "like":
        case "not like":
        case Constants.IS:
        case Constants.IS_NOT:
        case Constants.IN:
        case Constants.NOT_IN:
            break;
        default:
            throw new RuntimeException("Unknown SQL condition operator: " + newValue);
        }
        this.operator = lowerValue;
    }

    /**
     * No-op: autoFormatValue is now computed server-side and cannot be
     * overridden by client input. This setter exists solely so Jabsorb
     * deserialization does not error when legacy JSON contains the field.
     */
    public void setAutoFormatValue( boolean newValue ) { }

    public String getTable() { return this.table; }
    public void setTable( String newValue ) { this.table = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Build the SQL fragment for this condition, using the condition's
     * own table field for column-reference detection.
     */
    public String toSqlString()
    {
        return toSqlString(getTable());
    }

    /**
     * Build the SQL fragment for this condition with safe handling per operator:
     * - IS/IS NOT: keyword allowlist (NULL, TRUE, FALSE, NOT NULL, UNKNOWN)
     * - IN/NOT IN: parse literal values and emit individual '?' placeholders
     * - All others: '?' parameterization, unless value is a valid column name
     *   (column-to-column comparison)
     */
    public String toSqlString( String table )
    {
        String op = getOperator();
        String val = getValue();

        if (Constants.IS.equals(op) || Constants.IS_NOT.equals(op)) {
            if (val == null || !IS_KEYWORDS.contains(val.toLowerCase())) {
                throw new SqlParseException("Invalid IS/IS NOT value: " + val);
            }
            return getColumn() + " " + op + " " + val + " ";
        }

        if (Constants.IN.equals(op) || Constants.NOT_IN.equals(op)) {
            List<SqlUtil.InLiteral> literals = SqlUtil.parseInValues(val);
            if (literals.isEmpty()) {
                throw new SqlParseException("Empty IN value list");
            }
            String placeholders = String.join(",", Collections.nCopies(literals.size(), "?"));
            return getColumn() + " " + op + " (" + placeholders + ") ";
        }

        if (val != null && table != null) {
            String colType = ReportsManagerImpl.getInstance().getColumnType(table, val);
            if (colType != null) {
                return getColumn() + " " + op + " " + val + " ";
            }
        }

        return getColumn() + " " + op + " ? ";
    }

    public String toString()
    {
        return "SqlCondition[ column=" + getColumn() + ", operator=" + getOperator() + " value=" + getValue() + " ]";
    }

    public static void setPreparedStatementValues( PreparedStatement statement, SqlCondition[] conditions, String table )
    {
        if ( conditions == null )
            return;

        try {
            int i = 0;
            for ( SqlCondition condition : conditions ) {
                String op = condition.getOperator();
                String val = condition.getValue();

                if (Constants.IS.equalsIgnoreCase(op) || Constants.IS_NOT.equalsIgnoreCase(op)) {
                    continue;
                }

                if (Constants.IN.equalsIgnoreCase(op) || Constants.NOT_IN.equalsIgnoreCase(op)) {
                    List<SqlUtil.InLiteral> literals = SqlUtil.parseInValues(val);
                    String columnType = ReportsManagerImpl.getInstance().getColumnType(table, condition.getColumn());
                    if (columnType == null) {
                        logger.warn("Ignoring unknown column " + condition.getColumn() + " in table " + table);
                        continue;
                    }
                    for (SqlUtil.InLiteral literal : literals) {
                        i++;
                        bindInLiteral(statement, i, literal, columnType);
                    }
                    continue;
                }

                if (val != null) {
                    String valColType = ReportsManagerImpl.getInstance().getColumnType(table, val);
                    if (valColType != null) {
                        continue;
                    }
                }

                i++;
                String columnType = ReportsManagerImpl.getInstance().getColumnType( table, condition.getColumn() );

                if ( val == null ) {
                    logger.warn("Invalid null value for column: " + condition.getColumn());
                    throw new RuntimeException( "Invalid value: null" );
                }
                if ( columnType == null ) {
                    logger.warn("Ignoring unknown column " + condition.getColumn() + " in table " + table );
                    continue;
                }

                columnType = normalizeColumnType(columnType);
                bindValue(statement, i, columnType, val);
            }
        } catch (Exception e ) {
            logger.warn( "Failed to set values in prepared statement.", e );
            throw new RuntimeException( "Failed to set values in prepared statement.", e );
        }
    }

    private static String normalizeColumnType( String columnType )
    {
        if (columnType != null && columnType.startsWith(Constants.CHAR_PREFIX)) {
            return Constants.TEXT;
        }
        return columnType;
    }

    private static void bindInLiteral( PreparedStatement statement, int index, SqlUtil.InLiteral literal, String columnType ) throws Exception
    {
        columnType = normalizeColumnType(columnType);

        switch (literal.type) {
        case NUMBER:
            switch (columnType) {
            case Constants.INTEGER: case Constants.INT:
            case Constants.INT2: case Constants.INT4:
            case Constants.INT8: case Constants.TINYINT:
            case Constants.SMALLINT: case Constants.MEDIUMINT:
            case Constants.BIGINT:
                statement.setLong(index, literal.numberValue.longValueExact());
                break;
            default:
                statement.setBigDecimal(index, literal.numberValue);
                break;
            }
            break;
        case STRING:
            statement.setString(index, literal.stringValue);
            break;
        }
    }

    private static void bindValue( PreparedStatement statement, int index, String columnType, String value ) throws Exception
    {
        if (Constants.NULL.equalsIgnoreCase(value)) {
            bindNull(statement, index, columnType);
            return;
        }

        switch (columnType) {
        case Constants.NUMERIC:
        case Constants.INTEGER: case Constants.INT:
        case Constants.INT2: case Constants.INT4:
        case Constants.INT8: case Constants.TINYINT:
        case Constants.SMALLINT: case Constants.MEDIUMINT:
        case Constants.BIGINT:
        case Constants.REAL: case Constants.FLOAT:
        case Constants.FLOAT4: case Constants.FLOAT8:
            try {
                statement.setLong(index, Long.valueOf( value ));
            } catch (Exception e) {
                try {
                    statement.setFloat(index, Float.valueOf( value ));
                } catch (Exception exc) {
                    logger.warn("Failed to parse long", e);
                    logger.warn("Failed to parse float", exc);
                    throw new RuntimeException( "Invalid number: " + value );
                }
            }
            break;

        case Constants.INET:
            statement.setObject(index, value, java.sql.Types.OTHER);
            break;

        case Constants.TIMESTAMP:
            statement.setObject(index, value, java.sql.Types.OTHER);
            break;

        case Constants.BOOL:
        case Constants.BOOLEAN:
            if ( value.toLowerCase().contains(Constants.TRUE.toLowerCase()) || value.toLowerCase().contains("1") )
                statement.setBoolean(index, true);
            else
                statement.setBoolean(index, false);
            break;

        case Constants.BPCHAR: case Constants.CHARACTER:
        case Constants.VARCHAR: case Constants.TEXT:
            statement.setString(index, value);
            break;

        default:
            logger.warn("Unknown column type: " + columnType);
            break;
        }
    }

    private static void bindNull( PreparedStatement statement, int index, String columnType ) throws Exception
    {
        switch (columnType) {
        case Constants.NUMERIC:
        case Constants.INTEGER: case Constants.INT:
        case Constants.INT2: case Constants.INT4:
        case Constants.INT8: case Constants.TINYINT:
        case Constants.SMALLINT: case Constants.MEDIUMINT:
        case Constants.BIGINT: case Constants.REAL:
        case Constants.FLOAT: case Constants.FLOAT4:
        case Constants.FLOAT8:
            statement.setNull(index, java.sql.Types.INTEGER);
            break;
        case Constants.INET:
            statement.setNull(index, java.sql.Types.OTHER);
            break;
        case Constants.TIMESTAMP:
            statement.setNull(index, java.sql.Types.TIMESTAMP);
            break;
        case Constants.BOOL:
        case Constants.BOOLEAN:
            statement.setNull(index, java.sql.Types.BOOLEAN);
            break;
        case Constants.BPCHAR: case Constants.CHARACTER:
        case Constants.VARCHAR: case Constants.TEXT:
            statement.setNull(index, java.sql.Types.VARCHAR);
            break;
        default:
            statement.setNull(index, java.sql.Types.OTHER);
            break;
        }
    }

    public static void setPreparedStatementValues( PreparedStatement statement, List<SqlCondition> conditions, String table )
    {
        if ( conditions == null )
            setPreparedStatementValues( statement, (SqlCondition[])null, table );
        else
            setPreparedStatementValues( statement, conditions.toArray( new SqlCondition[0] ), table );
        return;
    }

}

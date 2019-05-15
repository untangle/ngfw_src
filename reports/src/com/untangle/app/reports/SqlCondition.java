/**
 * $Id: SqlCondition.java,v 1.00 2015/02/27 19:23:29 dmorris Exp $
 */
package com.untangle.app.reports;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

/**
 * A SQL condition (clause) for limiting results of a ReportEntry
 */
@SuppressWarnings("serial")
public class SqlCondition implements Serializable, JSONString
{
    private static final Logger logger = Logger.getLogger(SqlCondition.class);

    private String column;
    private String value;
    private String operator;
    private boolean autoFormatValue = true;
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
        this.table = table;
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
        case "is":
        case "is not":
        case "in":
        case "not in":
            break;
        default:
            throw new RuntimeException("Unknown SQL condition operator: " + newValue);
        }
        this.operator = lowerValue;
    }

    /**
     * If true, then the "value" will be handled in the Sql Statement with a "?"
     * If false, the value will be hardcoded verbatim inside the sql string.
     * This is necessary because not all values/operators are correctly supported by Statement
     *
     * @return 
     * true if auto-format supported, false otherwise
     */
    public boolean getAutoFormatValue()
    {
        /**
         * Some operators require special handling
         */
        if ("is".equalsIgnoreCase( getOperator() )) {
            return false;
        }
        if ("is not".equalsIgnoreCase( getOperator() )) {
            return false;
        }
        if ("in".equalsIgnoreCase( getOperator() )) {
            return false;
        }
        if ("not in".equalsIgnoreCase( getOperator() )) {
            return false;
        }

        return this.autoFormatValue;
    }

    public void setAutoFormatValue( boolean newValue )
    {
        this.autoFormatValue = newValue;
    }

    public String getTable() { return this.table; }
    public void setTable( String newValue ) { this.table = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public String toSqlString()
    {
        // these operators are not supported with prepareStatement
        // as such there are hardcoded in the SQL query
        if ( !getAutoFormatValue() ) {
            return getColumn() + " " + getOperator() + " " + getValue() + " ";
        }
        // otherwise use the PreparedStatement '?'
        else {
            return getColumn() + " " + getOperator() + " ? ";
        }
    }

    public String toString()
    {
        return "SqlCondition[ " + getColumn() + " " + getOperator() + " " + getValue() + " ]";
    }

    public static void setPreparedStatementValues( PreparedStatement statement, SqlCondition[] conditions, String table )
    {
        if ( conditions == null )
            return;

        try {
            int i = 0;
            for ( SqlCondition condition : conditions ) {
                    
                // these operators are not supported with Statement
                if ( !condition.getAutoFormatValue() ) {
                    continue;
                }
                    
                i++;
                String columnType = ReportsManagerImpl.getInstance().getColumnType( table, condition.getColumn() );
                String value = condition.getValue();

                if ( value == null ) {
                    logger.warn("Ignoring bad condition: Invalid value: " + value );
                    throw new RuntimeException( "Invalid value: " + value );
                }
                if ( columnType == null ) {
                    logger.warn("Ignoring unknown column " + condition.getColumn() + " in table " + table );
                    continue;
                }

                // count all "char(x)" as "char"
                if (columnType.startsWith("char")) {
                    columnType = "text";
                }

                switch (columnType) {
                case "numeric":
                case "integer":
                case "int":
                case "int2":
                case "int4":
                case "int8":
                case "tinyint":
                case "smallint":
                case "mediumint":
                case "bigint":
                case "real":
                case "float":
                case "float4":
                case "float8":
                    if ("null".equalsIgnoreCase(value))
                        statement.setNull(i, java.sql.Types.INTEGER);
                    else {
                        try {
                            statement.setLong(i, Long.valueOf( value ));
                        } catch (Exception e) {
                            try {
                                statement.setFloat(i, Float.valueOf( value ));
                            } catch (Exception exc) {
                                logger.warn("Failed to parse long",e);
                                logger.warn("Failed to parse float",exc);
                                throw new RuntimeException( "Invalid number: " + value );
                            }
                        }
                    }
                break;
                
                case "inet":
                    if ("null".equalsIgnoreCase(value))
                        statement.setNull(i, java.sql.Types.OTHER);
                    else 
                        statement.setObject(i, value, java.sql.Types.OTHER);
                    break;

                case "timestamp":
                    if ("null".equalsIgnoreCase(value))
                        statement.setNull(i, java.sql.Types.TIMESTAMP);
                    else 
                        statement.setObject(i, value, java.sql.Types.OTHER);
                    break;
                    
                case "bool":
                case "boolean":
                    if ("null".equalsIgnoreCase(value))
                        statement.setNull(i, java.sql.Types.BOOLEAN);
                    else if ( value.toLowerCase().contains("true") || value.toLowerCase().contains("1") )
                        statement.setBoolean(i, true);
                    else
                        statement.setBoolean(i, false);
                    break;

                case "bpchar":
                case "character":
                case "varchar":
                case "text":
                    if ("null".equalsIgnoreCase(value))
                        statement.setNull(i, java.sql.Types.VARCHAR);
                    else
                        statement.setString(i, condition.getValue());
                break;
                default:
                    logger.warn("Unknown column type: " + columnType);
                    continue;
                }
            }
        } catch (Exception e ) {
            logger.warn( "Failed to set values in prepared statement.", e );
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

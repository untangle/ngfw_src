/**
 * $Id: SqlCondition.java,v 1.00 2015/02/27 19:23:29 dmorris Exp $
 */
package com.untangle.uvm.node;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

/**
 * A SQL condition (clause) for limiting results of a ReportEntry
 */
@SuppressWarnings("serial")
public class SqlCondition implements Serializable, JSONString
{
    private String column;
    private String value;
    private String operator;

    public SqlCondition() {}
    
    public SqlCondition( String column, String operator, String value )
    {
        this.column = column;
        this.operator = operator;
        this.value = value;
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
        case "in":
        case "is":
            break;
        default:
            throw new RuntimeException("Unknown SQL condition operator: " + newValue);
        }
        this.operator = lowerValue;
    }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
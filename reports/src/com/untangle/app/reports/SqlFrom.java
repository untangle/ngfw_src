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
import org.apache.velocity.util.StringUtils;

/**
 * A SQL from target to retrieve data.
 */
@SuppressWarnings("serial")
public class SqlFrom implements Serializable, JSONString
{
    private static final Logger logger = Logger.getLogger(SqlCondition.class);

    private static final String TABLE_TEMPLATE = " %table% WHERE %where% ";
    private static final String RANGE_TEMPLATE = " ( SELECT %rangeCase% as %column% from %table% WHERE %where% ) t ";
    private static final String BITMASK_TEMPLATE = " generate_series(1,%length%) i cross join lateral (select case when (%column%::bit(%length%) & power(2,i-1)::int::bit(%length%) )::int = power(2,i-1) is true then power(2,i-1) else null end as %column% from %table% where %where% ) t where %column% is not null";

    public static enum FromType {
        TABLE,
        RANGE,
        BITMASK
    }

    private FromType type;
    private Integer[][] rangeValues;
    private Integer length;
    private String table;

    public SqlFrom() {
        this.type = FromType.TABLE;
    }
    
    public SqlFrom( FromType type, Integer[][] rangeValues)
    {
        this.type = type;
        this.rangeValues = rangeValues;
    }

    public SqlFrom( FromType type, Integer length)
    {
        this.type = type;
        this.length = length;
    }

    public FromType getType() { return this.type; }
    public void setType( FromType newValue ) { this.type = newValue; }

    public Integer[][] getRangeValues() { return this.rangeValues; }
    public void setRangeValues( Integer[][] newValue ) { this.rangeValues = newValue; }

    public Integer getLength() { return this.length; }
    public void setLength( Integer newValue ) { this.length = newValue; }

    public String getTable() { return this.table; }
    public void setTable( String newValue ) { this.table = newValue; }

    public String getFrom(String column, String where)
    {
        String template = "";
        switch(type){
            case RANGE:
                StringBuilder rangeBuilder = new StringBuilder(1024);
                rangeBuilder.append("CASE");
                for(int i = 0; i < rangeValues.length; i++){
                    rangeBuilder.append(" WHEN " + column + " BETWEEN " + Integer.toString(rangeValues[i][0]) + " AND " + Integer.toString(rangeValues[i][1]) + " THEN " + Integer.toString(rangeValues[i][0]));
                }
                rangeBuilder.append(" ELSE '0' END");
                template = RANGE_TEMPLATE.replaceAll("%rangeCase%", rangeBuilder.toString());
                break;

            case BITMASK:
                template = BITMASK_TEMPLATE.replaceAll("%length%", Integer.toString(length));
                // may need to modify where to add column > 0
// generate_series(1,16) i cross join lateral (select (<COLUMN>::bit(16) & power(2,i-1)::int::bit(16) )::int = power(2,i-1) as match, power(2,i-1) as <COLUMN> from <TABLE> where <COLUMN> > 0) t 
                break;

            default:
                template = TABLE_TEMPLATE;
        }
        return template.replaceAll("%table%", getTable()).replaceAll("%column%", column).replaceAll("%where%", where);
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}

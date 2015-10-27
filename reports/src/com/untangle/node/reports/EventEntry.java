/**
 * $Id$
 */
package com.untangle.node.reports;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

/**
 * This class represents a unique Event Log query and stores all the information the UI needs to
 * render and exec the query
 */
@SuppressWarnings("serial")
public class EventEntry implements Serializable, JSONString
{
    private final Logger logger = Logger.getLogger(getClass());

    private String uniqueId = null;
    private int displayOrder = 9999; /* The order to display this report entry (relative to others) */

    private String title;
    private String table;
    private String category;
    private SqlCondition[] conditions;
    private String[] defaultColumns;

    public EventEntry() {}
    
    public EventEntry( String title, String table, SqlCondition[] conditions )
    {
        this.title = title;
        this.table = table;
        this.conditions = conditions;
    }

    public String getUniqueId() { return uniqueId; }
    public void setUniqueId( String newValue ) { this.uniqueId = newValue; }

    public int getDisplayOrder() { return this.displayOrder; }
    public void setDisplayOrder( int newValue ) { this.displayOrder = newValue; }

    public String getName() { return this.title; } /* REMOVEME deprecated */
    
    public String getTitle() { return this.title; }
    public void setTitle( String newValue ) { this.title = newValue; }

    public String getCategory() { return this.category; }
    public void setCategory( String newValue ) { this.category = newValue; }
    
    public String getTable() { return this.table; }
    public void setTable( String newValue ) { this.table = newValue; }
    
    public SqlCondition[] getConditions() { return this.conditions; }
    public void setConditions( SqlCondition[] newValue ) { this.conditions = newValue; }

    public String[] getDefaultColumns() { return this.defaultColumns; }
    public void setDefaultColumns( String[] newValue ) { this.defaultColumns = newValue; }
    
    public String toSqlQuery( SqlCondition[] extraConditions )
    {
        String query = ""; 
        query +=  "SELECT * FROM reports." + this.table + " WHERE true";
        if ( getConditions() != null ) {
            for ( SqlCondition condition : getConditions() ) {
                if ( ! ReportsManagerImpl.getInstance().tableHasColumn( table, condition.getColumn() ) ) {
                    logger.warn("Ignoring unknown column " + condition.getColumn() + " in table " + getTable() );
                    continue;
                }

                query += " and " + condition.toSqlString() + " ";
            }
        }
        if ( extraConditions != null ) {
            for ( SqlCondition condition : extraConditions )  {
                if ( ! ReportsManagerImpl.getInstance().tableHasColumn( table, condition.getColumn() ) ) {
                    logger.warn("Ignoring unknown column " + condition.getColumn() + " in table " + getTable() );
                    continue;
                }

                query += " and " + condition.toSqlString() + " ";
            }
        }
        
        query += " ORDER BY time_stamp DESC";
        return query;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
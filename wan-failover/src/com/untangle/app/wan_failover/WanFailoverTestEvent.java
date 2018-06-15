/**
 * $Id$
 */

package com.untangle.app.wan_failover;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;
import java.io.Serializable;
import org.json.JSONString;

/**
 * Log event for a WAN Failover test.
 */
@SuppressWarnings("serial")
public class WanFailoverTestEvent extends LogEvent implements Serializable, JSONString
{
    private int interfaceId;
    private String name;
    private String osName;
    private String description;
    private Boolean success;
    
    public WanFailoverTestEvent() { }
    
    public WanFailoverTestEvent( int interfaceId, String name, String osName, String description, Boolean success )
    {
        super();
        this.interfaceId = interfaceId;
        this.name = name;
        this.osName = osName;
        this.description = description;
        this.success = success;
    }
    
    public int getInterfaceId() { return this.interfaceId; }
    public void setInterfaceId( int interfaceId ) { this.interfaceId = interfaceId; }

    public String getName() { return this.name; }
    public void setName( String newValue ) { this.name = newValue; }

    public String getOsName() { return this.osName; }
    public void setOsName( String newValue ) { this.osName = newValue; }

    public Boolean getSuccess() { return this.success; }
    public void setSuccess( Boolean newValue ) { this.success = newValue; }
    
    public String getDescription() { return this.description; }
    public void setDescription( String newValue ) { this.description = newValue; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "wan_failover_test_events" + getPartitionTablePostfix() + " " +
            "(time_stamp, interface_id, name, success, description) " + 
            "values " +
            "( ?, ?, ?, ?, ?)";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setInt(++i, getInterfaceId());
        pstmt.setString(++i, getName());
        pstmt.setBoolean(++i, getSuccess());
        pstmt.setString(++i, getDescription());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String summary = I18nUtil.marktr("WAN") + " " + getName() + " " + ( getSuccess() ? I18nUtil.marktr("passed") : I18nUtil.marktr("failed") ) + " WAN Failover " + I18nUtil.marktr("test") + " \"" + getDescription() + "\"";
        return summary;
    }
}

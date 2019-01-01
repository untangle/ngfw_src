/**
 * $Id$
 */

package com.untangle.app.wan_failover;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;
import java.io.Serializable;
import org.json.JSONString;

/**
 * Log event for a WAN state change (disconnect/reconnect)
 */
@SuppressWarnings("serial")
public class WanFailoverEvent extends LogEvent implements Serializable, JSONString
{
    public static enum Action { CONNECTED, DISCONNECTED };
    private Action action;
    private int interfaceId;
    private String name;
    private String osName;
    
    public WanFailoverEvent() { }
    
    public WanFailoverEvent( Action action, int interfaceId, String name, String osName )
    {
        super();
        this.action = action;
        this.interfaceId = interfaceId;
        this.name = name;
        this.osName = osName;
    }
    
    public int getInterfaceId() { return this.interfaceId; }
    public void setInterfaceId( int interfaceId ) { this.interfaceId = interfaceId; }

    public String getName() { return this.name; }
    public void setName( String newValue ) { this.name = newValue; }

    public String getOsName() { return this.osName; }
    public void setOsName(String newValue) { this.osName = newValue; }

    public Action getAction() { return action; }
    public void setAction( Action action ) { this.action = action; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "wan_failover_action_events" + getPartitionTablePostfix() + " " +
            "(time_stamp, interface_id, name, os_name, action) " + 
            "values " +
            "( ?, ?, ?, ?, ? )";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setInt(++i, getInterfaceId());
        pstmt.setString(++i, getName());
        pstmt.setString(++i, getOsName());
        pstmt.setString(++i, getAction().toString());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String action;
        switch (getAction()) {
        case CONNECTED: action = I18nUtil.marktr("came online"); break;
        case DISCONNECTED: action = I18nUtil.marktr("went offline"); break;
        default: action = I18nUtil.marktr("unknown");
        }
        String summary = I18nUtil.marktr("WAN") + " " + action;
        return summary;
    }
}

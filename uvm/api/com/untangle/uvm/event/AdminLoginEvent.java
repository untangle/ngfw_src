/**
 * $Id: AdminLoginEvent.java,v 1.00 2017/06/16 12:13:17 dmorris Exp $
 */
package com.untangle.uvm.event;

import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for an event
 */
@SuppressWarnings("serial")
public class AdminLoginEvent extends LogEvent
{
    private String login;
    private boolean local;
    private boolean succeeded;
    private InetAddress clientAddress;
    private String reason;
    
    public AdminLoginEvent() { }

    public AdminLoginEvent( String login, boolean local, InetAddress clientAddress, boolean succeeded, String reason )
    {
        this.login = login;
        this.local = local;
        this.clientAddress = clientAddress;
        this.succeeded = succeeded;
        this.reason = reason;
    }

    public String getLogin() { return login; }
    public void setLogin( String newValue ) { this.login = newValue; }

    public boolean getLocal() { return local; }
    public void setLocal( boolean newValue ) { this.local = newValue; }

    public InetAddress getClientAddress() { return clientAddress; }
    public void setClientAddress( InetAddress newValue ) { this.clientAddress = newValue; }

    public boolean getSucceeded() { return succeeded; }
    public void setSucceeded( boolean newValue ) { this.succeeded = newValue; }

    public String getReason() { return reason; }
    public void setReason( String newValue ) { this.reason = newValue; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "admin_logins" + getPartitionTablePostfix() + " " +
            "(time_stamp, login, local, client_addr, succeeded, reason) " +
            "values " +
            "(?, ?, ?, ?, ?, ?); ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setString(++i, getLogin());
        pstmt.setBoolean(++i, getLocal());
        pstmt.setObject(++i, getClientAddress().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setBoolean(++i, getSucceeded());
        pstmt.setString(++i, (getReason() == null ? null : getReason().substring(0,1)));

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String summary = I18nUtil.marktr("Administrator Login") + " " + getLogin() + " " + getSucceeded();

        return summary;
    }
    
}

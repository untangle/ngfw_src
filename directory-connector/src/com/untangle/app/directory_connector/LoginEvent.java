/**
 * $Id$
 */
package com.untangle.app.directory_connector;

import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for a login/login-attempt.
 */
@SuppressWarnings("serial")
public class LoginEvent extends LogEvent
{
    public static String EVENT_LOGIN = "I";
    public static String EVENT_AUTHENTICATE = "A";
    public static String EVENT_UPDATE = "U";
    public static String EVENT_LOGOUT = "O";

    public static String EVENT_LOGIN_TYPE_CLIENT = "W";
    public static String EVENT_LOGIN_TYPE_WIN = "A";
    public static String EVENT_LOGIN_TYPE_RADIUS = "R";
    public static String EVENT_LOGIN_TYPE_TEST = "T";

    private InetAddress clientAddr;
    private String loginName;
    private String domain;
    private String event;
    private String loginType;

    public LoginEvent() { }

    public LoginEvent(InetAddress clientAddr, String loginName, String domain, String event, String loginType)
    {
        this.clientAddr = clientAddr;
        this.loginName = loginName;
        this.domain = domain;
        this.event = event;
        this.loginType = loginType;
    }

    public InetAddress getClientAddr() { return clientAddr; }
    public void setClientAddr( InetAddress clientAddr ) { this.clientAddr = clientAddr; }

    public String getLoginName() { return loginName; }
    public void setLoginName( String loginName ) { this.loginName = loginName; }

    public String getDomain() { return domain; }
    public void setDomain( String newDomain ) { this.domain = newDomain; }

    public String getEvent() { return event; }
    public void setEvent( String newEvent ) { this.event = newEvent; }

    public String getLoginType() { return loginType; }
    public void setLoginType( String newLoginType ) { this.loginType = newLoginType; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "directory_connector_login_events" + getPartitionTablePostfix() + " " +
            "(time_stamp, login_name, domain, type, client_addr, login_type) " + 
            "values " +
            "( ?, ?, ?, ?, ?, ?)";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setString(++i, getLoginName());
        pstmt.setString(++i, getDomain());
        pstmt.setString(++i, getEvent());
        pstmt.setObject(++i, getClientAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setString(++i, getLoginType());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String action;
        switch( getEvent() ){
        case "I": action = I18nUtil.marktr("logged in to"); break;
        case "U": action = I18nUtil.marktr("refreshed on"); break;
        case "O": action = I18nUtil.marktr("logged out of"); break;
        default: action = I18nUtil.marktr("unknown"); 
        }
        
        String summary = I18nUtil.marktr("User") + " " + getLoginName() + " " + action + " " + getClientAddr();
        return summary;
    }
    
}

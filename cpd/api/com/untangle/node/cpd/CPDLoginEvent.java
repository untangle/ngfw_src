/**
 * $Id$
 */
package com.untangle.node.cpd;

import java.net.InetAddress;

import com.untangle.node.cpd.CPDSettings.AuthenticationType;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;

/**
 * Log event for a login/login-attempt.
 */
@SuppressWarnings("serial")
public class CPDLoginEvent extends LogEvent
{
    public enum EventType { LOGIN, UPDATE, FAILED, LOGOUT };

    private InetAddress clientAddr;
    private String loginName;
    private String authenticationTypeValue;
    private String eventValue;

    // constructors --------------------------------------------------------

    public CPDLoginEvent() { }

    public CPDLoginEvent(InetAddress clientAddr, String loginName, AuthenticationType type, EventType event)
    {
        this.clientAddr = clientAddr;
        this.loginName = loginName;
        setAuthenticationType(type);
        setEvent(event);
    }

    // accessors -----------------------------------------------------------

    /**
     * Client address
     */
    public InetAddress getClientAddr() { return clientAddr; }
    public void setClientAddr( InetAddress clientAddr ) { this.clientAddr = clientAddr; }

    /**
     * Login used to login
     */
    public String getLoginName() { return loginName; }
    public void setLoginName( String loginName ) { this.loginName = loginName; }

    private String getEventValue() { return eventValue; }
    private void setEventValue( String eventValue ) { this.eventValue = eventValue; }

    public EventType getEvent() { return EventType.valueOf(this.eventValue); }
    public void setEvent( EventType newEvent ) { this.eventValue = newEvent.toString(); }
    
    private String getAuthenticationTypeValue() { return authenticationTypeValue; }
    private void setAuthenticationTypeValue( String newValue ) { this.authenticationTypeValue = newValue; }
    
    public AuthenticationType getAuthenticationType() { return AuthenticationType.valueOf(this.authenticationTypeValue); }
    public void setAuthenticationType( AuthenticationType newValue ) { this.authenticationTypeValue = newValue.toString(); }

    @Override
    public String getDirectEventSql()
    {
        String sql = "INSERT INTO reports.n_cpd_login_events " +
            "(time_stamp, login_name, event, auth_type, client_addr) " +
            "values " +
            "( " +
            "timestamp '" + new java.sql.Timestamp(getTimeStamp().getTime()) + "'" + "," +
            "'" + getLoginName() + "'" + "," +
            "'" + getEvent() + "'" + "," +
            "'" + getAuthenticationTypeValue() + "'" + "," +
            "'" + getClientAddr().getHostAddress() + "'" + ") " +
            ";";
            return sql;
    }
    
    // Syslog methods ------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("client-addr", clientAddr);
        sb.addField("login-name", loginName);
        sb.addField("auth-type", authenticationTypeValue);
        sb.addField("type", eventValue);
    }

    public String getSyslogId()
    {
        return "CPD Login";
    }

    public SyslogPriority getSyslogPriority()
    {
        return SyslogPriority.INFORMATIONAL;
    }

    // Object methods ------------------------------------------------------

    public String toString()
    {
        return "CPDLoginEvent login-name: " + loginName + " authenticationType: " + authenticationTypeValue +  " + event: " + eventValue;
    }
}

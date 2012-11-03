/**
 * $Id: CaptureLoginEvent.java 31915 2012-05-11 00:45:52Z mahotz $
 */

package com.untangle.node.capture;

import com.untangle.node.capture.CaptureSettings.AuthenticationType;
import com.untangle.uvm.logging.LogEvent;

@SuppressWarnings("serial")
public class CaptureLoginEvent extends LogEvent
{
    public enum EventType { LOGIN, FAILED, TIMEOUT, INACTIVE, USER_LOGOUT, ADMIN_LOGOUT };

    private String clientAddr;
    private String loginName;
    private String authenticationTypeValue;
    private String eventValue;

    // constructors --------------------------------------------------------

    public CaptureLoginEvent() { }

    public CaptureLoginEvent(String clientAddr, String loginName, AuthenticationType type, EventType event)
    {
        this.clientAddr = clientAddr;
        this.loginName = loginName;
        setAuthenticationType(type);
        setEvent(event);
    }

    // accessors -----------------------------------------------------------

    public String getClientAddr() { return clientAddr; }
    public void setClientAddr( String clientAddr ) { this.clientAddr = clientAddr; }

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

    private static String sql = "INSERT INTO reports.n_capture_login_events " +
        "(time_stamp, login_name, event, auth_type, client_addr) " +
        "values " +
        "( ?, ?, ?, ?, ? )";

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i = 0;
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setString(++i, getLoginName());
        pstmt.setString(++i, getEvent().toString());
        pstmt.setString(++i, getAuthenticationTypeValue());
        pstmt.setString(++i, getClientAddr());
        return pstmt;
    }
}

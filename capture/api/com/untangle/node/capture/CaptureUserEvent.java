/**
 * $Id: CaptureUserEvent.java 31915 2012-05-11 00:45:52Z mahotz $
 */

package com.untangle.node.capture;

import com.untangle.node.capture.CaptureSettings.AuthenticationType;
import com.untangle.uvm.logging.LogEvent;

@SuppressWarnings("serial")
public class CaptureUserEvent extends LogEvent
{
    public enum EventType { LOGIN, FAILED, TIMEOUT, INACTIVE, USER_LOGOUT, ADMIN_LOGOUT };

    private String clientAddr;
    private String loginName;
    private String authenticationTypeValue;
    private String eventValue;
    private Long policyId;

    // constructors --------------------------------------------------------

    public CaptureUserEvent() { }

    public CaptureUserEvent(Long policyId, String clientAddr, String loginName, AuthenticationType type, EventType event)
    {
        setPolicyId(policyId);
        setClientAddr(clientAddr);
        setLoginName(loginName);
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

    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }

    private String getAuthenticationTypeValue() { return authenticationTypeValue; }
    private void setAuthenticationTypeValue( String newValue ) { this.authenticationTypeValue = newValue; }

    public AuthenticationType getAuthenticationType() { return AuthenticationType.valueOf(this.authenticationTypeValue); }
    public void setAuthenticationType( AuthenticationType newValue ) { this.authenticationTypeValue = newValue.toString(); }

    private static String sql = "INSERT INTO reports.n_capture_user_events " +
        "(time_stamp, policy_id, login_name, event_info, auth_type, client_addr) " +
        "values ( ?, ?, ?, ?, ?, ? )";

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i = 0;
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setLong(++i, getPolicyId().longValue());
        pstmt.setString(++i, getLoginName());
        pstmt.setString(++i, getEvent().toString());
        pstmt.setString(++i, getAuthenticationTypeValue());
        pstmt.setString(++i, getClientAddr());
        return pstmt;
    }
}

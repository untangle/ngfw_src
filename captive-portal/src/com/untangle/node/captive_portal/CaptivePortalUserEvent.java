/**
 * $Id$
 */
package com.untangle.node.captive_portal;

import java.net.InetAddress;

import com.untangle.node.captive_portal.CaptivePortalSettings.AuthenticationType;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

@SuppressWarnings("serial")
public class CaptivePortalUserEvent extends LogEvent
{
    public enum EventType
    {
        LOGIN, FAILED, TIMEOUT, INACTIVE, USER_LOGOUT, ADMIN_LOGOUT
    };

    private InetAddress clientAddr;
    private String loginName;
    private String authenticationTypeValue;
    private String eventValue;
    private Long policyId;

    public CaptivePortalUserEvent()
    {
    }

    public CaptivePortalUserEvent(Long policyId, InetAddress clientAddr, String loginName, AuthenticationType type, EventType event)
    {
        setPolicyId(policyId);
        setClientAddr(clientAddr);
        setLoginName(loginName);
        setAuthenticationType(type);
        setEvent(event);
    }

    public InetAddress getClientAddr() { return clientAddr; }
    public void setClientAddr(InetAddress clientAddr) { this.clientAddr = clientAddr; }

    public String getLoginName() { return loginName; }
    public void setLoginName(String loginName) { this.loginName = loginName; }

    private String getEventValue() { return eventValue; }
    private void setEventValue(String eventValue) { this.eventValue = eventValue; }

    public EventType getEvent() { return EventType.valueOf(this.eventValue); }
    public void setEvent(EventType newEvent) { this.eventValue = newEvent.toString(); }

    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }

    private String getAuthenticationTypeValue() { return authenticationTypeValue; }
    private void setAuthenticationTypeValue(String newValue) { this.authenticationTypeValue = newValue; }

    public AuthenticationType getAuthenticationType() { return AuthenticationType.valueOf(this.authenticationTypeValue); }
    public void setAuthenticationType(AuthenticationType newValue) { this.authenticationTypeValue = newValue.toString(); }

    @Override
    public java.sql.PreparedStatement getDirectEventSql(java.sql.Connection conn) throws Exception
    {
        String sql = "INSERT INTO reports.captive_portal_user_events" + getPartitionTablePostfix() + " " +
            "(time_stamp, policy_id, login_name, event_info, auth_type, client_addr) " +
            "values ( ?, ?, ?, ?, ?, ? )";

        java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);

        int i = 0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setLong(++i, getPolicyId().longValue());
        pstmt.setString(++i, getLoginName());
        pstmt.setString(++i, getEvent().toString());
        pstmt.setString(++i, getAuthenticationTypeValue());
        pstmt.setString(++i, getClientAddr().getHostAddress().toString());
        return pstmt;
    }

    @Override
    public String toSummaryString()
    {
        String action;
        switch (getEvent()) {
        case LOGIN: action = I18nUtil.marktr("logged in"); break;
        case FAILED: action = I18nUtil.marktr("failed to login"); break;
        case TIMEOUT: action = I18nUtil.marktr("timed out"); break;
        case INACTIVE: action = I18nUtil.marktr("timed out (inactivity)"); break;
        case USER_LOGOUT: action = I18nUtil.marktr("logged out"); break;
        case ADMIN_LOGOUT: action = I18nUtil.marktr("logged out (admin forced)"); break;
        default: action = I18nUtil.marktr("unknown"); break;
        }

        String summary = "Captive Portal: " + I18nUtil.marktr("User") + " " + getLoginName() + " " + action;
        return summary;
    }

}

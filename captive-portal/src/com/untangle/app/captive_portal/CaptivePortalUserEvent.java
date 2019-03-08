/**
 * $Id$
 */
package com.untangle.app.captive_portal;

import com.untangle.app.captive_portal.CaptivePortalSettings.AuthenticationType;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * This is the implementation of a User Event used when logging user
 * authentication events to the database.
 * 
 * @author mahotz
 * 
 */

@SuppressWarnings("serial")
public class CaptivePortalUserEvent extends LogEvent
{
    public enum EventType
    {
        LOGIN, FAILED, TIMEOUT, INACTIVE, USER_LOGOUT, ADMIN_LOGOUT, HOST_CHANGE
    };

    private String clientAddr;
    private String loginName;
    private String authenticationTypeValue;
    private String eventValue;
    private Integer policyId;

    public CaptivePortalUserEvent()
    {
    }

    public CaptivePortalUserEvent(Integer policyId, String clientAddr, String loginName, AuthenticationType type, EventType event)
    {
        setPolicyId(policyId);
        setClientAddr(clientAddr);
        setLoginName(loginName);
        setAuthenticationType(type);
        setEvent(event);
    }

// THIS IS FOR ECLIPSE - @formatter:off

    public String getClientAddr() { return clientAddr; }
    public void setClientAddr(String clientAddr) { this.clientAddr = clientAddr; }

    public String getLoginName() { return loginName; }
    public void setLoginName(String loginName) { this.loginName = loginName; }

    public String getEventValue() { return eventValue; }
    public void setEventValue(String eventValue) { this.eventValue = eventValue; }

    public EventType getEvent() { return EventType.valueOf(this.eventValue); }
    public void setEvent(EventType newEvent) { this.eventValue = newEvent.toString(); }

    public Integer getPolicyId() { return policyId; }
    public void setPolicyId(Integer policyId) { this.policyId = policyId; }

    public String getAuthenticationTypeValue() { return authenticationTypeValue; }
    public void setAuthenticationTypeValue(String newValue) { this.authenticationTypeValue = newValue; }

    public AuthenticationType getAuthenticationType() { return AuthenticationType.valueOf(this.authenticationTypeValue); }
    public void setAuthenticationType(AuthenticationType newValue) { this.authenticationTypeValue = newValue.toString(); }

// THIS IS FOR ECLIPSE - @formatter:on

    @Override
    public void compileStatements(java.sql.Connection conn, java.util.Map<String, java.sql.PreparedStatement> statementCache) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "captive_portal_user_events" + getPartitionTablePostfix() + " " + "(time_stamp, policy_id, login_name, event_info, auth_type, client_addr) " + "values ( ?, ?, ?, ?, ?, ? )";

        java.sql.PreparedStatement pstmt = getStatementFromCache(sql, statementCache, conn);

        int i = 0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setInt(++i, getPolicyId());
        pstmt.setString(++i, getLoginName());
        pstmt.setString(++i, getEvent().toString());
        pstmt.setString(++i, getAuthenticationTypeValue());
        pstmt.setString(++i, getClientAddr());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String action;
        switch (getEvent())
        {
        case LOGIN:
            action = I18nUtil.marktr("logged in");
            break;
        case FAILED:
            action = I18nUtil.marktr("failed to login");
            break;
        case TIMEOUT:
            action = I18nUtil.marktr("timed out");
            break;
        case INACTIVE:
            action = I18nUtil.marktr("timed out (inactivity)");
            break;
        case USER_LOGOUT:
            action = I18nUtil.marktr("logged out");
            break;
        case ADMIN_LOGOUT:
            action = I18nUtil.marktr("logged out (admin forced)");
            break;
        default:
            action = I18nUtil.marktr("unknown");
            break;
        }

        String summary = "Captive Portal: " + I18nUtil.marktr("User") + " " + getLoginName() + " " + action;
        return summary;
    }
}

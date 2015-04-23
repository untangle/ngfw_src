/**
 * $Id: SmtpMessageAddressEvent.java 39268 2014-12-11 18:07:09Z dmorris $
 */
package com.untangle.node.smtp;

import java.io.Serializable;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log e-mail message info.
 */
@SuppressWarnings("serial")
public class SmtpMessageAddressEvent extends LogEvent implements Serializable
{
    private Long messageId; /* msg_id */
    private SmtpMessageEvent messageInfo;
    private AddressKind kind;
    private String addr;
    private String personal;

    public SmtpMessageAddressEvent() { }

    public SmtpMessageAddressEvent( SmtpMessageEvent messageInfo, AddressKind kind, String addr, String personal )
    {
        this.messageInfo = messageInfo;
        this.messageId = messageInfo.getMessageId();
        this.kind = kind;
        if (addr.length() > SmtpMessageEvent.DEFAULT_STRING_SIZE) {
            addr = addr.substring(0, SmtpMessageEvent.DEFAULT_STRING_SIZE);
        }
        this.addr = addr;
        if (personal != null && personal.length() > SmtpMessageEvent.DEFAULT_STRING_SIZE) {
            personal = personal.substring(0, SmtpMessageEvent.DEFAULT_STRING_SIZE);
        }
        this.personal = personal;
    }

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long id) { this.messageId = id; }

    /**
     * The email address, in RFC822 format
     * 
     * @return email address.
     */
    public String getAddr()
    {
        return addr;
    }

    public void setAddr(String addr)
    {
        if (addr.length() > SmtpMessageEvent.DEFAULT_STRING_SIZE) {
            addr = addr.substring(0, SmtpMessageEvent.DEFAULT_STRING_SIZE);
        }
        this.addr = addr;
    }

    /**
     * Get a personal for display purposes.
     * 
     * @return personal.
     */
    public String getPersonal()
    {
        return personal;
    }

    public void setPersonal(String personal)
    {
        if (personal != null && personal.length() > SmtpMessageEvent.DEFAULT_STRING_SIZE) {
            personal = personal.substring(0, SmtpMessageEvent.DEFAULT_STRING_SIZE);
        }
        this.personal = personal;
    }

    /**
     * The kind of address (To, CC, etc).
     * 
     * @return addressKind.
     */
    public AddressKind getKind()
    {
        return kind;
    }

    public void setKind(AddressKind kind)
    {
        this.kind = kind;
    }

    @Override
    public java.sql.PreparedStatement getDirectEventSql(java.sql.Connection conn) throws Exception
    {
        String sql = "INSERT INTO reports.mail_addrs" +  getPartitionTablePostfix() + " " +
            "(time_stamp, " + 
            "session_id, client_intf, server_intf, " + "c_client_addr, c_client_port, c_server_addr, c_server_port, " + 
            "s_client_addr, s_client_port, s_server_addr, s_server_port, " + "policy_id,  " + "username,  " + 
            "msg_id, subject, " + "addr, addr_name, addr_kind,  " + "sender,  " + 
            "hostname) " + " VALUES " + "( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

        SessionEvent se = messageInfo.getSessionEvent();
        java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);

        int i = 0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setLong(++i, se.getSessionId());
        pstmt.setInt(++i, se.getClientIntf());
        pstmt.setInt(++i, se.getServerIntf());
        pstmt.setObject(++i, se.getCClientAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, se.getCClientPort());
        pstmt.setObject(++i, se.getCServerAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, se.getCServerPort());
        pstmt.setObject(++i, se.getSClientAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, se.getSClientPort());
        pstmt.setObject(++i, se.getSServerAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, se.getSServerPort());
        pstmt.setLong(++i, se.getPolicyId());
        pstmt.setString(++i, (se.getUsername() == null ? "" : se.getUsername()));
        pstmt.setLong(++i, messageInfo.getMessageId());
        pstmt.setString(++i, messageInfo.getSubject());
        pstmt.setString(++i, addr);
        pstmt.setString(++i, personal);
        pstmt.setString(++i, Character.toString(kind.getKey()));
        pstmt.setString(++i, messageInfo.getSender());
        pstmt.setString(++i, (se.getHostname() == null ? "" : se.getHostname()));

        return pstmt;
    }

    @Override
    public String toSummaryString()
    {
        String action;
        switch ( getKind().getKey() ) {
        case 'F': action = I18nUtil.marktr("sent"); break; // from
        case 'G': action = I18nUtil.marktr("sent"); break; // envelope from
        case 'T': action = I18nUtil.marktr("received"); break; // to
        case 'C': action = I18nUtil.marktr("received"); break; // cc
        case 'B': action = I18nUtil.marktr("received"); break; // envelope to
        default: action = I18nUtil.marktr("involved in");
        }
          
        String summary = messageInfo.getSessionEvent().toSummaryString() + "\n" +
            addr + " " + action + " " + I18nUtil.marktr("email") + ":\n" +
            I18nUtil.marktr("subject") + ": "  + messageInfo.getSubject() + "\n" +
            I18nUtil.marktr("sender") + ": "  + messageInfo.getSender();

        return summary;
    }

}

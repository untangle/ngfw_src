/**
 * $Id$
 */
package com.untangle.node.mail.papi;

import java.io.Serializable;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log e-mail message info.
 */
@SuppressWarnings("serial")
public class MessageInfoAddr extends LogEvent implements Serializable
{
    private Long messageId; /* msg_id */
    private MessageInfo messageInfo;
    private int position;
    private AddressKind kind;
    private String addr;
    private String personal;

    /* constructors */
    public MessageInfoAddr() { }

    public MessageInfoAddr(MessageInfo messageInfo, int position, AddressKind kind, String addr, String personal)
    {
        this.messageInfo = messageInfo;
        this.position = position;
        this.messageId = messageInfo.getMessageId();
        this.kind = kind;
        if (addr.length() > MessageInfo.DEFAULT_STRING_SIZE) {
            addr = addr.substring(0, MessageInfo.DEFAULT_STRING_SIZE);
        }
        this.addr = addr;
        if (personal != null
            && personal.length() > MessageInfo.DEFAULT_STRING_SIZE) {
            personal = personal.substring(0, MessageInfo.DEFAULT_STRING_SIZE);
        }
        this.personal = personal;
    }

    // accessors --------------------------------------------------------------


    /**
     * The MessageId object.
     */
    public Long getMessageId()
    {
        return messageId;
    }

    public void setMessageId(Long id)
    {
        this.messageId = id;
    }

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
        if (addr.length() > MessageInfo.DEFAULT_STRING_SIZE) {
            addr = addr.substring(0, MessageInfo.DEFAULT_STRING_SIZE);
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
        if (personal != null
            && personal.length() > MessageInfo.DEFAULT_STRING_SIZE) {
            personal = personal.substring(0,MessageInfo.DEFAULT_STRING_SIZE);
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

    private static String sql = "INSERT INTO reports.n_mail_addrs " +
        "(time_stamp, " + 
        "session_id, client_intf, server_intf, " + 
        "c_client_addr, c_client_port, c_server_addr, c_server_port, " + 
        "s_client_addr, s_client_port, s_server_addr, s_server_port, " + 
        "policy_id,  " + 
        "uid,  " + 
        "msg_id, subject, server_type,  " + 
        "addr_pos, addr, addr_name, addr_kind,  " + 
        "sender,  " + 
        "hname) " + 
        " VALUES " +
        "( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";
    
    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        SessionEvent se = messageInfo.getSessionEvent();
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;
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
        pstmt.setString(++i, String.valueOf(messageInfo.getServerType()));
        pstmt.setInt(++i, position);
        pstmt.setString(++i, addr);
        pstmt.setString(++i, personal);
        pstmt.setString(++i, Character.toString(kind.getKey()));
        pstmt.setString(++i, messageInfo.getSender());
        pstmt.setString(++i, (se.getHostname() == null ? "" : se.getHostname()));
        
        return pstmt;
    }
}

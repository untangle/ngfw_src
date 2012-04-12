/**
 * $Id$
 */
package com.untangle.node.mail.papi;

import java.io.Serializable;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
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
     * The MessageInfo object.
     */
    public MessageInfo getMessageInfo()
    {
        return messageInfo;
    }

    public void setMessageInfo(MessageInfo messageInfo)
    {
        this.messageInfo = messageInfo;
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

    @Override
    public boolean isDirectEvent()
    {
        return true;
    }

    @Override
    public String getDirectEventSql()
    {
        SessionEvent se = messageInfo.getSessionEvent();
        String sql = "INSERT INTO reports.n_mail_addrs " +
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
            "( " +
            "timestamp '" + new java.sql.Timestamp(getTimeStamp().getTime()) + "'" + "," +
            se.getId() + "," +
            se.getClientIntf() + "," +
            se.getServerIntf() + "," +
            "'" + se.getCClientAddr().getHostAddress() + "'" + "," +
            se.getCClientPort() + "," +
            "'" + se.getCServerAddr().getHostAddress() + "'" + "," +
            se.getCServerPort() + "," +
            "'" + se.getSClientAddr().getHostAddress() + "'" + "," +
            se.getSClientPort() + "," +
            "'" + se.getSServerAddr().getHostAddress() + "'" + "," +
            se.getSServerPort() + "," +
            se.getPolicyId() + "," +
            "'" + (se.getUsername() == null ? "" : se.getUsername()) + "'" + "," +
            "'" + messageInfo.getMessageId() + "'" + "," +
            "'" + messageInfo.getSubject() + "'" + "," +
            "'" + messageInfo.getServerType() + "'" + "," +

            "'" + position + "'" + "," +
            "'" + addr + "'" + "," +
            "'" + personal + "'" + "," +
            "'" + Character.toString(kind.getKey()) + "'" + "," +
            
            "'" + messageInfo.getSender() + "'" + "," +
            "'" + (se.getHostname() == null ? "" : se.getHostname()) + "'" + " )" +
            ";";
        return sql;
    }

    public String toString()
    {
        return kind + ": " + addr;
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("addr", addr);
    }

    public String getSyslogId()
    {
        return "MessageAddr";
    }

    public SyslogPriority getSyslogPriority()
    {
        return SyslogPriority.INFORMATIONAL; // statistics or normal operation
    }
    

}

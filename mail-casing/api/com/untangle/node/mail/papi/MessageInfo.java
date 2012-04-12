/**
 * $Id$
 */
package com.untangle.node.mail.papi;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log e-mail message info.
 */
@SuppressWarnings("serial")
public class MessageInfo extends LogEvent implements Serializable
{

    /* constants */
    public static final int SMTP_PORT = 25;
    public static final int POP3_PORT = 110;
    public static final int IMAP4_PORT = 143;

    // How big a varchar() do we get for default String fields. 
    public static final int MAX_STRING_SIZE = 255;

    /* columns */
    private SessionEvent sessionEvent;
    private String subject;
    private char serverType;
    private Date timeStamp = new Date();
    private String sender;
    private Long messageId;
    
    /* Senders/Receivers */
    private Set<MessageInfoAddr> addresses = new HashSet<MessageInfoAddr>();

    /* non-persistent fields */
    public Map<AddressKind,Integer> counts = new HashMap<AddressKind,Integer>();

    private static long nextId = 0;
    
    /* constructors */
    public MessageInfo() { }

    public MessageInfo(SessionEvent pe, int serverPort, String subject)
    {
        sessionEvent = pe;

        if (subject == null)
            subject = "";

        if (subject != null && subject.length() > MAX_STRING_SIZE) {
            subject = subject.substring(0, MAX_STRING_SIZE);
        }
        this.subject = subject;

        switch (serverPort) {
        case SMTP_PORT:
            serverType = 'S';
            break;
        case POP3_PORT:
            serverType = 'P';
            break;
        case IMAP4_PORT:
            serverType = 'I';
            break;
        default:
            serverType = 'U';
            break;
        }

        synchronized(this) {
            if (nextId == 0) 
                nextId = pe.getSessionId(); /* borrow the session Id as a starting point */
            this.messageId = nextId++;
        }
    }

    /* Business methods */
    public void addAddress(AddressKind kind, String address, String personal)
    {
        Integer p = counts.get(kind);
        if (null == p) {
            p = 0;
        }
        counts.put(kind, ++p);

        MessageInfoAddr newAddr = new MessageInfoAddr(this, p, kind, address, personal);
        addresses.add(newAddr);
        if (AddressKind.FROM.equals(kind))
            setSender(address);
        return;
    }

    /* public methods */

    /**
     * Set of the addresses involved (to, from, etc) in the email.
     *
     * @return the set of the email addresses involved in the email
     */
    public Set<MessageInfoAddr> getAddresses()
    {
        return addresses;
    }

    public void setAddresses(Set<MessageInfoAddr> s)
    {
        addresses = s;
        return;
    }

    /**
     * The message id
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
     * Get the SessionEvent.
     *
     * @return the SessionEvent.
     */
    public Long getSessionId()
    {
        return sessionEvent.getSessionId();
    }

    public void setSessionId( Long sessionId )
    {
        this.sessionEvent.setSessionId(sessionId);
    }

    public SessionEvent getSessionEvent()
    {
        return sessionEvent;
    }

    public void setSessionEvent(SessionEvent sessionEvent)
    {
        this.sessionEvent = sessionEvent;
    }

    /**
     * Identify RFC822 Subject.
     *
     * @return RFC822 Subject.
     */
    public String getSubject()
    {
        return subject;
    }

    public void setSubject(String subject)
    {
        if (subject != null && subject.length() > MAX_STRING_SIZE) {
            subject = subject.substring(0, MAX_STRING_SIZE);
        }
        this.subject = subject;
        return;
    }

    /**
     * Identify server type (SMTP, POP3, or IMAP4).
     *
     * @return server type.
     */
    public char getServerType()
    {
        return serverType;
    }

    public void setServerType(char serverType)
    {
        this.serverType = serverType;
        return;
    }

    /**
     * The email sender
     */
    public String getSender()
    {
        return sender;
    }

    public void setSender(String sender)
    {
        this.sender = sender;
    }
    
    /**
     * Identify approximate datetime that this message was received.
     *
     * @return datetime of message.
     */
    public Date getTimeStamp()
    {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp)
    {
        this.timeStamp = timeStamp;
        return;
    }

    @Override
    public boolean isDirectEvent()
    {
        return true;
    }

    @Override
    public List<String> getDirectEventSqls()
    {
        List<String> sqlList = new LinkedList<String>();
        
        String sql = "INSERT INTO reports.n_mail_msgs " +
            "(time_stamp, session_id, client_intf, server_intf, " +
            "c_client_addr, c_client_port, c_server_addr, c_server_port, " + 
            "s_client_addr, s_client_port, s_server_addr, s_server_port, " + 
            "policy_id, " +
            "uid, " +
            "msg_id, subject, server_type, " + 
            "sender, " +
            "hname " + ")" +
            " VALUES " +
            "( " +
            "timestamp '" + new java.sql.Timestamp(getTimeStamp().getTime()) + "'" + "," +
            getSessionEvent().getSessionId() + "," +
            getSessionEvent().getClientIntf() + "," +
            getSessionEvent().getServerIntf() + "," +
            "'" + getSessionEvent().getCClientAddr().getHostAddress() + "'" + "," +
            getSessionEvent().getCClientPort() + "," +
            "'" + getSessionEvent().getCServerAddr().getHostAddress() + "'" + "," +
            getSessionEvent().getCServerPort() + "," +
            "'" + getSessionEvent().getSClientAddr().getHostAddress() + "'" + "," +
            getSessionEvent().getSClientPort() + "," +
            "'" + getSessionEvent().getSServerAddr().getHostAddress() + "'" + "," +
            getSessionEvent().getSServerPort() + "," +
            getSessionEvent().getPolicyId() + "," +
            "'" + (getSessionEvent().getUsername() == null ? "" : getSessionEvent().getUsername()) + "'" + "," +
            "'" + getMessageId() + "'" + "," +
            "'" + getSubject() + "'" + "," +
            "'" + getServerType() + "'" + "," +
            "'" + getSender() + "'" + "," +
            "'" + (getSessionEvent().getHostname() == null ? "" : getSessionEvent().getHostname()) + "'" + " )" +
            ";";

        sqlList.add(sql);
        
        for (MessageInfoAddr addr : this.addresses) {
            sqlList.add(addr.getDirectEventSql());
        }
        
        return sqlList;
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("subject", getSubject());;
        sb.addField("sender", getSender());
    }

    public String getSyslogId()
    {
        return "Message";
    }

    public SyslogPriority getSyslogPriority()
    {
        return SyslogPriority.INFORMATIONAL; // statistics or normal operation
    }
    
}

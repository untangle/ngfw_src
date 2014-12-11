/**
 * $Id$
 */
package com.untangle.node.smtp;

import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.internet.MimeUtility;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log e-mail message info.
 */
@SuppressWarnings("serial")
public class MessageInfo extends LogEvent implements Serializable
{
    // How big a varchar() do we get for default String fields.
    public static final int MAX_STRING_SIZE = 255;

    /* columns */
    private SessionEvent sessionEvent;
    private String subject;
    private Long messageId;
    private File tmpFile;

    /* Senders/Receivers */
    private Set<MessageInfoAddr> addresses = new HashSet<MessageInfoAddr>();

    private static long nextId = 0;

    public MessageInfo() {}

    public MessageInfo( SessionEvent pe, String subject )
    {
        sessionEvent = pe;

        if (subject == null)
            subject = "";

        if (subject != null && subject.length() > MAX_STRING_SIZE) {
            subject = subject.substring(0, MAX_STRING_SIZE);
        }
        this.subject = decodeText(subject);

        synchronized (this) {
            if (nextId == 0)
                nextId = pe.getSessionId(); /* borrow the session Id as a starting point */
            this.messageId = nextId++;
        }
    }

    public void addAddress(AddressKind kind, String rawAddress, String rawPersonal)
    {
        String address = decodeText(rawAddress).toLowerCase();
        String personal = decodeText(rawPersonal);
        
        MessageInfoAddr newAddr = new MessageInfoAddr(this, kind, address, personal);

        addresses.add(newAddr);
        return;
    }

    public String getAddress( AddressKind kind )
    {
        if ( kind == null )
            return null;
        
        for ( MessageInfoAddr addr : addresses ) {
            if ( kind.equals( addr.getKind() ) ) {
                return addr.getAddr().toLowerCase();
            }
        }

        return null;
    }

    /**
     * Set of the addresses involved (to, from, etc) in the email.
     * 
     * @return the set of the email addresses involved in the email
     */
    public Set<MessageInfoAddr> trans_getAddresses()
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

    public void setSessionId(Long sessionId)
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

    public void setSubject(String rawSubject)
    {
        String subject = decodeText(rawSubject);
        if (subject != null && subject.length() > MAX_STRING_SIZE) {
            subject = subject.substring(0, MAX_STRING_SIZE);
        }
        this.subject = subject;
        return;
    }

    public String getSender()
    {
        String envelopeSender = getAddress( AddressKind.ENVELOPE_FROM );
        if ( envelopeSender != null )
            return envelopeSender;
        else
            return getAddress( AddressKind.FROM );
    }

    public String getReceiver()
    {
        return getAddress( AddressKind.ENVELOPE_TO );
    }

    public String getEnvelopeFromAddress()
    {
        return getAddress( AddressKind.ENVELOPE_FROM );
    }

    public String getEnvelopeToAddress()
    {
        return getAddress( AddressKind.ENVELOPE_TO );
    }
    
    public File getTmpFile()
    {
        return tmpFile;
    }
    
    public void setTmpFile(File tmpFile)
    {
        this.tmpFile = tmpFile;
    }
    
    
    private String decodeText(String rawValue)
    {
        if (rawValue == null)
            return null;
        String value = null;
        try {
            value = MimeUtility.decodeText(rawValue);
        } catch (UnsupportedEncodingException e) {
            value = rawValue;
        }
        return value;
    }

    private static String sql = "INSERT INTO reports.mail_msgs "
            + "(time_stamp, session_id, client_intf, server_intf, "
            + "c_client_addr, c_client_port, c_server_addr, c_server_port, "
            + "s_client_addr, s_client_port, s_server_addr, s_server_port, " + "policy_id, " + "username, "
            + "msg_id, subject, server_type, " + "sender, " + "hostname " + ")" + " VALUES "
            + "( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

    @Override
    public java.sql.PreparedStatement getDirectEventSql(java.sql.Connection conn) throws Exception
    {
        return null;
    }

    @Override
    public List<java.sql.PreparedStatement> getDirectEventSqls(java.sql.Connection conn) throws Exception
    {
        List<java.sql.PreparedStatement> sqlList = new LinkedList<java.sql.PreparedStatement>();
        java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);

        int i = 0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setLong(++i, getSessionEvent().getSessionId());
        pstmt.setInt(++i, getSessionEvent().getClientIntf());
        pstmt.setInt(++i, getSessionEvent().getServerIntf());
        pstmt.setObject(++i, getSessionEvent().getCClientAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSessionEvent().getCClientPort());
        pstmt.setObject(++i, getSessionEvent().getCServerAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSessionEvent().getCServerPort());
        pstmt.setObject(++i, getSessionEvent().getSClientAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSessionEvent().getSClientPort());
        pstmt.setObject(++i, getSessionEvent().getSServerAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSessionEvent().getSServerPort());
        pstmt.setLong(++i, getSessionEvent().getPolicyId());
        pstmt.setString(++i, (getSessionEvent().getUsername() == null ? "" : getSessionEvent().getUsername()));
        pstmt.setLong(++i, getMessageId());
        pstmt.setString(++i, getSubject());
        pstmt.setString(++i, "S");
        pstmt.setString(++i, getSender());
        pstmt.setString(++i, getSessionEvent().getHostname() == null ? "" : getSessionEvent().getHostname());

        sqlList.add(pstmt);

        for (MessageInfoAddr addr : this.addresses) {
            sqlList.add(addr.getDirectEventSql(conn));
        }

        return sqlList;
    }


    @Override
    public String toSummaryString()
    {
        String summary = "[ " + I18nUtil.marktr("sender") + ": \""  + getSender() + "\", " + I18nUtil.marktr("subject") + ": \""  + getSubject() + "\" ]";
        return summary;
    }
    
}

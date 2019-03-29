/**
 * $Id: SmtpMessageEvent.java 39268 2014-12-11 18:07:09Z dmorris $
 */
package com.untangle.app.smtp;

import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.mail.internet.MimeUtility;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log e-mail message info.
 */
@SuppressWarnings("serial")
public class SmtpMessageEvent extends LogEvent implements Serializable
{
    // How big a varchar() do we get for default String fields.
    public static final int MAX_STRING_SIZE = 255;

    /* columns */
    private SessionEvent sessionEvent;
    private String subject;
    private Long messageId;
    private File tmpFile;

    /* Senders/Receivers */
    private Set<SmtpMessageAddressEvent> addresses = new HashSet<>();

    private static long nextId = 0;

    public SmtpMessageEvent() {}

    public SmtpMessageEvent( SessionEvent pe, String subject )
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

    public void addAddress( AddressKind kind, String rawAddress, String rawPersonal )
    {
        String address = decodeText(rawAddress);
        address = (address != null ? address.toLowerCase() : null);
        String personal = decodeText(rawPersonal);
        
        SmtpMessageAddressEvent newAddr = new SmtpMessageAddressEvent(this, kind, address, personal);

        addresses.add(newAddr);
        return;
    }

    public String getAddress( AddressKind kind )
    {
        if ( kind == null )
            return null;
        
        for ( SmtpMessageAddressEvent addr : addresses ) {
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
    public Set<SmtpMessageAddressEvent> getAddresses() { return addresses; }
    public void setAddresses( Set<SmtpMessageAddressEvent> newValue ) { this.addresses = newValue; }

    /**
     * The message id.
     * @return Message id.
     */
    public Long getMessageId() { return messageId; }
    public void setMessageId( Long newValue ) { this.messageId = newValue; }

    /**
     * Get the SessionEvent.
     * 
     * @return the SessionEvent.
     */
    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent( SessionEvent newValue ) { this.sessionEvent = newValue; }

    public Long getSessionId()
    {
        return sessionEvent.getSessionId();
    }

    public void setSessionId(Long sessionId)
    {
        this.sessionEvent.setSessionId(sessionId);
    }

    /**
     * Identify RFC822 Subject.
     * 
     * @return RFC822 Subject.
     */
    public String getSubject()
    {
        return this.subject;
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
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "mail_msgs" + getPartitionTablePostfix() + " " +
            "(time_stamp, session_id, client_intf, server_intf, " +
            "c_client_addr, c_client_port, c_server_addr, c_server_port, " +
            "s_client_addr, s_client_port, s_server_addr, s_server_port, " + "policy_id, " + "username, " +
            "msg_id, subject, " + "sender, " + "hostname " + ")" + " VALUES " +
            "( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

        List<java.sql.PreparedStatement> sqlList = new LinkedList<>();

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

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
        pstmt.setString(++i, getSender());
        pstmt.setString(++i, getSessionEvent().getHostname() == null ? "" : getSessionEvent().getHostname());

        pstmt.addBatch();

        for (SmtpMessageAddressEvent addr : this.addresses) {
            addr.compileStatements(conn, statementCache);
        }

        return;
    }

    @Override
    public String toSummaryString()
    {
        String summary = "[ " + I18nUtil.marktr("sender") + ": \""  + getSender() + "\", " + I18nUtil.marktr("subject") + ": \""  + getSubject() + "\" ]";
        return summary;
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
}

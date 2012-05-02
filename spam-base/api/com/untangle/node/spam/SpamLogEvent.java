/**
 * $Id$
 */
package com.untangle.node.spam;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.net.InetAddress;

import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.node.mail.papi.AddressKind;
import com.untangle.node.mail.papi.MessageInfo;
import com.untangle.node.mail.papi.MessageInfoAddr;

/**
 * Log for POP3/IMAP Spam events.
 */
@SuppressWarnings("serial")
public class SpamLogEvent extends LogEvent
{
    // action types
    public static final int PASSED = 0; // pass or clean message
    public static final int MARKED = 1;
    public static final int BLOCKED = 2;
    public static final int QUARANTINED = 3;
    public static final int SAFELISTED = 4;
    public static final int OVERSIZED = 5;

    private Long messageId;
    private MessageInfo messageInfo;
    private float score;
    private boolean isSpam;
    private SpamMessageAction action;
    private String vendorName;

    // constructors -----------------------------------------------------------

    public SpamLogEvent() { }

    public SpamLogEvent(MessageInfo messageInfo, float score, boolean isSpam, SpamMessageAction action, String vendorName)
    {
        this.messageInfo = messageInfo;
        this.messageId = messageInfo.getMessageId();
        this.score = score;
        this.isSpam = isSpam;
        this.action = action;
        this.vendorName = vendorName;
    }

    public String getSender()
    {
        return get(AddressKind.FROM);
    }

    public String getReceiver()
    {
        return get(AddressKind.TO);
    }

    public String getSubject()
    {
        return null == getMessageInfo() ? "" : getMessageInfo().getSubject();
    }

    public InetAddress getClientAddr()
    {
        if (null == getMessageInfo()) {
            return null;
        } else {
            SessionEvent pe = getMessageInfo().getSessionEvent();
            return null == pe ? null : pe.getCClientAddr();
        }
    }

    public int getClientPort()
    {
        if (null == getMessageInfo()) {
            return -1;
        } else {
            SessionEvent pe = getMessageInfo().getSessionEvent();
            return null == pe ? -1 : pe.getCClientPort();
        }
    }

    public InetAddress getServerAddr()
    {
        if (null == getMessageInfo()) {
            return null;
        } else {
            SessionEvent pe = getMessageInfo().getSessionEvent();
            return null == pe ? null : pe.getSServerAddr();
        }
    }

    public int getServerPort()
    {
        if (null == getMessageInfo()) {
            return -1;
        } else {
            SessionEvent pe = getMessageInfo().getSessionEvent();
            return null == pe ? -1 : pe.getSServerPort();
        }
    }

    // accessors --------------------------------------------------------------

    /**
     * The message id
     */
    public Long getMessageId() { return messageId; }
    public void setMessageId( Long id ) { this.messageId = id; }

    /**
     * Associate e-mail message info with event.
     */
    public MessageInfo getMessageInfo() { return messageInfo; }
    public void setMessageInfo( MessageInfo info ) { this.messageInfo = info; }
    
    /**
     * Spam scan score.
     */
    public float getScore() { return score; }
    public void setScore( float score ) { this.score = score; }

    /**
     * Was it declared spam?
     */
    public boolean isSpam() { return isSpam; }
    public void setSpam( boolean isSpam ) { this.isSpam = isSpam; }

    /**
     * The action taken
     */
    public SpamMessageAction getAction() { return action; }
    public void setAction(SpamMessageAction action) { this.action = action; }

    /**
     * Spam scanner vendor.
     */
    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    @Override
    public boolean isDirectEvent()
    {
        return true;
    }

    @Override
    public List<String> getDirectEventSqls()
    {
        List<String> sqlList = new LinkedList<String>();
        String sql;

        String prefix = ""; /* FIXME this is a hack - we should use proper column names */
        if ("spamassassin".equals(getVendorName().toLowerCase()))
            prefix = "sa";
        else if ("commtouchas".equals(getVendorName().toLowerCase()))
            prefix = "ct";
        else {
            throw new RuntimeException("Unknown vendor name: " + getVendorName());
        }
        
        sql = "UPDATE reports.n_mail_msgs " +
            "SET " +
            prefix + "_is_spam = " + "'" + isSpam() + "'" + ", " +
            prefix + "_score = "  + "'" + getScore() + "'" + ", " +
            prefix + "_action = "  + "'" + getAction().getKey() + "'" + " " +
            "WHERE " +
            "msg_id = " + getMessageId() +
            ";";
        sqlList.add(sql);
        
        sql = "UPDATE reports.n_mail_addrs " +
            "SET " +
            prefix + "_is_spam = " + "'" + isSpam() + "'" + ", " +
            prefix + "_score = "  + "'" + getScore() + "'" + ", " +
            prefix + "_action = "  + "'" + getAction().getKey() + "'" + " " +
            "WHERE " +
            "msg_id = " + getMessageId() +
            ";";
        sqlList.add(sql);

        return sqlList;
    }

    public void appendSyslog(SyslogBuilder sb)
    {
        SessionEvent pe = getMessageInfo().getSessionEvent();
        if (null != pe) {
            pe.appendSyslog(sb);
        }

        sb.startSection("info");
        sb.addField("vendor", getVendorName());
        sb.addField("score", getScore());
        sb.addField("spam", isSpam());
        sb.addField("sender", getSender());
        sb.addField("receiver", getReceiver());
        sb.addField("subject", getSubject());
    }

    public String getSyslogId()
    {
        return "Mail";
    }

    public SyslogPriority getSyslogPriority()
    {
        return SyslogPriority.INFORMATIONAL;
    }

    // internal methods ---------------------------------------------------------

    protected String get(AddressKind kind)
    {
        MessageInfo messageInfo = getMessageInfo();

        if (null == messageInfo) {
            return "";
        } else {
            for (Iterator<MessageInfoAddr> i = messageInfo.getAddresses().iterator(); i.hasNext(); ) {
                MessageInfoAddr mi = i.next();

                if (mi.getKind() == kind) {
                    String addr = mi.getAddr();
                    if (addr == null)
                        return "";
                    else
                        return addr;
                }
            }

            return "";
        }
    }
    
}

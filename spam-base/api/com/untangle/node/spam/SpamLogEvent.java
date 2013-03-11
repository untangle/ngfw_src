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
import com.untangle.uvm.logging.LogEvent;
import com.untangle.node.mail.papi.AddressKind;
import com.untangle.node.mail.papi.MessageInfo;
import com.untangle.node.mail.papi.MessageInfoAddr;

/**
 * Event for Spam events.
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
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        return null;
    }

    @Override
    public List<java.sql.PreparedStatement> getDirectEventSqls( java.sql.Connection conn ) throws Exception
    {
        List<java.sql.PreparedStatement> sqlList = new LinkedList<java.sql.PreparedStatement>();
        String sql;
        int i=0;
        java.sql.PreparedStatement pstmt;
        
        String prefix = ""; /* FIXME this is a hack - we should use proper column names */
        if ("spamassassin".equals(getVendorName().toLowerCase()))
            prefix = "sa";
        else if ("commtouchas".equals(getVendorName().toLowerCase()))
            prefix = "ct";
        else if ("clam".equals(getVendorName().toLowerCase()))
            prefix = "phish";
        else {
            throw new RuntimeException("Unknown vendor name: " + getVendorName());
        }
        
        sql = "UPDATE reports.mail_msgs " +
            "SET " +
            prefix + "_is_spam = ?, " +
            prefix + "_score = ?, " + 
            prefix + "_action = ? " +
            "WHERE " +
            "msg_id = ? ";

        pstmt = conn.prepareStatement( sql );
        i=0;
        pstmt.setBoolean(++i, isSpam());
        pstmt.setFloat(++i, getScore());
        pstmt.setString(++i, String.valueOf(getAction().getKey()));
        pstmt.setLong(++i, getMessageId());
        sqlList.add(pstmt);
        
        sql = "UPDATE reports.mail_addrs " +
            "SET " +
            prefix + "_is_spam = ?, " +
            prefix + "_score = ?, " +
            prefix + "_action = ? " +
            "WHERE " +
            "msg_id = ? ";
        pstmt = conn.prepareStatement( sql );
        i=0;
        pstmt.setBoolean(++i, isSpam());
        pstmt.setFloat(++i, getScore());
        pstmt.setString(++i, String.valueOf(getAction().getKey()));
        pstmt.setLong(++i, getMessageId());
        sqlList.add(pstmt);
        
        return sqlList;
    }

    // internal methods ---------------------------------------------------------

    protected String get(AddressKind kind)
    {
        MessageInfo messageInfo = getMessageInfo();

        if (null == messageInfo) {
            return "";
        } else {
            for (Iterator<MessageInfoAddr> i = messageInfo.trans_getAddresses().iterator(); i.hasNext(); ) {
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

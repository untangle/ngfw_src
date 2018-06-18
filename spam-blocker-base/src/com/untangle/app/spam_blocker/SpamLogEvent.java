/**
 * $Id$
 */

package com.untangle.app.spam_blocker;

import java.util.Iterator;
import java.net.InetAddress;
import java.io.Serializable;
import org.json.JSONString;

import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.app.smtp.AddressKind;
import com.untangle.app.smtp.SmtpMessageEvent;
import com.untangle.app.smtp.SmtpMessageAddressEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Event for Spam events.
 */
@SuppressWarnings("serial")
public class SpamLogEvent extends LogEvent implements Serializable, JSONString
{
    private Long messageId;
    private SmtpMessageEvent messageInfo;
    private float score;
    private boolean isSpam;
    private SpamMessageAction action;
    private String vendorName;
    private String testsString;

    // constructors -----------------------------------------------------------

    public SpamLogEvent() { }

    public SpamLogEvent(SmtpMessageEvent messageInfo, float score, boolean isSpam, SpamMessageAction action, String vendorName, String testsString)
    {
        this.messageInfo = messageInfo;
        this.messageId = messageInfo.getMessageId();
        this.score = score;
        this.isSpam = isSpam;
        this.action = action;
        this.vendorName = vendorName;
        this.testsString = testsString;
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
        return null == getSmtpMessageEvent() ? "" : getSmtpMessageEvent().getSubject();
    }

    public InetAddress getClientAddr()
    {
        if (null == getSmtpMessageEvent()) {
            return null;
        } else {
            SessionEvent pe = getSmtpMessageEvent().getSessionEvent();
            return null == pe ? null : pe.getCClientAddr();
        }
    }

    public int getClientPort()
    {
        if (null == getSmtpMessageEvent()) {
            return -1;
        } else {
            SessionEvent pe = getSmtpMessageEvent().getSessionEvent();
            return null == pe ? -1 : pe.getCClientPort();
        }
    }

    public InetAddress getServerAddr()
    {
        if (null == getSmtpMessageEvent()) {
            return null;
        } else {
            SessionEvent pe = getSmtpMessageEvent().getSessionEvent();
            return null == pe ? null : pe.getSServerAddr();
        }
    }

    public int getServerPort()
    {
        if (null == getSmtpMessageEvent()) {
            return -1;
        } else {
            SessionEvent pe = getSmtpMessageEvent().getSessionEvent();
            return null == pe ? -1 : pe.getSServerPort();
        }
    }

    // accessors --------------------------------------------------------------

    /*
     * The message id
     */
    public Long getMessageId() { return messageId; }
    public void setMessageId( Long newValue ) { this.messageId = newValue; }

    /*
     * Associate e-mail message info with event.
     */
    public SmtpMessageEvent getSmtpMessageEvent() { return messageInfo; }
    public void setSmtpMessageEvent( SmtpMessageEvent newValue ) { this.messageInfo = newValue; }
    
    /*
     * Spam scan score.
     */
    public float getScore() { return score; }
    public void setScore( float newValue ) { this.score = newValue; }

    /*
     * Was it declared spam?
     */
    public boolean isSpam() { return isSpam; }
    public void setSpam( boolean newValue ) { this.isSpam = newValue; }

    /*
     * The action taken
     */
    public SpamMessageAction getAction() { return action; }
    public void setAction( SpamMessageAction newValue ) { this.action = newValue; }

    /*
     * Spam scanner vendor.
     */
    public String getVendorName() { return vendorName; }
    public void setVendorName( String newValue ) { this.vendorName = newValue; }

    /*
     * The list of tests hit (represented in a single string)
     */
    public String getTestsString() { return testsString; }
    public void setTestsString( String newValue ) { this.testsString = newValue; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql;
        int i=0;
        java.sql.PreparedStatement pstmt;
        
        String prefix = ""; /* XXX this is a hack - we should use proper column names */
        if ("spamblockerlite".equals(getVendorName().toLowerCase()))
            prefix = "spam_blocker_lite";
        else if ("spamblocker".equals(getVendorName().toLowerCase()))
            prefix = "spam_blocker";
        else if ("phishblocker".equals(getVendorName().toLowerCase()))
            prefix = "phish_blocker";
        else {
            throw new RuntimeException("Unknown vendor name: " + getVendorName());
        }
        
        sql = "UPDATE " + schemaPrefix() + "mail_msgs" + messageInfo.getPartitionTablePostfix() + " " +
            "SET " +
            prefix + "_is_spam = ?, " +
            prefix + "_score = ?, " + 
            prefix + "_tests_string = ?, " +
            prefix + "_action = ? " +
            "WHERE " +
            "msg_id = ? ";

        pstmt = getStatementFromCache( sql, statementCache, conn );        
        i=0;
        pstmt.setBoolean(++i, isSpam());
        pstmt.setFloat(++i, getScore());
        pstmt.setString(++i, getTestsString());
        pstmt.setString(++i, String.valueOf(getAction().getKey()));
        pstmt.setLong(++i, getMessageId());
        pstmt.addBatch();
        
        sql = "UPDATE " + schemaPrefix() + "mail_addrs" + messageInfo.getPartitionTablePostfix() + " " + 
            "SET " +
            prefix + "_is_spam = ?, " +
            prefix + "_score = ?, " +
            prefix + "_tests_string = ?, " +
            prefix + "_action = ? " +
            "WHERE " +
            "msg_id = ? ";
        pstmt = getStatementFromCache( sql, statementCache, conn );        
        i=0;
        pstmt.setBoolean(++i, isSpam());
        pstmt.setFloat(++i, getScore());
        pstmt.setString(++i, getTestsString());
        pstmt.setString(++i, String.valueOf(getAction().getKey()));
        pstmt.setLong(++i, getMessageId());
        pstmt = getStatementFromCache( sql, statementCache, conn );        
        pstmt.addBatch();

        return;
    }

    protected String get(AddressKind kind)
    {
        SmtpMessageEvent messageInfo = getSmtpMessageEvent();

        if (null == messageInfo) {
            return "";
        } else {
            for (Iterator<SmtpMessageAddressEvent> i = messageInfo.getAddresses().iterator(); i.hasNext(); ) {
                SmtpMessageAddressEvent mi = i.next();

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

    @Override
    public String toSummaryString()
    {
        String appName;
        switch ( vendorName ) {
        case "spamassassin": appName = "Spam Blocker Lite"; break;
        case "spamblocker": appName = "Spam Blocker"; break;
        default: appName = "Spam Blocker"; break;
        }

        String summary = appName + " " + I18nUtil.marktr("scored") + " "  + messageInfo.toSummaryString() + " " + I18nUtil.marktr("as") + " " + getScore() + " " + ( isSpam() ? "(spam)" : "(ham)" );
        return summary;
    }

    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}

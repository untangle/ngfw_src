/**
 * $Id: InboxRecordTag.java 34293 2013-03-17 05:22:02Z dmorris $
 */
package com.untangle.node.smtp.web.euv.tags;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.jsp.PageContext;

import com.untangle.node.smtp.quarantine.InboxRecord;
import com.untangle.node.util.JSEscape;


/**
 * Works with InboxIndexTag
 *
 */
@SuppressWarnings("serial")
public final class InboxRecordTag extends SingleValueTag
{
    private static final String INBOX_RECORD_PS_KEY = "untangle.inbox_record";
    
    private static final String ELLIPSE_STR = "...";
    private static final int SENDER_MAX_LENGTH = 32; // plus ellipse
    private static final int SUBJECT_MAX_LENGTH = 42; // plus ellipse

    // constant values must be in lower case
    public static final String MAILID_PROP = "mailid";
    public static final String SENDER_PROP = "sender";
    public static final String TSENDER_PROP = "tsender"; // truncated
    public static final String TSUBJECT_PROP = "tsubject"; // truncated
    public static final String FSCORE_PROP = "fdetail"; // formatted
    public static final String FDATE_PROP = "fdate"; // formatted
    public static final String FTIME_PROP = "ftime"; // formatted
    public static final String FSIZE_PROP = "fsize"; // formatted

    private String m_propName;
    private boolean m_jsEscape = true;

    public void setProp(String s) {
        m_propName = s;
    }
    public String getProp() {
        return m_propName;
    }

    public void setJSEscape(boolean escape) {
        m_jsEscape = escape;
    }

    public boolean isJSEscape() {
        return m_jsEscape;
    }

    @Override
    protected String getValue() {
        String ret = propNameToProp(getCurrent(pageContext), getProp());
        ret = m_jsEscape?JSEscape.escapeJS(ret):ret;
        return ret;
    }

    private String propNameToProp(InboxRecord record, String name) {
        if(record == null || name == null) {
            return null;
        }
        name = name.trim().toLowerCase();
        if(name.equals(MAILID_PROP)) {
            return record.getMailID();
        }
        if(name.equals(SENDER_PROP)) {
            return record.getMailSummary().getSender();
        }
        if(name.equals(TSENDER_PROP)) {
            return record.getMailSummary().getTruncatedSender();
        }
        if(name.equals(TSUBJECT_PROP)) {
            return record.getMailSummary().getTruncatedSubject();
        }
        if(name.equals(FSCORE_PROP)) {
            return record.getMailSummary().getFormattedQuarantineDetail();
        }
        if(name.equals(FDATE_PROP)) {
            return getFormattedDate(record.getInternDate());
        }
        if(name.equals(FTIME_PROP)) {
            return getFormattedTime(record.getInternDate());
        }
        if(name.equals(FSIZE_PROP)) {
            return getFormattedSize(record.getSize());
        }
        return null;
    }
    
    private static final String getFormattedDate(long internDate)
    {
        try {
            Date iDate = new Date(internDate);
            SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yy");
            return dateFormat.format(iDate).toString();
        } catch (Exception ex) {
            return "<unknown>";
        }
    }

    private static final String getFormattedTime(long internDate)
    {
        try {
            Date iDate = new Date(internDate);
            SimpleDateFormat dateFormat = new SimpleDateFormat("H:mm");
            return dateFormat.format(iDate).toString();
        } catch (Exception ex) {
            return "<unknown>";
        }
    }

    private static final String getFormattedSize(long size)
    {
        try {
            // in kilobytes
            return String.format("%01.1f", new Float(size / 1024.0));
        } catch (Exception ex) {
            return "<unknown>";
        }
    }
    
    private static String truncate(String source, int maxLength)
    {
        StringBuffer truncateSource = new StringBuffer(source);
        if (maxLength < truncateSource.length()) {
            truncateSource.setLength(maxLength);
            truncateSource.append(ELLIPSE_STR);
        }
        return truncateSource.toString();
    }
    
    public static String getTruncatedSender(String sender)
    {
        return truncate(sender, SENDER_MAX_LENGTH);
    }

    public String getTruncatedSubject(String subject)
    {
        return truncate(subject, SUBJECT_MAX_LENGTH);
    }

    public String getFormattedQuarantineDetail(String quarantineDetail)
    {
        // Attempts to convert to a formatted float. If this fails (i.e.
        // it isn't a number) then just return the detail.
        try {
            float f = Float.parseFloat(quarantineDetail);
            return String.format("%03.1f", f);
        } catch (Exception ex) {
            if (true == quarantineDetail.equals("Message determined to be a fraud attempt")) {
                // no conversion script so catch and change for display purposes
                // here
                return "Identity Theft";
            } else {
                return quarantineDetail;
            }
        }
    } 

    /**
     * Returns null if not found
     */
    public static InboxRecord getCurrent(PageContext pageContext) {
        return (InboxRecord) pageContext.getAttribute(INBOX_RECORD_PS_KEY);
    }

    public static void setCurrent(PageContext pageContext, InboxRecord record) {
        pageContext.setAttribute(INBOX_RECORD_PS_KEY, record, PageContext.PAGE_SCOPE);
    }

    public void release() {
        m_jsEscape = true;
        super.release();
    }
}

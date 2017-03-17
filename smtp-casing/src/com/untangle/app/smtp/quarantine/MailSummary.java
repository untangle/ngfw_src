/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

/**
 * Summary of a mail within the Quarantine.
 */
@SuppressWarnings("serial")
public final class MailSummary implements Serializable
{

    private static final int SENDER_MAX_LENGTH = 32; // plus ellipse
    private static final int SUBJECT_MAX_LENGTH = 42; // plus ellipse
    private static final String ELLIPSE_STR = "...";

    private int attachmentCount = 0;
    private String sender;
    private String subject;
    private String quarantineCategory;
    private String quarantineDetail;
    private long quarantineSize;

    public MailSummary() {
    }

    public MailSummary(String sender, String subject, String quarantineCategory, String quarantineDetail,
            int attachmentCount, long quarantineSize) {

        this.sender = sender;
        this.subject = subject;
        this.quarantineCategory = quarantineCategory;
        this.quarantineDetail = quarantineDetail;
        this.attachmentCount = attachmentCount;
        this.quarantineSize = quarantineSize;
    }

    /** Getters and Setters **/

    public int getAttachmentCount()
    {
        return attachmentCount;
    }

    public void setAttachmentCount(int attachmentCount)
    {
        this.attachmentCount = attachmentCount;
    }

    public String getSender()
    {
        return sender;
    }

    public void setSender(String sender)
    {
        this.sender = sender;
    }

    public String getSubject()
    {
        return subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public String getQuarantineCategory()
    {
        return quarantineCategory;
    }

    public void setQuarantineCategory(String cat)
    {
        this.quarantineCategory = cat;
    }

    public String getQuarantineDetail()
    {
        return quarantineDetail;
    }

    public void setQuarantineDetail(String det)
    {
        this.quarantineDetail = det;
    }

    public long getQuarantineSize()
    {
        return quarantineSize;
    }

    public void setQuarantineSize(long size)
    {
        this.quarantineSize = size;
    }

    /**
     * Convienence method (needed for Velocity since "getAttachmentCount()>0" seems to have bugs)
     */
    public boolean hasAttachments()
    {
        return getAttachmentCount() > 0;
    }

    public String getTruncatedSender()
    {
        return truncate(sender, SENDER_MAX_LENGTH);
    }

    public String getTruncatedSubject()
    {
        return truncate(subject, SUBJECT_MAX_LENGTH);
    }

    public String getFormattedQuarantineDetail()
    {
        // Attempts to convert to a formatted float. If this fails (i.e.
        // it isn't a number) then just return the detail.
        if (quarantineDetail == null)
            return null;
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

    public void setFormattedQuarantineDetail(String detail)
    {
        this.quarantineDetail = detail;
    }

    private String truncate(String source, int maxLength)
    {
        if (source == null)
            return null;
        StringBuffer truncateSource = new StringBuffer(source);
        if (maxLength < truncateSource.length()) {
            truncateSource.setLength(maxLength);
            truncateSource.append(ELLIPSE_STR);
        }
        return truncateSource.toString();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Sender: ").append(getSender()).append(", Subject: ").append(getSubject()).append(", Cat: ")
                .append(getQuarantineCategory()).append(", AttachCount: ").append(getAttachmentCount())
                .append(", Detail: ").append(getQuarantineDetail()).append(", Size: ").append(getQuarantineSize());

        return sb.toString();
    }

    /************** Tests ******************/

    public static String runTest(String[] args) throws Exception
    {

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputStream in;
            
        try {
            in = new FileInputStream(args[0]);
        } catch (Exception e) {
            return "File not found";
        }
        // InputSource input = new org.xml.sax.InputSource(in);

        String result = "";
        try {
            while (true) {
                Document document = builder.parse(in);
                result += "Got a document\n";
            }
        } catch (Exception ex) {
            result += "exception:\n" + ex.toString();
        }

        result += "MailSummary";
        return result;
    }
}

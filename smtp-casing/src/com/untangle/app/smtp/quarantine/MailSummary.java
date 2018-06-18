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

    /**
     * Initialize instance of MailSummary.
     * @return Instance of MailSummary.
     */
    public MailSummary() {
    }

    /**
     * Initialize instance of MailSummary.
     * @param  sender             Sender address.
     * @param  subject            Message subject.
     * @param  quarantineCategory Quarantine category.
     * @param  quarantineDetail   Quarantine detail.
     * @param  attachmentCount    Attachment count.
     * @param  quarantineSize     Quarantine size.
     * @return Instance of MailSummary.
     */
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

    /**
     * Return attachment message count.
     * @return attachment message count.
     */
    public int getAttachmentCount()
    {
        return attachmentCount;
    }

    /**
     * Write attachment message count.
     * @param attachmentCount attachment message count.
     */
    public void setAttachmentCount(int attachmentCount)
    {
        this.attachmentCount = attachmentCount;
    }

    /**
     * Get quarantine sender address.
     * @return String of address.
     */
    public String getSender()
    {
        return sender;
    }

    /**
     * Write quarantine sender address.
     * @param sender String of address.
     */
    public void setSender(String sender)
    {
        this.sender = sender;
    }

    /**
     * Return message subject.
     * @return String of subject.
     */
    public String getSubject()
    {
        return subject;
    }

    /**
     * Write message subject.
     * @param subject String of subject.
     */
    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    /**
     * Return quarantine category.
     * @return String of quarantine category.
     */
    public String getQuarantineCategory()
    {
        return quarantineCategory;
    }

    /**
     * Write quarantine category.
     * @param cat String of quarantine category.
     */
    public void setQuarantineCategory(String cat)
    {
        this.quarantineCategory = cat;
    }

    /**
     * Return quarantine detail.
     * @return String of quarantine detail.
     */
    public String getQuarantineDetail()
    {
        return quarantineDetail;
    }

    /**
     * Write quarantine detail.
     * @param det String of quarantine detail.
     */
    public void setQuarantineDetail(String det)
    {
        this.quarantineDetail = det;
    }

    /**
     * Return quarantine size.
     * @return Length of quarantine.
     */
    public long getQuarantineSize()
    {
        return quarantineSize;
    }

    /**
     * Write quarantine size.
     * @param size Length of quarantine.
     */
    public void setQuarantineSize(long size)
    {
        this.quarantineSize = size;
    }

    /**
     * Convienence method (needed for Velocity since "getAttachmentCount()>0" seems to have bugs)
     * @return true if has attachments, false if not.
     */
    public boolean hasAttachments()
    {
        return getAttachmentCount() > 0;
    }

    /**
     * Return sender address truncated by length.
     * @return Truncated sender address.
     */
    public String getTruncatedSender()
    {
        return truncate(sender, SENDER_MAX_LENGTH);
    }

    /**
     * Return subject truncated by length.
     * @return Truncated subject.
     */
    public String getTruncatedSubject()
    {
        return truncate(subject, SUBJECT_MAX_LENGTH);
    }

    /**
     * Return quarantine detail.
     * @return String of quarantine detail.
     */
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

    /**
     * Write quarantine detail.
     * @param detail String of quarantine detail.
     */
    public void setFormattedQuarantineDetail(String detail)
    {
        this.quarantineDetail = detail;
    }

    /**
     * Elide string at length such as "maximu...""
     * @param  source    Source string
     * @param  maxLength Maximum length.
     * @return           String of truncated source.
     */
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

    /**
     * Return quarantine as string.
     * @return Quarantine as string.
     */
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

    /**
     * Run tests.
     * @param  args      List of tests to run.
     * @return           String of test results.
     * @throws Exception On error.
     */
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

/*
 * $HeadURL: svn://chef/work/src/smtp-casing/src/com/untangle/node/smtp/quarantine/MailSummary.java $
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.smtp.quarantine;

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

        InputStream in = new FileInputStream(args[0]);

        // InputSource input = new org.xml.sax.InputSource(in);

        String result = "";
        try {
            while (true) {
                Document document = builder.parse(in);
                result += "Got a document\n";
            }
        } catch (Exception ex) {
            result += "Got exception\n";
            ex.printStackTrace();
        }

        result += "MailSummary";
        return result;
    }
}

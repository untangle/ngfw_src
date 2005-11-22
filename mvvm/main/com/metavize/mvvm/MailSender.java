/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm;

import java.util.List;
import java.io.File;
import java.io.InputStream;
import javax.mail.internet.MimeBodyPart;


public interface MailSender
{
    /**
     * <code>sendAlert</code> sends an email to all administrative
     * users who have selected to receive alert emails.
     *
     * @param subject a <code>String</code> value
     * @param bodyText a <code>String</code> value
     */
    void sendAlert(String subject, String bodyText);

    /**
     * <code>sendReports</code> sends a normal report email (with attachments)
     * to the reportEmail address (from the mail settings).  This is a convenience
     * function that just calls sendMessageWithAttachments
     *
     * @param subject a <code>String</code> value
     * @param bodyHTML a <code>String</code> containing the HTML for the "main page" that will become the first extra
     * @param extraLocations a <code>List<String></code> containing URI for the corresponding extra.  This could be part of the trailing path, or some arbitrary string unrelated to the file name.
     * @param extras a <code>List<File></code> containing the additional extras (#S1, #S2, ...).  They will have a MIME filename that matches the File's name
     */
    void sendReports(String subject, String bodyHTML, List<String> extraLocations, List<File> extras);

    /**
     * <code>sendErrorLogs</code> sends an error log email to Metavize.  Each attachment contains
     * the latest events for a particular transform.
     * to the reportEmail address (from the mail settings).
     *
     * @param subject a <code>String</code> giving the subject.
     * @param bodyText a <code>String</code> containing the plain text for the "main page" that will become the first extra
     * @param parts a <code>List<MimeBodyPart></code> containing the additional parts.
     */
    void sendErrorLogs(String subject, String bodyText, List<MimeBodyPart> parts);


    /****************************************************************************
     * BASIC (low-level) METHODS FOLLOW
     ***************************************************************************/

    /**
     * Sends an already formatted RFC822 (i.e. MIME) message.  The TO, FROM, etc
     * are lifted from the MIME (no chance for "bcc").
     * <br><br>
     * With or without a positive outcome, the
     * passed-in stream is <b>not</b> closed
     * <br><br>
     * All headers (date, to, from) should already
     * be set.  The only header added is "X-Mailer".
     *
     * @param msgStream the stream containing the message, positioned
     *        just before the headers and <b>not</b> byte stuffed (for SMTP).
     *
     * @return true if sent, false if an error.
     */
    boolean sendMessage(InputStream msgStream);


    /**
     * Sends an already formatted RFC822 (i.e. MIME) message.  The FROM
     * for SMTP transport is lifted from the MIME message, but the
     * recipients are specified.
     * <br><br>
     * With or without a positive outcome, the
     * passed-in stream is <b>not</b> closed
     * <br><br>
     * All headers (date, to, from) should already
     * be set.  The only header added is "X-Mailer".
     *
     * @param msgStream the stream containing the message, positioned
     *        just before the headers and <b>not</b> byte stuffed (for SMTP).
     * @param rcptStrs the recipients
     *
     * @return true if sent, false if an error.
     */
    boolean sendMessage(InputStream msgStream, String...rcptStrs);

    /**
     * <code>sendMessageWithAttachments</code> sends a email (with attachments)
     * to the given recipients.
     *
     * Each extra will have a MIME filename that matches the File's name, and a
     * Content-Location given by the extraLocation.
     *
     * The two lists must be the same length or a <code>IllegalArgumentException</code> is thrown.
     *
     * @param recipients a <code>String[]</code> of recipient email addresses
     * @param subject a <code>String</code> value
     * @param bodyHTML a <code>String</code> containing the HTML for the "main page" that will become the first extra
     * @param extraLocations a <code>List<String></code> containing URI for the corresponding extra.  This could be part of the trailing path, or some arbitrary string unrelated to the file name.
     * @param extras a <code>List<File></code> containing the additional extras (#S1, #S2, ...).  They will have a MIME filename that matches the File's name
     */
    void sendMessageWithAttachments(String[] recipients, String subject, String bodyHTML, List<String> extraLocations, List<File> extras);

    /**
     * <code>sendMessage</code> sends an email message to the given
     * recipients.
     *
     * @param recipients a <code>String[]</code> giving the recipient
     * email addresss
     * @param subject a <code>String</code> value
     * @param bodyText a <code>String</code> value
     */
    void sendMessage(String[] recipients, String subject, String bodyText);

    void setMailSettings(MailSettings settings);

    MailSettings getMailSettings();


    /****************************************************************************
     * DEPRECATED METHODS FOLLOW
     ***************************************************************************/

    /**
     * <code>sendAlertWithAttachment</code> sends an email with a
     * single attachment to all administrative users who have selected
     * to receive alert emails.  The attachment is provided as a List
     * of ByteBuffers, one buffer per line.
     *
     * @param subject a <code>String</code> value
     * @param bodyText a <code>String</code> value
     * @param attachment a <code>List</code> of
     * <code>ByteBuffer</code>s each containing a single line of the
     * attachment
     */
    void sendAlertWithAttachment(String subject, String bodyText,
                                 List attachment);

    /**
     * <code>sendMessageWithAttachment</code> sends an email message
     * with an optional attachment to the given recipients.  The
     * attachment is provided as a List of ByteBuffers, one buffer per
     * line.
     *
     * @param recipients a <code>String[]</code> giving the recipient
     * email addresss
     * @param subject a <code>String</code> value
     * @param bodyText a <code>String</code> value
     * @param attachment a <code>List</code> of
     * <code>ByteBuffer</code>s each containing a single line of the
     * attachment
     */
    void sendMessageWithAttachment(String[] recipients, String subject,
                                   String bodyText, List attachment);


    /**
     * <code>sendReport</code> sends a simple report email (with no
     * attachments) to the reportEmail address (from the mail settings).
     *
     * @param subject a <code>String</code> value
     * @param bodyText a <code>String</code> containing the simple text or HTML 
     */
    void sendReport(String subject, String bodyText);


}

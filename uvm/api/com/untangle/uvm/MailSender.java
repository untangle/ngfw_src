/*
 * $HeadURL$
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

package com.untangle.uvm;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import javax.mail.internet.MimeBodyPart;

/**
 * Interface for sending email.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public interface MailSender
{
    public static final String DEFAULT_SENDER = "untangle";

    /**
     * Returns true if automatic configuration of the underlying MTA (currently Exim4)
     * is enabled.  If it is not enabled, then the corresponding MailSettings
     * (smtpHost, authUser, etc) are ignored.
     *
     * @return a <code>boolean</code> true if auto configuration of the local MTA is enabled
     */
    boolean isAutoConfigEnabled();

    /**
     * Set the mail settings.
     *
     * @param settings the new mail settings.
     */
    void setMailSettings(MailSettings settings);

    /**
     * Get the mail settings.
     *
     * @return the current mail settings.
     */
    MailSettings getMailSettings();

    /**
     * Sends an email to all administrative users who have selected to
     * receive alert emails.
     *
     * @param subject subject of the mail.
     * @param bodyText text of the mail.
     */
    void sendAlert(String subject, String bodyText);

    /**
     * Sends a normal report email (with attachments) to the
     * reportEmail address (from the mail settings).  This is a
     * convenience function that just calls
     * sendMessageWithAttachments.
     *
     * @param subject subject of the email.
     * @param bodyHTML HTML for the "main page" that will become the
     * first extra.
     * @param extraLocations URI for the corresponding extra.  This
     * could be part of the trailing path, or some arbitrary string
     * unrelated to the file name.
     * @param extras additional extras with a MIME filename that
     * matches the File's name
     */
    void sendReports(String subject, String bodyHTML,
                     List<String> extraLocations, List<File> extras);


    /**
     * Sends an error log email to Untangle to the reportEmail address
     * (from the mail settings).  Each attachment contains the last
     * events a node.
     *
     * @param subject the subject.
     * @param bodyText the plain text for the "main page" that will
     * become the first extra.
     * @param parts the additional parts.
     */
    void sendErrorLogs(String subject, String bodyText,
                       List<MimeBodyPart> parts);

    /**************************************************************************
     * BASIC (low-level) METHODS FOLLOW
     *************************************************************************/

    /**
     * Sends an email message to the given recipients.
     *
     * @param recipients recipient email addresses.
     * @param subject subject of the message.
     * @param bodyText text of the message.
     */
    void sendMessage(String[] recipients, String subject, String bodyText);

    /**
     * Sends an email message to the given recipients.
     *
     * @param recipients recipient email addresses.
     * @param subject subject of the message.
     * @param bodyHtml body of the message.
     * 
     * @return true if sent, false if an error.
     */
    boolean sendHtmlMessage(String[] recipients, String subject, String bodyHtml);
    
    /**
     * Sends a pre-formatted RFC822 (i.e. MIME) message.  The
     * <code>To</code>, <code>From</code>, etc... headers are lifted
     * from the MIME message.  All headers (<code>Date</code>,
     * <code>To</code>, <code>From</code>) should be set in the MIME
     * message.  The only header added is <code>X-Mailer</code>.
     *
     * The InputStream stream is <b>not</b> closed regardless of send
     * status.
     *
     * @param msgStream the stream containing the message, positioned
     * just before the headers and <b>not</b> byte stuffed (for SMTP).
     * @return true if sent, false if an error.
     */
    boolean sendMessage(InputStream msgStream);

    /**
     * Sends a pre-formatted RFC822 (i.e. MIME) message.  The
     * <code>To</code>, <code>From</code>, etc... headers are lifted
     * from the MIME message.  All headers (<code>Date</code>,
     * <code>To</code>, <code>From</code>) should be set in the MIME
     * message.  The only header added is <code>X-Mailer</code>. The
     * InputStream stream is <b>not</b> closed regardless of send
     * status.
     *
     * @param msgStream the stream containing the message, positioned
     * just before the headers and <b>not</b> byte stuffed (for SMTP).
     * @param rcptStrs the recipients.
     *
     * @return true if sent, false if an error.
     */
    boolean sendMessage(InputStream msgStream, String...rcptStrs);

    /**
     * Sends a email (with attachments) to the given recipients.
     *
     * Each extra must have a MIME filename that matches the File's
     * name and Content-Location given by the extraLocation.
     *
     * The two lists must be the same length or a
     * <code>IllegalArgumentException</code> is thrown.
     *
     * @param recipients recipient email addresses.
     * @param subject subject of mail message.
     * @param bodyHTML the HTML for the "main page" that will become
     * the first extra.
     * @param extraLocations the URI for the corresponding extra.
     * This could be part of the trailing path, or some arbitrary
     * string unrelated to the file name.
     * @param extras the additional extras having have a MIME filename
     * that matches the File's name.
     */
    void sendMessageWithAttachments(String[] recipients, String subject,
                                    String bodyHTML,
                                    List<String> extraLocations,
                                    List<File> extras);

    /**
     * Used by the UI to test saved MailSettings.  A static test email
     * is sent to the given address.
     *
     * Note that a true return doesn't guarantee that the email will
     * be received, it could be dropped somewhere along the way. A
     * false return guarantees that the settings are wrong.
     *
     * @param recipient recipient's email address for the test message.
     * @return false if an exception occurs while sending, otherwise true.
     */
    boolean sendTestMessage(String recipient);
}

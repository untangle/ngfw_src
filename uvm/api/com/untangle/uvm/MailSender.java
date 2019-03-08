/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Interface for sending email.
 */
public interface MailSender
{
    /**
     * Set the mail settings.
     *
     * @param settings the new mail settings.
     */
    void setSettings(MailSettings settings);

    /**
     * Get the mail settings.
     *
     * @return the current mail settings.
     */
    MailSettings getSettings();

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
     * Sends a multi-part MIME email message to the given recipients.
     *
     * @param recipients recipient email addresses.
     * @param subject subject of the message.
     * @param parts. Text, HTML, and attachments for the message assembled as:
     * + mixed
     *      + alternative
     *           - text
     *           - html
     *       - attachment 1
     *       - attachment ...
     *
     * When HTML is constructed with images referencing attachments as content-ids (CID),
     * images appear correctly in message for common email clients (Gmail, Outlook)
     *
     * @return true if sent, false if an error.
     */
    public enum MessagePartsField {
        TEXT,
        HTML,
        FILENAME,
        CID
    }

    void sendMessage(String[] recipients, String subject, List<Map<MessagePartsField,String>> parts);

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
    void sendMessageWithAttachments(String[] recipients, String subject, String bodyHTML, List<String> extraLocations, List<File> extras);

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
    String sendTestMessage(String recipient);
}

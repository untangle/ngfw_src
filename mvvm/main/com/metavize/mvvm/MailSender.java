/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MailSender.java,v 1.3 2004/12/20 08:24:20 amread Exp $
 */

package com.metavize.mvvm;

import java.util.List;


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
     * <code>sendReport</code> sends an email to the reportEmail
     * address (from the mail settings).
     *
     * @param subject a <code>String</code> value
     * @param bodyText a <code>String</code> value
     */
    void sendReport(String subject, String bodyText);

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

    void setMailSettings(MailSettings settings);

    MailSettings getMailSettings();
}

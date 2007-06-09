/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.node.mail.papi.smtp;

import java.io.*;
import java.nio.*;
import java.util.*;

import com.untangle.uvm.MailSender;
import com.untangle.uvm.node.TemplateValues;
import com.untangle.uvm.node.TemplateValuesChain;
import com.untangle.node.mail.papi.*;
import com.untangle.node.mime.*;
import com.untangle.node.util.*;
import org.apache.log4j.Logger;

/**
 * Subclass of MessageGenerator which understands
 * the SMTPNotifyAction and creates a MIME Message
 * accordingly (or not, of the SMTPNotifyAction
 * is "NEITHER").
 *
 */
public class SmtpNotifyMessageGenerator
    extends MessageGenerator {

    private final Logger m_logger =
        Logger.getLogger(SmtpNotifyMessageGenerator.class);

    private boolean m_attachCause;

    public SmtpNotifyMessageGenerator() {
        this(null, null, false);
    }

    /**
     * See superclass docs
     */
    public SmtpNotifyMessageGenerator(String subjectTemplate,
                                      String bodyTemplate,
                                      boolean attachCause) {

        super(subjectTemplate, bodyTemplate);

        m_attachCause = attachCause;
    }

    public boolean isAttachCause() {
        return m_attachCause;
    }

    /**
     * If true, the message which caused the notification
     * will be attached.
     */
    public void setAttachCause(boolean attachCause) {
        m_attachCause = attachCause;
    }

    /**
     * Send a Notification (if the <code>action</code>
     * determines we should).
     * <br><br>
     * Returns false if an error occured (only really
     * for logging.  There is nothing the caller can do).
     * <br><br>
     * The transaction is <b>not</b> implicitly added
     * to the <code>substitutionSources</code>
     *
     * @param sender the MailSender
     * @param action the action to take
     * @param cause the message which, when inspected, caused this notification
     * @param tx the current transaction (which had <code>cause</code> as its
     *        message data).
     * @param substitutionSources Any TemplateValues used for substitution values
     */
    public boolean sendNotification(MailSender sender,
                                    SMTPNotifyAction action,
                                    MIMEMessage cause,
                                    SmtpTransaction tx,
                                    TemplateValues... substitutionSources) {
        return sendNotification(sender,
                                action,
                                cause,
                                tx,
                                new TemplateValuesChain(substitutionSources));
    }

    /**
     * Send a Notification (if the <code>action</code>
     * determines we should).
     * <br><br>
     * Returns false if an error occured (only really
     * for logging.  There is nothing the caller can do).
     * <br><br>
     * The transaction is <b>not</b> implicitly added
     * to the <code>substitutionSources</code>
     *
     * @param sender the MailSender
     * @param action the action to take
     * @param cause the message which, when inspected, caused this notification
     * @param tx the current transaction (which had <code>cause</code> as its
     *        message data).
     * @param substitutionSources TEmplate values for substitution
     */
    public boolean sendNotification(MailSender sender,
                                    SMTPNotifyAction action,
                                    MIMEMessage cause,
                                    SmtpTransaction tx,
                                    TemplateValuesChain substitutionSources) {

        //TODO bscott Use a "set" instead of a list, to prevent dupes
        //     if sender was also a recipient

        if(action == SMTPNotifyAction.NEITHER || action == null) {
            return true;
        }
        List<EmailAddress> recipients = new ArrayList<EmailAddress>();

        if(
           action == SMTPNotifyAction.SENDER) {
            if(tx.getFrom() != null) {
                recipients.add(tx.getFrom());
            }
        }
        else if(action == SMTPNotifyAction.RECEIVER) {
            for(EmailAddress addr : tx.getRecipients(true)) {
                recipients.add(addr);
            }
        }
        else {//BOTH
            for(EmailAddress addr : tx.getRecipients(true)) {
                recipients.add(addr);
            }
            if(tx.getFrom() != null) {
                recipients.add(tx.getFrom());
            }
        }

        if(recipients.size() == 0) {
            m_logger.debug("No recipients for notification email.  Do not send");
            return false;
        }

        //
        String fromAsString = "postmaster@local.domain";
        if(sender.getMailSettings() != null &&
           sender.getMailSettings().getFromAddress() != null) {
            fromAsString = sender.getMailSettings().getFromAddress();
        }
        EmailAddress from = null;
        try {
            from = new EmailAddress(fromAsString);
        }
        catch(Exception ex) {
            m_logger.error("Error parsing address \"" + fromAsString + "\"", ex);
            return false;
        }

        //Create the message
        MIMEMessage msg = createMessage(recipients,
                                        from,
                                        cause,
                                        isAttachCause(),
                                        substitutionSources);

        if(msg == null) {
            m_logger.error("Unable to create notification message");
            return false;
        }

        //Convert message to a Stream
        InputStream in = null;
        try {
            ByteBuffer buf = msg.toByteBuffer();
            in = new ByteBufferInputStream(buf);
        }
        catch(Exception ex) {
            m_logger.error("Exception converting MIMEMessage to a byte[]", ex);
            return false;
        }

        //Attempt the send
        boolean ret = sender.sendMessage(in);

        try {in.close();}catch(Exception ignore){}

        return ret;
    }


}

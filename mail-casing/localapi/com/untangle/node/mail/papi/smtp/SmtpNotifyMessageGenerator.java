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

package com.untangle.node.mail.papi.smtp;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.MessageGenerator;
import com.untangle.node.mime.EmailAddress;
import com.untangle.node.mime.MIMEMessage;
import com.untangle.node.util.ByteBufferInputStream;
import com.untangle.uvm.MailSender;
import com.untangle.uvm.node.TemplateValues;
import com.untangle.uvm.node.TemplateValuesChain;

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

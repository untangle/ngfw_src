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

package com.untangle.node.smtp;

import static com.untangle.node.smtp.mime.HeaderNames.CONTENT_TRANSFER_ENCODING;
import static com.untangle.node.smtp.mime.HeaderNames.CONTENT_TYPE;
import static com.untangle.node.smtp.mime.HeaderNames.DATE;
import static com.untangle.node.smtp.mime.HeaderNames.MIME_VERSION;
import static com.untangle.node.util.Ascii.CRLF_BA;

import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.mime.AttachedMIMEMessage;
import com.untangle.node.smtp.mime.ByteArrayMIMESource;
import com.untangle.node.smtp.mime.ContentTypeHeaderField;
import com.untangle.node.smtp.mime.ContentXFerEncodingHeaderField;
import com.untangle.node.smtp.mime.EmailAddress;
import com.untangle.node.smtp.mime.MIMEMessage;
import com.untangle.node.smtp.mime.MIMEMessageHeaders;
import com.untangle.node.smtp.mime.MIMEPart;
import com.untangle.node.smtp.mime.MIMESourceRecord;
import com.untangle.node.smtp.mime.MIMEUtil;
import com.untangle.node.smtp.mime.RcptType;
import com.untangle.uvm.node.Template;
import com.untangle.uvm.node.TemplateValues;
import com.untangle.uvm.node.TemplateValuesChain;

/**
 * Class which assists in creating notification messages,
 * by encapsulating some templating as well as optionaly
 * attaching a "cause" message to the net-new message.  Takes
 * care of details like "Date" and "MIME" headers.
 * <br><br>
 * This class uses the {@link #com.untangle.node.util.Template Template}
 * class internally.  You can create templates which derefference
 * keys found in any number of TemplateValues, passed-into the
 * {@link #wrap wrap method}
 * <br><br>
 * Note that the {@link #getBody Body}
 * and {@link #getSubject Subject}
 * templates shouldn't be null, but this is
 * not prevented.  If either is null, blanks ("")
 * will be substituted.
 *
 */
public class MessageGenerator {

    private final Logger m_logger = Logger.getLogger(MessageGenerator.class);

    private Template m_subjectTemplate;
    private Template m_bodyTemplate;


    public MessageGenerator() {
        this(null, null);
    }

    /**
     * Full constructor.
     *
     * @param subjectTemplate the subject template
     * @param bodyTemplate the bodyTemplate
     */
    public MessageGenerator(String subjectTemplate,
                            String bodyTemplate) {
        setSubject(subjectTemplate);
        setBody(bodyTemplate);
    }


    public String getSubject() {
        return m_subjectTemplate==null?
            null:m_subjectTemplate.getTemplate();
    }

    /**
     * Set the subject template.
     *
     * @param template the template (or null to declare
     *        that subjects should not be modified).
     */
    public void setSubject(String template) {
        if(template == null) {
            m_subjectTemplate = null;
        }
        if(m_subjectTemplate == null) {
            m_subjectTemplate = new Template(template, false);
        }
        else {
            m_subjectTemplate.setTemplate(template);
        }
    }

    public String getBody() {
        return m_bodyTemplate==null?
            null:m_bodyTemplate.getTemplate();
    }


    /**
     * Set the body template.
     *
     * @param template the template (or null to declare
     *        that body should not be wrapped).
     */
    public void setBody(String template) {
        if(template == null) {
            m_bodyTemplate = null;
        }
        if(m_bodyTemplate == null) {
            m_bodyTemplate = new Template(template, false);
        }
        else {
            m_bodyTemplate.setTemplate(template);
        }
    }


    /**
     * For subclasses to access the internal body
     * Template Object (or null if not set).
     */
    protected Template getBodyTemplate() {
        return m_bodyTemplate;
    }

    /**
     * For subclasses to access the internal subject
     * Template Object (or null if not set).
     */
    protected Template getSubjectTemplate() {
        return m_subjectTemplate;
    }

    public MIMEMessage createMessage(List<EmailAddress> recipients,
                                     EmailAddress from,
                                     MIMEMessage cause,
                                     boolean attachCause) {
        return createMessage(recipients,
                             from,
                             cause,
                             attachCause,
                             new TemplateValuesChain());
    }


    public MIMEMessage createMessage(List<EmailAddress> recipients,
                                     EmailAddress from,
                                     MIMEMessage cause,
                                     boolean attachCause,
                                     TemplateValues... values) {
        return createMessage(recipients,
                             from,
                             cause,
                             attachCause,
                             new TemplateValuesChain(values));
    }

    /**
     * Creates a net-new MIME message, to the given recipients
     * from the given sender.  The subject and body are set from
     * the template members of this class, and may be a function
     * of the <code>cause</code> message and/or any other
     * TemplateValues passed-in.
     * <br><br>
     * Kind-of lame, but returns null if there was
     * an error (rather than throwing the exception).
     * <br><br>
     * The <code>cause</code> message is implicitly
     * added to the values.
     *
     * @param recipients the recipients for the mail
     * @param from the sender of the mail
     * @param cause the mail which caused this mail to be generated
     * @param attachCause if true, the "cause" message will be attached.
     * @param values any sources of substitution parameters
     */
    public MIMEMessage createMessage(List<EmailAddress> recipients,
                                     EmailAddress from,
                                     MIMEMessage cause,
                                     boolean attachCause,
                                     TemplateValuesChain values) {
        try {
            if(attachCause) {
                return createMessageWithCause(recipients, from, cause, values);
            }
            else {
                return createMessageWithoutCause(recipients, from, cause, values);
            }
        }
        catch(Exception ex) {
            m_logger.error("Error creating message", ex);
            return null;
        }
    }

    private MIMEMessage createMessageWithCause(List<EmailAddress> recipients,
                                               EmailAddress from,
                                               MIMEMessage cause,
                                               TemplateValuesChain values)
        throws Exception {

        //Add the original message to the chain
        values.append(cause);


        MIMEMessageHeaders headers = new MIMEMessageHeaders();


        //Take care of boiler-plate headers
        headers.addHeaderField(DATE, MIMEUtil.getRFC822Date());
        headers.addHeaderField(MIME_VERSION, "1.0");

        //Add content-centric headers
        headers.addHeaderField(CONTENT_TYPE,
                               ContentTypeHeaderField.MULTIPART_MIXED +
                               "; boundary=\"" + MIMEUtil.makeBoundary() + "\"");

        headers.addHeaderField(CONTENT_TRANSFER_ENCODING,
                               ContentXFerEncodingHeaderField.SEVEN_BIT_STR);

        //Sender/Recipient
        for(EmailAddress rcpt : recipients) {
            headers.addRecipient(rcpt, RcptType.TO);
        }
        if(from != null) {
            headers.setFrom(from);
        }

        //Subject
        Template subjectTemplate = getSubjectTemplate();
        if(subjectTemplate != null) {
            headers.setSubject(m_subjectTemplate.format(values));
        }


        //Create the MIME message, initialized on these headers
        MIMEMessage ret = new MIMEMessage(headers);


        //Create the (text) Body part
        MIMEPart bodyPart = new MIMEPart();
        bodyPart.getMPHeaders().addHeaderField(CONTENT_TYPE,
                                               ContentTypeHeaderField.TEXT_PLAIN);
        bodyPart.getMPHeaders().addHeaderField(CONTENT_TRANSFER_ENCODING,
                                               ContentXFerEncodingHeaderField.SEVEN_BIT_STR);
        byte[] bytes = getBodyBytes(values);
        bodyPart.setContent(
                            new MIMESourceRecord(new ByteArrayMIMESource(bytes),
                                                 0,
                                                 bytes.length,
                                                 false));

        //Add the new body to the returned message
        ret.addChild(bodyPart);

        //Add the wrapped other message
        ret.addChild(new AttachedMIMEMessage(cause));

        return ret;
    }

    private MIMEMessage createMessageWithoutCause(List<EmailAddress> recipients,
                                                  EmailAddress from,
                                                  MIMEMessage cause,
                                                  TemplateValuesChain values)
        throws Exception {

        //Add the original message to the chain
        values.append(cause);


        MIMEMessageHeaders headers = new MIMEMessageHeaders();


        //Take care of boiler-plate headers
        headers.addHeaderField(DATE, MIMEUtil.getRFC822Date());
        headers.addHeaderField(MIME_VERSION, "1.0");

        //Add content-centric headers
        headers.addHeaderField(CONTENT_TYPE,
                               ContentTypeHeaderField.TEXT_PLAIN);
        headers.addHeaderField(CONTENT_TRANSFER_ENCODING,
                               ContentXFerEncodingHeaderField.SEVEN_BIT_STR);

        //Sender/Recipient
        for(EmailAddress rcpt : recipients) {
            headers.addRecipient(rcpt, RcptType.TO);
        }
        headers.setFrom(from);

        //Subject
        Template subjectTemplate = getSubjectTemplate();
        if(subjectTemplate != null) {
            headers.setSubject(m_subjectTemplate.format(values));
        }

        //Create the MIME message, initialized on these headers
        MIMEMessage ret = new MIMEMessage(headers);

        //Set the content
        byte[] bytes = getBodyBytes(values);
        ret.setContent(
                       new MIMESourceRecord(new ByteArrayMIMESource(bytes),
                                            0,
                                            bytes.length,
                                            false));
        return ret;
    }



    /**
     * Helper which gets the formatted
     * plain-text body, or "" if there
     * isn't a template
     */
    private byte[] getBodyBytes(TemplateValuesChain keys) {
        Template bodyTemplate = getBodyTemplate();
        if(bodyTemplate == null) {
            return CRLF_BA;
        }
        return bodyTemplate.format(keys).getBytes();
    }
}

/**
 * $Id$
 */
package com.untangle.app.smtp.mime;

import static com.untangle.uvm.util.Ascii.CRLF;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import com.untangle.app.smtp.TemplateValues;

/**
 * Class representing a MIMEMessage. Adds the strongly-typed {@link #getMMHeaders MIMEMessageHeaders} with convienence
 * members for a top-level message (such as recipient and subject manipulation). <br>
 * <br>
 * This class also implements {@link com.untangle.uvm.util.TemplateValues TemplateValues}. The variable syntax for
 * accessing elements of the MIMEMessage is based on <code>MIMEMessage:&lt;name></code> where <code>name</code> can be
 * any one of the following:
 * <ul>
 * <li>
 * <code><b>TO</b></code> The "TO" addresses, as found in the MIME Headers Each recipient is on its own line. Even if
 * there is only one recipient, this variable will be substituted with the value of the recipient <i>followed by a
 * CRLF.</i> If there are no "TO" values on the header, than this will simply return a blank String ("").</li>
 * <li>
 * <code><b>CC</b></code> Any CC recipients, following the same rules for multiple values and new lines as
 * <code>TO</code></li>
 * <li>
 * <code><b>RECIPIENTS</b></code> Combination of <code>TO</code> and <code>CC</code></li>
 * <li>
 * <code><b>FROM</b></code> The "FROM" as found in the headers. If there is no FROM value, the literal "&lt;>" will be
 * substituted.</li>
 * <li>
 * <code><b>SUBJECT</b></code> The Subject, as found on the message. If there is no subject (or it is null), a blank
 * string ("") will be returned.</li>
 * <!--
 * <li>
 * <code><b>XXXXX</b></code></li>
 * -->
 * </ul>
 * Note also that any variables from the embedded Headers will also be evaluated (see the docs on
 * {@link com.untangle.app.smtp.mime.Headers Headers} for a list of possible variables).
 */
public class MIMEMessageTemplateValues implements TemplateValues
{
    private static final String MIME_MESSAGE_TEMPLATE_PREFIX = "MIMEMessage:".toLowerCase();
    public static String MIME_HEADER_VAR_PREFIX = "MIMEHeader:".toLowerCase();
    private static final String TO_TV = "TO".toLowerCase();
    private static final String CC_TV = "CC".toLowerCase();
    private static final String RECIP_TV = "RECIPIENTS".toLowerCase();
    private static final String FROM_TV = "FROM".toLowerCase();
    private static final String SUBJECT_TV = "SUBJECT".toLowerCase();

    private MimeMessage msg;

    /**
     * Initialize instance of MIMEMessageTemplateValues.
     * @param  msg MimeMessage to initialize with.
     * @return     Instance of MIMEMessageTemplateValues.
     */
    public MIMEMessageTemplateValues(MimeMessage msg) {
        this.msg = msg;
    }

    /**
     * For use in a Template
     * @param key String to search.
     * @return Template value string.
     */
    public String getTemplateValue(String key)
    {
        try {
            // First, see if this key is for the child Headers
            String headerRet = getHeaderTemplateValue(key);
            if (headerRet != null) {
                return headerRet;
            }

            // Not for the headers. Evaluate if this is a MIMEMessage variable
            key = key.trim().toLowerCase();
            if (key.startsWith(MIME_MESSAGE_TEMPLATE_PREFIX)) {
                key = key.substring(MIME_MESSAGE_TEMPLATE_PREFIX.length());
                if (key.equals(TO_TV)) {
                    Address[] tos = msg.getRecipients(RecipientType.TO);
                    StringBuilder sb = new StringBuilder();
                    for (Address addr : tos) {
                        sb.append(addr.toString());
                        sb.append(CRLF);
                    }
                    return sb.toString();
                } else if (key.equals(CC_TV)) {
                    Address[] ccs = msg.getRecipients(RecipientType.CC);
                    StringBuilder sb = new StringBuilder();
                    for (Address addr : ccs) {
                        sb.append(addr.toString());
                        sb.append(CRLF);
                    }
                    return sb.toString();
                } else if (key.equals(RECIP_TV)) {
                    Address[] allRcpts = msg.getAllRecipients();
                    StringBuilder sb = new StringBuilder();
                    for (Address eawrt : allRcpts) {
                        sb.append(eawrt.toString());
                        sb.append(CRLF);
                    }
                    return sb.toString();
                } else if (key.equals(FROM_TV)) {
                    Address[] from = msg.getFrom();
                    if (from == null || from.length == 0) {
                        return "<>";
                    }
                    return from[0].toString();
                } else if (key.equals(SUBJECT_TV)) {
                    return msg.getSubject() == null ? "" : msg.getSubject();
                }
            }
        } catch (MessagingException e) {
            // return null;
        }
        return null;
    }

    /**
     * For use in Templates (see JavaDoc at the top of this class for explanation of vairable format}.
     * @param key String to search.
     * @return Header template value string.
     * @throws MessagingException If error.
     */
    private String getHeaderTemplateValue(String key) throws MessagingException
    {
        if (key.toLowerCase().startsWith(MIME_HEADER_VAR_PREFIX)) {
            String headerName = key.substring(MIME_HEADER_VAR_PREFIX.length());
            String[] header = msg.getHeader(headerName);
            if (header == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String s : header) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(s);
            }
            return sb.toString();
        }
        return null;
    }

}

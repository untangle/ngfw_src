/**
 * $Id$
 */
package com.untangle.app.smtp.mime;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;

import com.untangle.app.smtp.CommandWithEmailAddress;

/**
 * Utility methods for working with MIME
 */
public class MIMEUtil
{
    public static final String MAIL_FORMAT_STR = "EEE, d MMM yyyy HH:mm:ss Z";

    private static final Logger logger = Logger.getLogger(MIMEUtil.class);

    public static final byte[] MIME_SPECIALS = { (byte) '(', (byte) ')', (byte) '<', (byte) '>', (byte) '@',
            (byte) ',', (byte) ';', (byte) ':', (byte) '\\', (byte) '"', (byte) '/', (byte) '[', (byte) ']',
            (byte) '?', (byte) '=' };

    /**
     * Return a list of all parts of a messages
     * @param msg MimeMessage to process.
     * @return List of Part.
     */
    public static List<Part> getParts(MimeMessage msg)
    {
        List<Part> list = new ArrayList<Part>();
        try {
            getParts(list, msg);
        } catch (MessagingException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        }
        return list;
    }

    /**
     * Get parts from content.
     * @param  list               List of Part
     * @param  part               Part to process.
     * @throws IOException        If unable to process.
     * @throws MessagingException If message exception.
     */
    private static void getParts(List<Part> list, Part part) throws IOException, MessagingException
    {
        Object msgContent = part.getContent();
        if (msgContent instanceof Multipart) {
            Multipart multipart = (Multipart) msgContent;

            for (int j = 0; j < multipart.getCount(); j++) {
                BodyPart bodyPart = multipart.getBodyPart(j);
                getParts(list, bodyPart);
            }
        } else {
            list.add(part);
        }
    }

    /**
     * Removes the child from its parent. Unlike the method with a similar name on MIMEPart itself, this method fixes-up
     * any parent container issues (for example, the parent is "multipart" yet the removal causes the parent to have no
     * children). <br>
     * <br>
     * If the child has no parent, then we assume that the child is a top-level MIMEMessage. In that case, we take
     * different action. We assume that the intent is to remove some "nasty" content, so we preserve the headers (except
     * for the "Content-XXX") stuff and replace the body with blank text. This is done via
     * {@link #convertToEmptyTextPart convertToEmptyTextPart()}.
     * 
     * @param child
     *            the child to be removed from its parent.
     * @throws MessagingException
     * @throws IOException
     */
    public static void removeChild(Part child) throws IOException, MessagingException
    {
        // Boundary-case. If the parent is itself a top-level MIMEMessage, and there are no other
        // children. This really means "nuke my content"
        if (!(child instanceof BodyPart)) {
            convertToEmptyTextPart(child);
            return;
        }
        Multipart parentContent = ((BodyPart) child).getParent();
        if (parentContent == null) {
            convertToEmptyTextPart(child);
            return;
        }

        parentContent.removeBodyPart((BodyPart) child);

        if (parentContent.getCount() == 0) {
            // If we just created an empty multipart, go up to the parent-parent and remove
            removeChild(parentContent.getParent());
        }
    }

    /**
     * Re-set the content for each multipart, so that the content will be cached; If read from input stream, the content
     * is not cached and the subsequent changes to it are not saved
     * 
     * @param part
     */
    public static void setContentForPart(Part part)
    {
        try {
            Object msgContent = part.getContent();
            if (msgContent instanceof Multipart) {
                Multipart multipart = (Multipart) msgContent;
                part.setContent(multipart);
                for (int j = 0; j < multipart.getCount(); j++) {
                    BodyPart bodyPart = multipart.getBodyPart(j);
                    setContentForPart(bodyPart);
                }

            }
        } catch (MessagingException e) {
            Logger.getLogger(MIMEUtil.class).error(e);
        } catch (IOException e) {
            Logger.getLogger(MIMEUtil.class).error(e);
        }
    }

    /**
     * Changes the part into an empty "text/plain" part, discarding any previous content
     *
     * @param part Part to process.
     * @throws MessagingException
     */
    public static void convertToEmptyTextPart(Part part) throws MessagingException
    {
        part.removeHeader(HeaderNames.CONTENT_TYPE);
        part.removeHeader(HeaderNames.CONTENT_TRANSFER_ENCODING);
        part.removeHeader(HeaderNames.CONTENT_DISPOSITION);

        part.addHeader(HeaderNames.CONTENT_TYPE, HeaderNames.TEXT_PLAIN);
        part.addHeader(HeaderNames.CONTENT_TRANSFER_ENCODING, HeaderNames.SEVEN_BIT_STR);

        part.setContent("", HeaderNames.TEXT_PLAIN);
    }

    /**
     * Wraps the given MIMEMessage in a new MIMEMessage with the old as an RFC822 attachment, with the new plaintext
     * (not multipart-alt) body. (Since the wrapped MIMEMessage references the given MIMEMessage, do not dispose the
     * given MIMEMessage.)
     * 
     * @param plainBodyContent
     *            the new body content (should be line-formatted such that lines are not longer than 76 chars).
     * @param oldMsg
     *            the old message
     * @return MimeMessage.
     * @throws Exception on error.
     */
    public static MimeMessage simpleWrap(String plainBodyContent, MimeMessage oldMsg) throws Exception
    {
        MimeMessage ret = new MimeMessage(Session.getDefaultInstance(new Properties()));
        @SuppressWarnings("unchecked")
        Enumeration<Header> oldHeaders = oldMsg.getAllHeaders();
        while (oldHeaders.hasMoreElements()) {
            Header h = oldHeaders.nextElement();
            ret.setHeader(h.getName(), h.getValue());
        }

        ret.removeHeader(HeaderNames.CONTENT_TYPE);
        ret.removeHeader(HeaderNames.CONTENT_TRANSFER_ENCODING);
        ret.removeHeader(HeaderNames.CONTENT_DISPOSITION);

        ret.setHeader(HeaderNames.CONTENT_TYPE, HeaderNames.MULTIPART_MIXED + "; boundary=\"" + makeBoundary() + "\"");

        Multipart multipart = new MimeMultipart();
        InternetHeaders partHeaders = new InternetHeaders();
        partHeaders.addHeader(HeaderNames.CONTENT_TRANSFER_ENCODING, HeaderNames.SEVEN_BIT_STR);
        partHeaders.addHeader(HeaderNames.CONTENT_TYPE, HeaderNames.TEXT_PLAIN);

        BodyPart bodyPart = new MimeBodyPart(partHeaders, plainBodyContent.getBytes());
        multipart.addBodyPart(bodyPart);

        BodyPart bodyPartOldMsg = new MimeBodyPart();

        bodyPartOldMsg.setContent(oldMsg, HeaderNames.MESSAGE_RFC822);

        bodyPartOldMsg.setHeader(HeaderNames.CONTENT_TRANSFER_ENCODING, HeaderNames.SEVEN_BIT_STR);
        bodyPartOldMsg.setHeader(HeaderNames.CONTENT_DISPOSITION, HeaderNames.INLINE_DISPOSITION_STR);

        multipart.addBodyPart(bodyPartOldMsg);

        ret.setContent(multipart);
        ret.saveChanges();

        return ret;

    }

    /**
     * Get the RFC822-compliant representation of the current time
     * 
     * @return the formatted String
     */
    public static String getRFC822Date()
    {
        return getRFC822Date(new Date());
    }

    /**
     * Get the RFC822-compliant representation of the given Date
     * 
     * @param d
     *            the date
     * @return the formatted String
     */
    public static String getRFC822Date(Date d)
    {
        return new SimpleDateFormat(MAIL_FORMAT_STR).format(d);
    }

    /**
     * Creates a unique boundary value
     * @return String of boundary.
     */
    public static String makeBoundary()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("----");
        sb.append(System.identityHashCode(sb));
        sb.append('_');
        sb.append("060105_");
        sb.append(System.currentTimeMillis());
        return sb.toString();
    }

    /**
     * Determine if address is empty.
     * @param  address InternetAddress to process.
     * @return         true if empty, false otherwise.
     */
    public static boolean isNullAddress(InternetAddress address)
    {
        if (address.getAddress() == null || address.getAddress().trim().length() == 0)
            return true;
        return false;
    }

    /**
     * Helper method to parse a single address, which may or may not contains a personal. Should contain only one
     * address. If there are no addresses, the {@link #NULL_ADDRESS NULL_ADDRESS} is returned. <br>
     * @param str String to parse.
     * @return InternetAddress parsed from string.
     * @throws AddressException on conversion error.
     */
    public static InternetAddress parseEmailAddress(String str) throws AddressException
    {
        if (str == null || "".equals(str.trim())) {
            return new InternetAddress();
        }
        InternetAddress[] addresses = InternetAddress.parseHeader(str, false);
        if (addresses == null || addresses.length == 0) {
            return new InternetAddress();
        }
        if (addresses.length > 1) {
            throw new AddressException("Line contained more than one address \"" + str + "\"");
        }
        return addresses[0];
    }

    /**
     * Convert to a String suitable for SMTP transport. This removes any of the "personal" stuff, and makes sure it has
     * leading and trailing "<>".
     * @param address InternetAddress to process.
     * @return String of InternetAddress.
     */
    public static String toSMTPString(InternetAddress address)
    {
        if (isNullAddress(address)) {
            return "<>";
        }
        try {
            String oldPersonal = address.getPersonal();
            if (oldPersonal != null) {
                address.setPersonal(null);
                String ret = ensureBrackets(address.toString());
                address.setPersonal(oldPersonal);
                return ret;
            }
        } catch (Exception shouldNotHappen) {
            Logger.getLogger(CommandWithEmailAddress.class).error(shouldNotHappen);
        }
        return ensureBrackets(address.toString());
    }

    /**
     * Add brackets if needed.
     * @param  str String to add brackets around.
     * @return     String with brackets.
     */
    private static String ensureBrackets(String str)
    {
        if (0 != str.indexOf('<')) {
            str = "<" + str;
        }
        if (str.length() - 1 != str.indexOf('>')) {
            str = str + ">";
        }
        return str;
    }

    /**
     * Return number of attachments from message.
     * @param  msg MimeMessage to process.
     * @return     Number of attachments.
     */
    public static int attachmentCount(MimeMessage msg)
    {
        int cnt = 0;
        try {
            Object msgContent = msg.getContent();
            if (msgContent instanceof Multipart) {
                Multipart multipart = (Multipart) msgContent;
                for (int j = 0; j < multipart.getCount(); j++) {
                    BodyPart bodyPart = multipart.getBodyPart(j);

                    String disposition = bodyPart.getDisposition();
                    if (disposition != null && (disposition.equalsIgnoreCase(HeaderNames.ATTACHMENT_DISPOSITION_STR)))
                        cnt++;
                }
            }
        } catch (MessagingException e) {
            // ignore
        } catch (IOException e) {
            // ignore
        }
        return cnt;
    }
}

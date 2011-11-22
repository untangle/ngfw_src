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
package com.untangle.node.mime;

import static com.untangle.node.mime.HeaderNames.CONTENT_DISPOSITION_LC;
import static com.untangle.node.mime.HeaderNames.CONTENT_TRANSFER_ENCODING;
import static com.untangle.node.mime.HeaderNames.CONTENT_TRANSFER_ENCODING_LC;
import static com.untangle.node.mime.HeaderNames.CONTENT_TYPE;
import static com.untangle.node.mime.HeaderNames.CONTENT_TYPE_LC;
import static com.untangle.node.util.Ascii.BACK_SLASH;
import static com.untangle.node.util.Ascii.HTAB;
import static com.untangle.node.util.Ascii.QUOTE;
import static com.untangle.node.util.Ascii.SP;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 * Utility methods for working with MIME
 */
public class MIMEUtil
{
    public static final String MAIL_FORMAT_STR = "EEE, d MMM yyyy HH:mm:ss Z";

    private static final Logger logger = Logger.getLogger(MIMEUtil.class);

    public static final byte[] MIME_SPECIALS = {
        (byte) '(',
        (byte) ')',
        (byte) '<',
        (byte) '>',
        (byte) '@',
        (byte) ',',
        (byte) ';',
        (byte) ':',
        (byte) '\\',
        (byte) '"',
        (byte) '/',
        (byte) '[',
        (byte) ']',
        (byte) '?',
        (byte) '='
    };

    /**
     * Convienence blank array of MIMEParts
     */
    public static final MIMEPart[] EMPTY_MIME_PARTS = new MIMEPart[0];

    /**
     * Method to test if the given header value needs quoting
     *
     * @param str the String to test
     * @return true if it needs quoting
     */
    public static boolean needsHeaderQuote(final String str)
    {
        boolean needsQuote = str.indexOf(SP) > 0 ||
            str.indexOf(HTAB) > 0 ||
            str.indexOf(QUOTE) > 0;
        if(!needsQuote) {
            for(byte b : MIME_SPECIALS) {
                if(str.indexOf((char) b) > 0) {
                    needsQuote = true;
                    break;
                }
            }
        }
        return needsQuote;
    }

    /**
     * Returns a quoted String, if the given String
     * {@link #needsHeaderQuote needs quoting}.  This is
     * used when creating MIME headers.
     *
     * @param str the String which may need quoting
     *
     * @return the quoted String, or <code>str</code>
     *         if quoting {@link #needsHeaderQuote was not required}.
     */
    public static String headerQuoteIfNeeded(final String str)
    {
        if(needsHeaderQuote(str)) {
            return headerQuote(str);
        }
        return str;
    }

    /**
     * Quotes the given string for MIME headers.
     *
     * @param str the String to quote
     *
     * @return the quoted String
     */
    public static String headerQuote(final String str)
    {
        StringBuilder sb = new StringBuilder(str.length()+2);
        sb.append(QUOTE);
        for(int i = 0; i<str.length(); i++) {
            if(str.charAt(i) == QUOTE) {
                sb.append(BACK_SLASH);
            }
            sb.append(str.charAt(i));
        }
        sb.append(QUOTE);
        return sb.toString();
    }

    /**
     * Wraps the given MIMEMessage in a new MIMEMessage with
     * the old as an RFC822 attachment, with the new
     * plaintext (not multipart-alt) body.
     * (Since the wrapped MIMEMessage references the given MIMEMessage,
     *  do not dispose the given MIMEMessage.)
     *
     * @param plainBodyContent the new body content (should be line-formatted
     *        such that lines are not longer than 76 chars).
     * @param oldMsg the old message
     */
    public static MIMEMessage simpleWrap(String plainBodyContent, MIMEMessage oldMsg) throws Exception /* FIXME */
    {
        //First, we need to "steal" the old headers.  This
        // is easiest by simply re-parsing them
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MIMEOutputStream mimeOut = new MIMEOutputStream(baos);
        oldMsg.getMMHeaders().writeTo(mimeOut);
        mimeOut.flush();

        ByteArrayMIMESource bams = new ByteArrayMIMESource(baos.toByteArray());
        MIMEMessageHeaders newHeaders = MIMEMessageHeaders.parseMMHeaders(bams.getInputStream(), bams);

        //Modify new headers, changing the Content-* stuff
        newHeaders.removeHeaderFields(CONTENT_TYPE_LC);
        newHeaders.removeHeaderFields(CONTENT_TRANSFER_ENCODING_LC);
        newHeaders.removeHeaderFields(CONTENT_DISPOSITION_LC);
        newHeaders.addHeaderField(CONTENT_TYPE,
                                  ContentTypeHeaderField.MULTIPART_MIXED +
                                  "; boundary=\"" + makeBoundary() + "\"");

        newHeaders.addHeaderField(CONTENT_TRANSFER_ENCODING,
                                  ContentXFerEncodingHeaderField.SEVEN_BIT_STR);

        //Create the new message
        MIMEMessage ret = new MIMEMessage(newHeaders);

        //Create the body part of the new message
        MIMEPart bodyPart = new MIMEPart();
        bodyPart.getMPHeaders().addHeaderField(CONTENT_TYPE,
                                               ContentTypeHeaderField.TEXT_PLAIN);
        bodyPart.getMPHeaders().addHeaderField(CONTENT_TRANSFER_ENCODING,
                                               ContentXFerEncodingHeaderField.SEVEN_BIT_STR);
        byte[] bytes = plainBodyContent.getBytes();
        bodyPart.setContent(
                            new MIMESourceRecord(new ByteArrayMIMESource(bytes),
                                                 0,
                                                 bytes.length,
                                                 false));

        //Add the new body to the returned message
        ret.addChild(bodyPart);

        //Add the wrapped old message to the new (returned) message
        ret.addChild(new AttachedMIMEMessage(oldMsg));

        return ret;

    }

    /**
     * Appends the body of the given MIMEMessage with
     * plaintext string
     *
     * @param plainBodyContent the new body content (should be line-formatted
     *        such that lines are not longer than 76 chars).
     * @param oldMsg the old message
     */
    public static MIMEMessage appendString ( String plainBodyContent, MIMEMessage oldMsg )
    {
        try {
            logger.error("appendString: " + oldMsg.getSourceRecord().source.getInputStream().readLine().bufferToString());
            logger.error("parts: " + oldMsg.getNumChildren());
        }
        catch (Exception e) {}

        return oldMsg;
    }
    
    /**
     * Get the RFC822-compliant representation of
     * the current time
     *
     * @return the formatted String
     */
    public static String getRFC822Date()
    {
        return getRFC822Date(new Date());
    }

    /**
     * Get the RFC822-compliant representation of
     * the given Date
     *
     * @param d the date
     * @return the formatted String
     */
    public static String getRFC822Date(Date d)
    {
        return new SimpleDateFormat(MAIL_FORMAT_STR).format(d);
    }

    /**
     * Removes the child from its parent.  Unlike the method
     * with a similar name on MIMEPart itself, this method
     * fixes-up any parent container issues (for example,
     * the parent is "multipart" yet the removal causes
     * the parent to have no children).
     * <br><br>
     * If the child has no parent, then we assume that the
     * child is a top-level MIMEMessage.  In that case, we take
     * different action.  We assume that the intent is to
     * remove some "nasty" content, so we preserve the headers
     * (except for the "Content-XXX") stuff and replace
     * the body with blank text.  This is done
     * via {@link #convertToEmptyTextPart convertToEmptyTextPart()}.
     *
     * @param child the child to be removed from its parent.
     */
    public static void removeChild (MIMEPart child) throws HeaderParseException
    {
        MIMEPart parent = child.getParent();
        //Boundary-case.  If the parent is itself
        //a top-level MIMEMessage, and there are no other
        //children.  This really means "nuke my content"
        if(parent == null) {
            convertToEmptyTextPart(child);
            return;
        }
        parent.removeChild(child);

        if(parent.getNumChildren() == 0) {
            //If we just created an empty multipart, go up to the parent-parent
            //and remove
            removeChild(parent);
        }

    }

    /**
     * Changes the part into an empty "text/plain" part, discarding
     * any previous content
     */
    public static void convertToEmptyTextPart (MIMEPart part) throws HeaderParseException
    {
        part.getMPHeaders().removeHeaderFields(CONTENT_TYPE_LC);
        part.getMPHeaders().removeHeaderFields(CONTENT_TRANSFER_ENCODING_LC);
        part.getMPHeaders().removeHeaderFields(CONTENT_DISPOSITION_LC);

        part.getMPHeaders().addHeaderField(CONTENT_TYPE,ContentTypeHeaderField.TEXT_PLAIN);
        part.getMPHeaders().addHeaderField(CONTENT_TRANSFER_ENCODING,ContentXFerEncodingHeaderField.SEVEN_BIT_STR);

        part.setContent(new MIMESourceRecord(new ByteArrayMIMESource(new byte[0]),0,0,false));
    }

    /**
     * Creates a unique boundary value
     */
    public static String makeBoundary ()
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
     * Helper which returns a list of parts which may
     * be candidates for virus scanning.  Takes care of boundary
     * case where top-level part is actualy an attachment
     */
    public static MIMEPart[] getCandidateParts(MIMEMessage msg)
    {
        //Need to special-case the top-level message
        //which itsef is only an attachment
        if(msg.isMultipart()) {
            return msg.getLeafParts(true);
        }
        else {
            if(shouldScan(msg)) {
                logger.debug("Message itself is scannable (no child parts, but not \"" + ContentTypeHeaderField.TEXT_PRIM_TYPE_STR + "/*\" content type");
                return new MIMEPart[] {msg};
            }
            else {
                return EMPTY_MIME_PARTS;
            }
        }
    }

    /**
     * Currently any non-text part (or attachment) is scanned
     */
    public static boolean shouldScan(MIMEPart part) {

        return part.isAttachment() || (part.getMPHeaders().getContentTypeHF() != null &&
                                       !part.getMPHeaders().getContentTypeHF().getPrimaryType().
                                       equalsIgnoreCase(ContentTypeHeaderField.TEXT_PRIM_TYPE_STR));
    }


    //------------- Debug/Test ---------------
    /*
      public static void main(String[] args) throws Exception {

      File f = new File(args[0]);

      FileMIMESource source = new FileMIMESource(f);

      MIMEMessage mp = new MIMEMessage(source.getInputStream(),
      source,
      new MIMEPolicy(),
      null);

      MIMEMessage wrapped = simpleWrap("This is wrapped\r\n", mp);

      FileOutputStream fOut = new FileOutputStream(new File(args[0] + "_WRAPPED"));
      wrapped.writeTo(new MIMEOutputStream(fOut));
      fOut.flush();
      fOut.close();
      }
    */

    //------------- Debug/Test ---------------

}

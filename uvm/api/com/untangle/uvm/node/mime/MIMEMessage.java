/**
 * $Id$
 */
package com.untangle.node.mime;

import static com.untangle.node.util.Ascii.CRLF;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import com.untangle.uvm.node.TemplateValues;

/**
 * Class representing a MIMEMessage.  Adds the strongly-typed
 * {@link #getMMHeaders MIMEMessageHeaders} with convienence
 * members for a top-level message (such as recipient
 * and subject manipulation).
 * <br><br>
 * This class also implements {@link com.untangle.node.util.TemplateValues TemplateValues}.
 * The variable syntax for accessing elements of the MIMEMessage is based on
 * <code>MIMEMessage:&lt;name></code> where <code>name</code> can be any
 * one of the following:
 * <ul>
 *   <li>
 *     <code><b>TO</b></code> The "TO" addresses, as found in the MIME Headers
 *     Each recipient is on its own line.  Even if there is only one recipient,
 *     this variable will be substituted with the value of the recipient <i>followed
 *     by a CRLF.</i>  If there are no "TO" values on the header, than this will
 *     simply return a blank String ("").
 *   </li>
 *   <li>
 *     <code><b>CC</b></code> Any CC recipients, following the same rules for
 *     multiple values and new lines as <code>TO</code>
 *   </li>
 *   <li>
 *     <code><b>RECIPIENTS</b></code> Combination of <code>TO</code> and <code>CC</code>
 *   </li>
 *   <li>
 *     <code><b>FROM</b></code> The "FROM" as found in the headers.  If there is no
 *     FROM value, the literal "&lt;>" will be substituted.
 *   </li>
 *   <li>
 *     <code><b>SUBJECT</b></code> The Subject, as found on the message.  If there is
 *     no subject (or it is null), a blank string ("") will be returned.
 *   </li>
 * <!--
 *   <li>
 *     <code><b>XXXXX</b></code>
 *   </li>
 * -->
 * </ul>
 * Note also that any variables from the embedded Headers will also be
 * evaluated (see the docs on {@link com.untangle.node.mime.Headers Headers}
 * for a list of possible variables).
 */
public class MIMEMessage extends MIMEPart implements TemplateValues
{
    private static final String MIME_MESSAGE_TEMPLATE_PREFIX = "MIMEMessage:".toLowerCase();
    private static final String TO_TV = "TO".toLowerCase();
    private static final String CC_TV = "CC".toLowerCase();
    private static final String RECIP_TV = "RECIPIENTS".toLowerCase();
    private static final String FROM_TV = "FROM".toLowerCase();
    private static final String SUBJECT_TV = "SUBJECT".toLowerCase();

    public MIMEMessage ()
    {
        super();
    }

    public MIMEMessage (MIMEMessageHeaders headers)
    {
        super(headers);
    }

    /**
     * Construct a MIME part, reading until the outerBoundary.
     */
    public MIMEMessage (MIMEParsingInputStream stream, MIMESource source, boolean ownsSource, MIMEPolicy policy, String outerBoundary) throws IOException, InvalidHeaderDataException, HeaderParseException, MIMEPartParseException
    {
        super();
        parse(new MailMessageHeaderFieldFactory(), stream, source, ownsSource, policy, outerBoundary);
    }

    /**
     * Construct a MIME part, reading until the outerBoundary.
     */
    public MIMEMessage (MIMEParsingInputStream stream, MIMESource source, MIMEPolicy policy, String outerBoundary) throws IOException, InvalidHeaderDataException, HeaderParseException, MIMEPartParseException
    {
        this(stream, source, true, policy, outerBoundary);
    }

    /**
     * Construct a MIMEMessage using the already-parsed headers.
     */
    public MIMEMessage (MIMEParsingInputStream stream, MIMESource source, MIMEPolicy policy, String outerBoundary, MIMEMessageHeaders headers) throws IOException, InvalidHeaderDataException, HeaderParseException, MIMEPartParseException
    {
        super(stream, source, true, policy, outerBoundary, headers);
    }

    /**
     * For use in a Template
     */
    public String getTemplateValue ( String key )
    {
        //First, see if this key is for the child Headers
        String headerRet = getMMHeaders().getTemplateValue(key);
        if(headerRet != null) {
            return headerRet;
        }

        //Not for the headers.  Evaluate if this is a MIMEMessage variable
        key = key.trim().toLowerCase();
        if(key.startsWith(MIME_MESSAGE_TEMPLATE_PREFIX)) {
            key = key.substring(MIME_MESSAGE_TEMPLATE_PREFIX.length());
            if(key.equals(TO_TV)) {
                Set<EmailAddress> tos = getMMHeaders().getRecipients(RcptType.TO);
                StringBuilder sb = new StringBuilder();
                for(EmailAddress addr : tos) {
                    sb.append(addr.toMIMEString());
                    sb.append(CRLF);
                }
                return sb.toString();
            }
            else if(key.equals(CC_TV)) {
                Set<EmailAddress> ccs = getMMHeaders().getRecipients(RcptType.CC);
                StringBuilder sb = new StringBuilder();
                for(EmailAddress addr : ccs) {
                    sb.append(addr.toMIMEString());
                    sb.append(CRLF);
                }
                return sb.toString();
            }
            else if(key.equals(RECIP_TV)) {
                List<EmailAddressWithRcptType> allRcpts = getMMHeaders().getAllRecipients();
                StringBuilder sb = new StringBuilder();
                for(EmailAddressWithRcptType eawrt : allRcpts) {
                    sb.append(eawrt.address.toMIMEString());
                    sb.append(CRLF);
                }
                return sb.toString();
            }
            else if(key.equals(FROM_TV)) {
                EmailAddress from = getMMHeaders().getFrom();
                if(from == null || from.isNullAddress()) {
                    return "<>";
                }
                return from.toMIMEString();
            }
            else if(key.equals(SUBJECT_TV)) {
                return getMMHeaders().getSubject() == null?
                    "":getMMHeaders().getSubject();
            }
        }
        return null;
    }

    /**
     * Get the MIMEMessageHeaders for this MIMEMessage.  Changes
     * to the headers will be known by this message.
     *
     * @return the headers
     */
    public MIMEMessageHeaders getMMHeaders ()
    {
        return (MIMEMessageHeaders) getMPHeaders();
    }

    /**
     * Convieience method to count attachments
     */
    public int getAttachmentCount ()
    {
        MIMEPart[] kids = getLeafParts(true);
        if(kids == null) {
            return 0;
        }
        int ret = 0;
        for(MIMEPart part : kids) {
            if(part.isAttachment()) {
                ret++;
            }
        }
        return ret;
    }

    /**
     * Get the contents of this MIMEPart as a file.  This applies to
     *
     */
    public final File toFile ( ) throws IOException
    {
        if(isChanged() || getSourceRecord() == null) {
            FileOutputStream fOut = null;
            try {
                File ret = File.createTempFile("MIMEMessage-", null);
                fOut = new FileOutputStream(ret);
                BufferedOutputStream bufOut = new BufferedOutputStream(fOut);
                MIMEOutputStream mimeOut = new MIMEOutputStream(bufOut);
                writeTo(mimeOut);
                mimeOut.flush();
                bufOut.flush();
                fOut.flush();
                fOut.close();
                return ret;
            }
            catch(IOException ex) {
                try{fOut.close();}catch(Exception ignore){}
                IOException ex2 = new IOException();
                ex2.initCause(ex);
                throw ex;
            }
        }
        return getSourceRecord().source.toFile( );
    }

    /**
     * <b>Do not use this method.  It is for debugging.  It will
     * cause too much to be read into memory</b>
     *
     * Returned buffer is ready for reading.
     */
    public ByteBuffer toByteBuffer() throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MIMEOutputStream mos = new MIMEOutputStream(baos);
        writeTo(mos);
        mos.flush();
        return ByteBuffer.wrap(baos.toByteArray());
    }

    //------------- Debug/Test ---------------

    public static void main(String[] args) throws Exception
    {

        File f = new File(args[0]);

        File tempDir = new File(new File(System.getProperty("user.dir")),
                                "mimeFiles");
        if(!tempDir.exists()) {
            tempDir.mkdirs();
        }

        //Dump file to another file, with byte offsets.  This
        //makes troubleshooting really easy
        FileInputStream fIn = new FileInputStream(f);
        FileOutputStream fOut =
            new FileOutputStream(new File("byteMap.txt"));
        int rawRead = fIn.read();
        int counter = 0;
        while(rawRead != -1) {
            fOut.write((counter + ": ").getBytes());
            if(rawRead < 33 || rawRead > 126) {
                fOut.write(("(unprintable)" + rawRead).getBytes());
            }
            else {
                fOut.write((byte) rawRead);
            }
            fOut.write(System.getProperty("line.separator").getBytes());
            rawRead = fIn.read();
            counter++;
        }
        fIn.close();
        fOut.flush();
        fOut.close();

        FileMIMESource source = new FileMIMESource(f);

        MIMEMessage mp = new MIMEMessage(source.getInputStream(),
                                         source,
                                         new MIMEPolicy(),
                                         null);

        System.out.println("");
        System.out.println("Message has subject: \"" +
                           mp.getMMHeaders().getSubject() + "\"");
        System.out.println("BEGIN Recipients");
        List<EmailAddressWithRcptType> allRcpts = mp.getMMHeaders().getAllRecipients();
        for(EmailAddressWithRcptType rwt : allRcpts) {
            System.out.println(rwt);
        }
        System.out.println("ENDOF Recipients");
        mp.dump("");

        File file = null;
        if(mp.isMultipart()) {

            MIMEPart[] children = mp.getLeafParts(true);

            System.out.println("Now, decode the " + children.length + " leaf children");
            for(MIMEPart part : children) {
                if(!part.isMultipart()) {
                    file = part.getContentAsFile( false);
                    System.out.println("Raw part to: " + file.getName());
                    file = part.getContentAsFile( true);
                    System.out.println("Decoded part to: " + file.getName());
                }
            }

            for(MIMEPart part : children) {
                part.changed();
                part.getObserver().mIMEPartChanged(part);
                part.getMPHeaders().addHeaderField("FooBar", "Goo");
                part.getMPHeaders().removeHeaderFields(new LCString("FooBar"));
            }
            System.out.println("Try writing it out (after declaring changed)");
            fOut = new FileOutputStream(new File(tempDir, "redone.txt"));
            mp.writeTo(new MIMEOutputStream(fOut));
            fOut.flush();
            fOut.close();
        }
        else {
            file = mp.getContentAsFile( false);
            System.out.println("Raw part to: " + file.getName());
            file = mp.getContentAsFile( true);
            System.out.println("Decoded part to: " + file.getName());
            System.out.println("Try writing it out (after declaring changed)");
            mp.changed();
            //        mp.getObserver().mIMEPartChanged(part);
            mp.getMPHeaders().addHeaderField("FooBar", "Goo");
            mp.getMPHeaders().removeHeaderFields(new LCString("FooBar"));
            fOut = new FileOutputStream(new File(tempDir, "redone.txt"));
            mp.writeTo(new MIMEOutputStream(fOut));
            fOut.flush();
            fOut.close();
        }

    }
}

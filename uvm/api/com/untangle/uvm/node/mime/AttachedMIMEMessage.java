/**
 * $Id$
 */
package com.untangle.node.mime;

import static com.untangle.node.mime.HeaderNames.CONTENT_DISPOSITION;
import static com.untangle.node.mime.HeaderNames.CONTENT_TRANSFER_ENCODING;
import static com.untangle.node.mime.HeaderNames.CONTENT_TYPE;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * Wrapper class which lets us attach a MIMEMessage
 * as an RFC822 Message.  Takes care of the correct
 * Content-XXXX headers to be an attached email.
 */
public class AttachedMIMEMessage extends MIMEPart
{
    private final Logger m_logger = Logger.getLogger(AttachedMIMEMessage.class);

    private MIMEMessage m_attach;

    public AttachedMIMEMessage(MIMEPartHeaders headers) throws IOException,
                                                               InvalidHeaderDataException,
                                                               HeaderParseException,
                                                               MIMEPartParseException
    {
        super(headers);
    }

    public AttachedMIMEMessage(MIMEMessage attach)
    {
        super();

        try {
            getMPHeaders().addHeaderField(CONTENT_TYPE, ContentTypeHeaderField.MESSAGE_RFC822);
            getMPHeaders().addHeaderField(CONTENT_TRANSFER_ENCODING, ContentXFerEncodingHeaderField.SEVEN_BIT_STR);
            getMPHeaders().addHeaderField(CONTENT_DISPOSITION, ContentDispositionHeaderField.INLINE_VAL);
        }
        catch(Exception ex) {
            //SHouldn't happen!
            m_logger.error(ex);
        }
        setWrappedMessage(attach);
    }

    public AttachedMIMEMessage( MIMEParsingInputStream stream, MIMESource source, boolean ownsSource, MIMEPolicy policy, String outerBoundary, MIMEPartHeaders headers )
        throws IOException, InvalidHeaderDataException, HeaderParseException, MIMEPartParseException
    {
        super(headers);
        m_logger.debug("[<init>] parse child");
        m_attach = new MIMEMessage(stream,
                                   source,
                                   false,
                                   policy,
                                   outerBoundary);
        m_attach.setObserver(m_childObserver);
        m_attach.setParent(this);
        getChildList().add(m_attach);
    }

    protected MIMEParsingInputStream.BoundaryResult parseChild( MIMEParsingInputStream stream, MIMESource source, MIMEPolicy policy, String outerBoundary)
        throws IOException, InvalidHeaderDataException, HeaderParseException, MIMEPartParseException
    {
        MIMEMessage msg = new MIMEMessage();
        MIMEParsingInputStream.BoundaryResult ret =
            msg.parse(new MailMessageHeaderFieldFactory(),
                      stream,
                      source,
                      false,
                      policy,
                      outerBoundary);
        setWrappedMessage(msg);
        return ret;
    }

    @Override
    public boolean isMultipart()
    {
        checkDisposed();
        return true;
    }

    @Override
    public void addChild(MIMEPart child)
    {
        checkDisposed();
        if(!(child instanceof MIMEMessage)) {
            throw new UnsupportedOperationException("Can only set one child of type MIMEMessage");
        }
        if(getWrappedMessage() != null) {
            removeChild(getWrappedMessage());
        }
        m_attach = (MIMEMessage) child;
        super.addChild(child);
    }

    public MIMEMessage getWrappedMessage()
    {
        return m_attach;
    }

    public void setWrappedMessage(MIMEMessage msg)
    {
        addChild(msg);
    }


    @Override
    public File getContentAsFile( boolean decoded ) throws IOException
    {
        checkDisposed();

        File f = File.createTempFile("AttachedMIMEMessage-", null);
        FileOutputStream fOut = null;
        try {
            //TODO bscott Again, this is a waste.  Cache files better!
            fOut = new FileOutputStream(f);
            BufferedOutputStream bufOut = new BufferedOutputStream(fOut);
            MIMEOutputStream mimeOut = new MIMEOutputStream(bufOut);
            m_attach.writeTo(mimeOut);
            mimeOut.flush();
            bufOut.flush();
            fOut.flush();
            fOut.close();
            return f;
        }
        catch(IOException ex) {
            try {f.delete();}catch(Exception ignore){}
            try {fOut.close();}catch(Exception ignore){}
            IOException ex2 = new IOException();
            ex2.initCause(ex);
            throw ex2;
        }
    }

    @Override
    public final void writeTo(MIMEOutputStream out)
        throws IOException
    {
        checkDisposed();
        m_logger.debug("[writeTo()] write headers");
        getMPHeaders().writeTo(out);
        m_logger.debug("[writeTo()] BEGIN write Wrapped");
        m_attach.writeTo(out);
        m_logger.debug("[writeTo()] ENDOF write Wrapped");
    }

}

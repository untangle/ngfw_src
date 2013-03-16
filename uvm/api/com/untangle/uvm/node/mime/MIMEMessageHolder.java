/**
 * $Id$
 */
package com.untangle.node.mime;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Top-level holder class.  Maintains an
 * association between a MIMEMessage and its
 * MIMESource.  It is assumes that the
 * MIMESource contains the <b>entire</b>
 * MIMEMessage.
 *
 * @depricated
 */
public class MIMEMessageHolder
{
    private MIMEMessage m_message;
    private MIMESource m_source;
    private boolean m_changed = false;
    private File m_file;


    public MIMEMessageHolder( MIMEMessage message, MIMESource source )
    {
        m_changed = message.isChanged();
        message.setObserver(new MIMEPartObserver() {
                public void mIMEPartChanged(MIMEPart part) {
                    m_changed = true;
                }
            });
        m_source = source;
    }

    public MIMEMessage getMIMEMessage()
    {
        return m_message;
    }

    /**
     * Write-out the MIMEMessage.  If the
     * message has not changed, the source is used.
     * Otherwise, the message's writeTo
     * method is invoked.
     *
     * @param out the output stream.
     */
    public void writeTo(MIMEOutputStream out) throws IOException
    {
        if(m_changed) {
            m_message.writeTo(out);
        }
        else {
            out.pipe(m_source.getInputStream());
        }
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

    /**
     * Ask that the contents of the internal MIMEMessage
     * be written to file.  Note that this may or
     * may not create a new file, depending on if
     * the Message has changed (or if the Source never
     * had a backing file.
     * <br>
     * <b>For now, this should only be called once.</b>
     */
    public File toFile( ) throws IOException
    {
        if(m_changed) {
            if(m_file == null) {
                FileOutputStream fOut = null;
                try {
                    m_file = File.createTempFile("MIMEMessageHolder-", null);
                    fOut = new FileOutputStream(m_file);
                    BufferedOutputStream bufOut = new BufferedOutputStream(fOut);
                    MIMEOutputStream mimeOut = new MIMEOutputStream(bufOut);
                    m_message.writeTo(mimeOut);
                    mimeOut.flush();
                    bufOut.flush();
                    fOut.flush();
                    fOut.close();
                }
                catch(IOException ex) {
                    try {fOut.close();}catch(Exception ignore){}
                    try {m_file.delete();}catch(Exception ignore){}
                    m_file = null;
                    IOException ex2 = new IOException();
                    ex2.initCause(ex);
                    throw ex2;
                }
            }
            return m_file;
        }
        else {
            return m_source.toFile();
        }
    }

    /**
     * Close this holder.  Also closes the
     * source and MIMEMessage
     */
    public void close()
    {
        m_message.dispose();
        m_source.close();
        if(m_file != null) {
            m_file.delete();
        }
        m_source = null;
        m_message = null;
    }

}

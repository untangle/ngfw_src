/**
 * $Id$
 */
package com.untangle.node.mime;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Implementation of MIMESource which has a backing
 * byte array.  Useful for testing.
 */
public class ByteArrayMIMESource implements MIMESource
{
    private final byte[] m_bytes;
    private final int m_start;
    private final int m_len;


    /**
     * Construct a new source wrapping the given bytes
     *
     * @param bytes the bytes
     */
    public ByteArrayMIMESource(byte[] bytes)
    {
        this(bytes, 0, bytes.length);
    }

    /**
     * Construct a new source wrapping the given bytes
     *
     * @param bytes the bytes
     * @param start start
     * @param len the length
     */
    public ByteArrayMIMESource(byte[] bytes, int start, int len)
    {
        m_bytes = bytes;
        m_start = start;
        m_len = len;
    }

    //-------------------------
    // See doc on MIMESource
    //-------------------------
    public MIMEParsingInputStream getInputStream()
    {
        return new MIMEParsingInputStream( new ByteArrayInputStream(m_bytes, m_start, m_len) );
    }

    public MIMEParsingInputStream getInputStream(long offset) throws IOException
    {
        return new MIMEParsingInputStream( new ByteArrayInputStream(m_bytes, (int) offset, (m_len - (int) offset)) );
    }

    public void close() { }

    public File toFile( ) throws IOException
    {
        File f = File.createTempFile("ByteArrayInputStream-", null);
        FileOutputStream fOut = new FileOutputStream(f);
        fOut.write(m_bytes, m_start, m_len);
        fOut.flush();
        fOut.close();
        return f;
    }

    public File toFile( String name ) throws IOException
    {
        return toFile();
    }

    public static void main(String[] args) throws Exception
    {
        com.untangle.node.util.ASCIIStringBuilder sb = new com.untangle.node.util.ASCIIStringBuilder();
        String newLine = "\r\n";
        sb.append("Received: foobaloo");
        sb.append(newLine);
        sb.append("Subject: This is folded");
        sb.append(newLine);
        sb.append(" into two lines");
        sb.append(newLine);
        sb.append("To:<bscott@sigaba.com>,foo@moo.com");
        sb.append(newLine);
        sb.append(newLine);

        ByteArrayMIMESource mabs = new ByteArrayMIMESource(sb.toString().getBytes());

        HeadersParser parser = new HeadersParser();
        Headers headers = parser.parseHeaders(mabs.getInputStream(),
                                              mabs,
                                              new MailMessageHeaderFieldFactory(),
                                              new MIMEPolicy());

        System.out.println(headers.toString());

    }
}

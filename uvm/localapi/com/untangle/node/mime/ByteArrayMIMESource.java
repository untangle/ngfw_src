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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.untangle.node.util.FileFactory;

/**
 * Implementation of MIMESource which has a backing
 * byte array.  Useful for testing.
 */
public class ByteArrayMIMESource
    implements MIMESource {

    private final byte[] m_bytes;
    private final int m_start;
    private final int m_len;


    /**
     * Construct a new source wrapping the given bytes
     *
     * @param bytes the bytes
     */
    public ByteArrayMIMESource(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    /**
     * Construct a new source wrapping the given bytes
     *
     * @param bytes the bytes
     * @param start start
     * @param len the length
     */
    public ByteArrayMIMESource(byte[] bytes,
                               int start,
                               int len) {
        m_bytes = bytes;
        m_start = start;
        m_len = len;
    }

    //-------------------------
    // See doc on MIMESource
    //-------------------------
    public MIMEParsingInputStream getInputStream() {
        return new MIMEParsingInputStream(
                                          new ByteArrayInputStream(m_bytes, m_start, m_len));
    }

    public MIMEParsingInputStream getInputStream(long offset)
        throws IOException {
        return new MIMEParsingInputStream(
                                          new ByteArrayInputStream(m_bytes, (int) offset, (m_len - (int) offset)));
    }

    public void close() {
    }


    public File toFile(FileFactory factory) throws IOException {
        //TODO bscott If this class is ever anything more than for testing,
        //     we should cache the file.
        File f = factory.createFile();
        FileOutputStream fOut = new FileOutputStream(f);
        fOut.write(m_bytes, m_start, m_len);
        fOut.flush();
        fOut.close();
        return f;
    }

    public File toFile(FileFactory factory, String name) throws IOException {
        return toFile(factory);
    }

    public static void main(String[] args)
        throws Exception {


        com.untangle.node.util.ASCIIStringBuilder sb =
            new com.untangle.node.util.ASCIIStringBuilder();
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

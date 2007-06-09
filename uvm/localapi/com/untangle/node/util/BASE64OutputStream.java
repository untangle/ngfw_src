/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.node.util;

import java.io.*;
import static com.untangle.node.util.BASE64InputStream.BASE64_ALPHABET;
import static com.untangle.node.util.Ascii.*;

/**
 * TODO bscott This class needs to be examined
 */
public class BASE64OutputStream
    extends FilterOutputStream {

    private static final int MAX_LINE_LENGTH = 76;

    private final byte[] m_buf;
    private int m_bufLength;
    private int m_bytesInLine;


    /**
     *
     */
    public BASE64OutputStream(OutputStream target) {
        super(target);
        m_buf = new byte[3];
        m_bytesInLine = 0;
        m_bufLength = 0;
    }

    @Override
    public void write(int c)
        throws IOException {
        m_buf[m_bufLength++] = (byte)c;
        if (m_bufLength==3) {
            encode();
            m_bufLength = 0;
        }
    }

    @Override
    public void write(byte b[])
        throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte b[], int off, int len)
        throws IOException {
        for (int i=0; i<len; i++) {
            write(b[off+i]);
        }
    }

    @Override
    public void flush()
        throws IOException {
        if (m_bufLength>0) {
            encode();
            m_bufLength = 0;
        }
        out.flush();
    }

    @Override
    public void close()
        throws IOException {
        flush();
        out.close();
    }

    private void encode()
        throws IOException {
        if ((m_bytesInLine+4)>MAX_LINE_LENGTH) {
            out.write(CR);
            out.write(LF);
            m_bytesInLine = 0;
        }
        if (m_bufLength==1) {
            byte b1 = m_buf[0];
            int zero = 0;
            out.write(BASE64_ALPHABET[b1>>>2 & 0x3f]);
            out.write(BASE64_ALPHABET[(b1<<4 & 0x30) + (zero>>>4 & 0xf)]);
            out.write(EQ);
            out.write(EQ);
        }
        else if (m_bufLength==2) {
            byte b1 = m_buf[0], b2 = m_buf[1];
            int zero = 0;
            out.write(BASE64_ALPHABET[b1>>>2 & 0x3f]);
            out.write(BASE64_ALPHABET[(b1<<4 & 0x30) + (b2>>>4 & 0xf)]);
            out.write(BASE64_ALPHABET[(b2<<2 & 0x3c) + (zero>>>6 & 0x3)]);
            out.write(EQ);
        }
        else {
            byte b1 = m_buf[0], b2 = m_buf[1], b3 = m_buf[2];
            out.write(BASE64_ALPHABET[b1>>>2 & 0x3f]);
            out.write(BASE64_ALPHABET[(b1<<4 & 0x30) + (b2>>>4 & 0xf)]);
            out.write(BASE64_ALPHABET[(b2<<2 & 0x3c) + (b3>>>6 & 0x3)]);
            out.write(BASE64_ALPHABET[b3 & 0x3f]);
        }
        m_bytesInLine += 4;
    }

}

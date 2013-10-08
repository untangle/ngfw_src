/*
 * $HeadURL: svn://chef/work/src/uvm/api/com/untangle/node/util/BASE64OutputStream.java $
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
package com.untangle.node.util;

import static com.untangle.node.util.Ascii.CR;
import static com.untangle.node.util.Ascii.EQ;
import static com.untangle.node.util.Ascii.LF;
import static com.untangle.node.util.BASE64InputStream.BASE64_ALPHABET;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

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

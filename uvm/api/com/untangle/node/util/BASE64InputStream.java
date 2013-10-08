/*
 * $HeadURL: svn://chef/work/src/uvm/api/com/untangle/node/util/BASE64InputStream.java $
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

import static com.untangle.node.util.Ascii.EQ;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * TODO bscott This class needs to be examined
 */
public class BASE64InputStream
    extends FilterInputStream {

    private final byte[] m_buf;
    private int m_bufLength;
    private int m_bufIndex;
    private final byte[] m_decodeBuf;

    static final char[] BASE64_ALPHABET = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
        'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
        'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
        'w', 'x', 'y', 'z', '0', '1', '2', '3',
        '4', '5', '6', '7', '8', '9', '+', '/'
    };

    private static final byte[] s_decodeMapping = new byte[256];


    static {
        for (int i = 0; i<255; i++) {
            s_decodeMapping[i] = -1;
        }
        for (int i = 0; i<BASE64_ALPHABET.length; i++) {
            s_decodeMapping[BASE64_ALPHABET[i]] = (byte)i;
        }
    }

    /**
     *
     */
    public BASE64InputStream(InputStream source) {
        super(source);
        m_decodeBuf = new byte[4];
        m_buf = new byte[3];
    }


    @Override
    public int read()
        throws IOException {
        if (m_bufIndex>=m_bufLength) {
            fill();
            if (m_bufLength==0) {
                return -1;
            }
            m_bufIndex = 0;
        }
        return m_buf[m_bufIndex++] & 0xff;
    }

    @Override
    public int read(final byte[] bytes, final int start, final int len)
        throws IOException {

        int read = -1;
        for(int i = 0;i<len; i++) {
            read = read();
            if(read == -1) {
                return i==0?
                    -1:
                    i;
            }
            bytes[start+i] = (byte) read;
        }
        return len;
    }

    @Override
    public int available()
        throws IOException {
        return (in.available()*3)/4+(m_bufLength-m_bufIndex);
    }

    /**
     * Mark is not supported, so this method does nothing
     *
     * @exception IOException from the backing stream
     */
    @Override
        public void mark(int readlimit) {
        //Do nothing
    }



    /**
     * Since marks are not supported, this always throws
     * an exception
     *
     * @exception IOException (always)
     */
    @Override
        public void reset()
        throws IOException {
        throw new IOException("mark not supported");
    }


    /**
     * Always returns false
     *
     * @exception IOException from the backing stream
     */
    @Override
        public boolean markSupported() {
        return false;
    }


    private void fill()
        throws IOException {

        m_bufLength = 0;
        int c;

        int index = 0;

        while(index < 4) {
            c = in.read();
            if(c == -1) {
                return;
            }
            if(s_decodeMapping[c] == -1 && c != '=') {
                continue;
            }
            m_decodeBuf[index++] = (byte) c;
        }


        byte b0 = s_decodeMapping[m_decodeBuf[0] & 0xff];
        byte b2 = s_decodeMapping[m_decodeBuf[1] & 0xff];
        m_buf[m_bufLength++] = (byte)(b0<<2 & 0xfc | b2>>>4 & 0x3);
        if (m_decodeBuf[2]!=EQ)
            {
                b0 = b2;
                b2 = s_decodeMapping[m_decodeBuf[2] & 0xff];
                m_buf[m_bufLength++] = (byte)(b0<<4 & 0xf0 | b2>>>2 & 0xf);
                if (m_decodeBuf[3]!=EQ)
                    {
                        b0 = b2;
                        b2 = s_decodeMapping[m_decodeBuf[3] & 0xff];
                        m_buf[m_bufLength++] = (byte)(b0<<6 & 0xc0 | b2 & 0x3f);
                    }
            }
    }

}

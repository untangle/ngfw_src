/*
 * $HeadURL: svn://chef/work/src/uvm/api/com/untangle/node/util/QPInputStream.java $
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
import static com.untangle.node.util.Ascii.SP;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 *
 */
public class QPInputStream
    extends FilterInputStream {

    private final byte[] m_buf;
    private int m_queuedSpaces;

    /**
     *
     */
    public QPInputStream(InputStream in) {
        super(new PushbackInputStream(in, 2));
        m_buf = new byte[2];
        m_queuedSpaces = 0;
    }

    public int read()
        throws IOException {
        if(m_queuedSpaces>0) {
            m_queuedSpaces--;
            return SP;
        }

        int read = in.read();
        if(read==SP) {
            while ((read = in.read())==SP) {
                m_queuedSpaces++;
            }
            if(
               read==LF ||
               read==CR ||
               read==-1) {
                m_queuedSpaces = 0;
            }
            else {
                ((PushbackInputStream)in).unread(read);
                read = SP;
            }
            return read;
        }
        if(read==EQ) {
            int read2 = super.in.read();
            if(read2==LF) {
                return read();
            }
            if(read2==CR) {
                int peek = in.read();
                if (peek!=LF) {
                    ((PushbackInputStream)in).unread(peek);
                }
                return read();
            }
            if(read2==-1) {
                return read2;
            }

            m_buf[0] = (byte)read2;
            m_buf[1] = (byte)in.read();
            try {
                return Integer.parseInt(new String(m_buf, 0, 2), 16);
            }
            catch (NumberFormatException e) {
                ((PushbackInputStream)in).unread(m_buf);
            }
            return read;
        }
        else
            return read;
    }


    public int read(byte[] bytes, int off, int len)
        throws IOException {
        int nextInsert = 0;
        try {
            while (nextInsert<len) {
                int c = read();
                if (c==-1) {
                    if(nextInsert==0) {
                        nextInsert = -1;
                    }
                    break;
                }
                bytes[off+nextInsert] = (byte)c;
                nextInsert++;
            }
        }
        catch (IOException e) {
            nextInsert = -1;
        }
        return nextInsert;
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


    @Override
    public int available()
        throws IOException {
        return in.available() + m_queuedSpaces;
    }

}

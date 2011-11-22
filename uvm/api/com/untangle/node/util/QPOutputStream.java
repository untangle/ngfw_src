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
package com.untangle.node.util;

import static com.untangle.node.util.Ascii.CR;
import static com.untangle.node.util.Ascii.CRLF_BA;
import static com.untangle.node.util.Ascii.EQ;
import static com.untangle.node.util.Ascii.LF;
import static com.untangle.node.util.Ascii.SP;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * TODO bscott This class needs to be examined
 */
public class QPOutputStream
    extends FilterOutputStream {

    private static final int MAX_LINE_LEN = 76;
    private static final byte[] HEX_ALPHABET = {
        (byte) '0',(byte) '1',(byte) '2',(byte) '3',
        (byte) '4',(byte) '5',(byte) '6',(byte) '7',
        (byte) '8',(byte) '9',(byte) 'A',(byte) 'B',
        (byte) 'C',(byte) 'D',(byte) 'E',(byte) 'F'
    };


    private int m_lineLength;
    private boolean m_spaceQueued;
    private boolean m_lastCR;


    public QPOutputStream(OutputStream stream) {
        super(stream);
        this.m_lineLength = 0;
        this.m_spaceQueued = false;
        this.m_lastCR = false;
    }



    @Override
    public void flush()
        throws IOException {
        if(m_spaceQueued) {
            writeImpl(SP, false);
            m_spaceQueued = false;
        }
        out.flush();
    }

    @Override
    public void write(byte[] bytes, int offset, int length)
        throws IOException  {

        for (int i = offset; i < length; i++) {
            write(bytes[i]);
        }

    }

    @Override
    public void write(byte[] bytes)
        throws IOException {
        write(bytes, 0, bytes.length);
    }

    @Override
    public void write(int b)
        throws IOException {

        b &= 0xff;
        if (m_spaceQueued) {
            if (b==LF || b==CR) {
                writeImpl(SP, true);
            }
            else {
                writeImpl(SP, false);
            }
            m_spaceQueued = false;
        }
        if (b==SP) {
            m_spaceQueued = true;
        }
        else if (b==CR) {
            m_lastCR = true;
            writeCRLF();
        }
        else if (b==LF) {
            if (m_lastCR) {
                m_lastCR = false;
            }
            else {
                writeCRLF();
            }
        }
        else {
            if (b<SP || b>=127 || b==EQ) {
                writeImpl(b, true);
            }
            else {
                writeImpl(b, false);
            }
        }
    }

    @Override
    public void close()
        throws IOException {
        out.close();
    }


    private void writeImpl(int b, boolean encode)
        throws IOException {
        if(encode) {
            if((m_lineLength += 3) > MAX_LINE_LEN) {
                out.write(EQ);
                out.write(CRLF_BA);
                m_lineLength = 3;
            }
            out.write(EQ);
            out.write(HEX_ALPHABET[b >> 4]);
            out.write(HEX_ALPHABET[b & 0xf]);
        }
        else {
            if(++m_lineLength > MAX_LINE_LEN) {
                out.write(EQ);
                out.write(CRLF_BA);
                m_lineLength = 1;
            }
            out.write(b);
        }
    }


    private void writeCRLF()
        throws IOException {
        out.write(CRLF_BA);
        m_lineLength = 0;
    }


}

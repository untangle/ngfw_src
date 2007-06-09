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
package com.untangle.tran.util;

import static com.untangle.tran.util.Ascii.*;
import java.io.*;


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

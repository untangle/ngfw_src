/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail;

import static com.metavize.tran.util.Ascii.*;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

public class Base64Encoder implements ByteEncoder
{
    private static final int QUANTUM_SIZE = 3;
    private static final int LINE_LENGTH = 76;
    private static final int OUT_BUFFER_SIZE = 4096;

    private static final byte[] ALPHABET =
    {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
        'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
        'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
        'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', '+', '/'
    };
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    private final byte[] quantum = new byte[QUANTUM_SIZE];

    private final Logger logger = Logger.getLogger(Base64Encoder.class);

    private int quantumIdx = 0;
    private int col = 0;
    private boolean ended = false;

    public List<ByteBuffer> encode(ByteBuffer buf)
    {
        if (ended) {
            throw new IllegalStateException("already ended");
        }

        List l = new LinkedList<ByteBuffer>();

        int r = buf.remaining();
        ByteBuffer out = ByteBuffer.allocate(OUT_BUFFER_SIZE);
        l.add(out);

        while (buf.hasRemaining()) {
            while (QUANTUM_SIZE > quantumIdx && buf.hasRemaining()) {
                quantum[quantumIdx++] = buf.get();
            }

            if (QUANTUM_SIZE == quantumIdx) {
                quantumIdx = 0;;

                int q = (quantum[0] << 16) & 0x00FF0000;
                q |= (quantum[1] << 8)     & 0x0000FF00;
                q |= quantum[2]            & 0x000000FF;

                if (6 > out.remaining()) {
                    out.flip();
                    out = ByteBuffer.allocate(OUT_BUFFER_SIZE);
                    l.add(out);
                }

                out.put(ALPHABET[q >>> 18 & 0x3F]);
                out.put(ALPHABET[q >>> 12 & 0x3F]);
                out.put(ALPHABET[q >>> 6  & 0x3F]);
                out.put(ALPHABET[q & 0x3F]);

                col += 4;

                if (LINE_LENGTH <= col) {
                    col = 0;

                    out.put((byte)CR);
                    out.put((byte)LF);
                }
            }
        }

        out.flip();

        return l;
    }

    public ByteBuffer endEncoding()
    {
        if (ended) {
            throw new IllegalStateException("already ended");
        }

        ByteBuffer buf = ByteBuffer.allocate(6);

        if (1 == quantumIdx) {
            int q = quantum[0] << 16;
            buf.put(ALPHABET[q >>> 18 & 0x3F]);
            buf.put(ALPHABET[q >>> 12 & 0x3F]);
            buf.put((byte)'=');
            buf.put((byte)'=');
        } else if (2 == quantumIdx) {
            int q = quantum[0] << 16;
            q |= quantum[1] << 8;

            buf.put(ALPHABET[q >>> 18 & 0x3F]);
            buf.put(ALPHABET[q >>> 12 & 0x3F]);
            buf.put(ALPHABET[q >>> 6 & 0x3F]);
            buf.put((byte)'=');
        }

        buf.put((byte)CR);
        buf.put((byte)LF);

        buf.flip();
        return buf;
    }
}

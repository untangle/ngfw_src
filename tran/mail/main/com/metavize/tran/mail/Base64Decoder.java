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

import com.metavize.tran.util.AsciiCharBuffer;
import org.apache.log4j.Logger;

public class Base64Decoder implements ByteDecoder
{
    private static final Logger logger = Logger.getLogger(Base64Decoder.class);

    private static final int QUANTUM_SIZE = 4;

    private static final byte[] TEBAHPLA =
    {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63,
        52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1,
        -1, -1, -1, -1, -1,  0,  1,  2,  3,  4,  5,  6,
         7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
        19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
        -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36,
        37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48,
        49, 50, 51
    };

    private final byte[] quantum = new byte[QUANTUM_SIZE];

    private int quantumIdx = 0;
    private int equalCount = 0;
    private boolean endOfInput = false;

    // Decoder methods --------------------------------------------------------

    public List<ByteBuffer> decode(ByteBuffer buf)
    {
        if (endOfInput) {
            logger.warn("end of input, ignoring data: '"
                        + AsciiCharBuffer.wrap(buf) + "'");
            return new LinkedList<ByteBuffer>();
        }

        List<ByteBuffer> l = new LinkedList<ByteBuffer>();
        ByteBuffer out = ByteBuffer.allocate(buf.remaining());
        l.add(out);

        while (buf.hasRemaining()) {
            while (QUANTUM_SIZE > quantumIdx && buf.hasRemaining()) {
                char c = (char)buf.get();
                byte b = TEBAHPLA.length > c ? TEBAHPLA[c] : -1;
                if (0 <= b) {
                    quantum[quantumIdx++] = b;
                } else if ('=' == c) {
                    quantum[quantumIdx++] = 0;
                    equalCount++;
                } else if (CR != c && LF != c && SP != c && HT != c)  {
                    logger.warn("ignoring char: 0x" + Integer.toHexString(c));
                }
            }

            if (QUANTUM_SIZE == quantumIdx) {
                quantumIdx = 0;

                int q = (quantum[0] << 18) & 0xFC0000;
                q |= (quantum[1] << 12)    & 0x03F000;
                q |= (quantum[2] << 6)     & 0x000FC0;
                q |= quantum[3]            & 0x00003F;

                out.put((byte)(q >>> 16 & 0xFF));

                if (2 > equalCount) {
                    out.put((byte)(q >>> 8 & 0xFF));
                }

                if (1 > equalCount) {
                    out.put((byte)(q & 0xFF));
                }

                if (0 < equalCount) {
                    endOfInput = true;
                }
            }
        }

        out.flip();

        return l;
    }
}

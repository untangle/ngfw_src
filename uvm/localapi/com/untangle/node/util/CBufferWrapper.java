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

import java.lang.*;
import java.nio.*;

/* !!!IMPORTANT!!! (portability issues)
 * CBufferWrapper implements CharSequence
 * to handle read-only access to characters
 * (based on arbitrary, single-byte charsets)
 * stored in given ByteByffer.
 * Each single-byte charset defines alphanumeric characters
 * on single code page and
 * this alphanumeric code page is derived
 * from US-ASCII (ANSI_X3.4-1968) charset.
 * (e.g., charset's code/value for character 'a' equals
 *  US-ASCII's code/value for character 'a', etc.)
 * - alphanumeric characters include (a-z,A-Z,0-9,punct,special chars
 *   like carriage return, line feed, etc)
 * - for Latin-based charsets, this assumption regarding code page is true
 * - for other charsets, this assumption regarding code page is not guaranteed
 * CBufferWrapper does not maintain any independent state info
 * for its underlying ByteBuffer.
 *
 * Unicode is double-byte charset that
 * reuses alphanumeric code page of US-ASCII charset.
 *
 * Given that:
 * Java defines char datatype (double-byte) as Unicode character.
 * Channels/Line buffers operate on ByteByffers
 * (based on (single) byte datatype)
 * to accept/return data.
 * -> We assume that data,
 *    that we handle through channels,
 *    are based on single-byte, Latin-based charsets and
 *    thus, we can use CBufferWrapper to wrap ByteBuffer.
 *    (This saves us the extra step of copying (converting) byte data
 *     to/from char form.)
 *    (We cannot update CBufferWrapper objects and
 *     must not feed CBufferWrapper objects to any method that
 *     tries to do so since
 *     CBufferWrapper maintains backing data in byte form and
 *     not in char form while
 *     the update methods expect data in char form.)
 * -> If we get data (through channels) that
 *    are based on multi-byte charsets,
 *    we must use Charset decoders and encoders
 *    to copy byte data to/from char form and
 *    cannot use CBufferWrapper.
 */

/* CBufferWrapper doesn't maintain any ByteBuffer state info
 * so it's not necessary to renew CBufferWrapper
 * after updating contents of ByteBuffer.
 * (CBufferWrapper only has to be renewed
 *  when we want to replace current ByteBuffer with new ByteBuffer.)
 */
public class CBufferWrapper implements CharSequence
{
    /* constants */

    /* class variables */

    /* instance variables */
    ByteBuffer zSrcByteBuffer;
    ByteBuffer zByteBuffer;

    /* constructors */
    private CBufferWrapper(ByteBuffer zSrcByteBuffer, ByteBuffer zByteBuffer)
    {
        this.zSrcByteBuffer = zSrcByteBuffer;
        this.zByteBuffer = zByteBuffer;
    }

    public CBufferWrapper(ByteBuffer zByteBuffer)
    {
        this.zSrcByteBuffer = zByteBuffer;
        this.zByteBuffer = zByteBuffer;
    }

    /* public methods */
    public char charAt(int index) throws IndexOutOfBoundsException
    {
        if (0 > index ||
            zByteBuffer.limit() < index)
            {
                throw new IndexOutOfBoundsException();
            }

        return (char) zByteBuffer.get(index);
    }

    public int length()
    {
        return zByteBuffer.position();
    }

    /* create slice from ByteBuffer (and not source ByteBuffer) */
    public CharSequence subSequence(int start, int end) throws IndexOutOfBoundsException
    {
        if (0 > start ||
            zByteBuffer.limit() < start ||
            0 > end ||
            zByteBuffer.limit() < end ||
            end < start)
            {
                throw new IndexOutOfBoundsException();
            }

        /* create snapshot of ByteBuffer state */
        int iPosition = zByteBuffer.position();
        int iLimit = zByteBuffer.limit();

        /* establish range of view to slice from ByteBuffer */
        zByteBuffer.position(start);
        zByteBuffer.limit(end);

        ByteBuffer zSlice = zByteBuffer.slice(); /* create view */
        /* set position to indicate that slice contains data */
        zSlice.position(zSlice.limit());

        /* restore ByteBuffer state */
        zByteBuffer.limit(iLimit);
        zByteBuffer.position(iPosition);

        /* keep source ByteBuffer for later reference */
        return new CBufferWrapper(zSrcByteBuffer, zSlice);
    }

    /* create string of ByteBuffer (and not of source ByteBuffer) */
    public String toString()
    {
        /* create snapshot of ByteBuffer state */
        int iPosition = zByteBuffer.position();
        int iLimit = zByteBuffer.limit();

        /* position indicates count of bytes with valid data */
        byte azCopyBytes[] = new byte[iPosition];
        /* rewind to get bytes from start (index 0) up to limit */
        zByteBuffer.rewind();
        zByteBuffer.get(azCopyBytes, 0, iPosition);

        /* restore ByteBuffer state */
        zByteBuffer.limit(iLimit);
        zByteBuffer.position(iPosition);

        /* creates new String from byte array
         * by decoding byte array with platform's default charset
         * - in other words, toString creates char array and
         *   decodes (= copies) bytes into chars
         * - another alternative is to create our own char array and
         *   copy bytes directly into chars (to avoid byte-char decode)
         */
        return new String(azCopyBytes);
    }

    public CBufferWrapper renew(ByteBuffer zByteBuffer)
    {
        this.zSrcByteBuffer = zByteBuffer;
        this.zByteBuffer = zByteBuffer;
        return this;
    }

    /* return source ByteBuffer */
    public ByteBuffer getSrc()
    {
        return zSrcByteBuffer;
    }

    /* return view of ByteBuffer */
    public ByteBuffer get()
    {
        return zByteBuffer;
    }

    /* private methods */
}

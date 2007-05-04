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

import java.text.CharacterIterator;

public final class AsciiCharBufferCharacterIterator implements CharacterIterator
{
    private AsciiCharBuffer buffer;
    private int begin;
    private int end;
    // invariant: begin <= pos <= end
    private int pos;

    public AsciiCharBufferCharacterIterator(AsciiCharBuffer buffer)
    {
        this(buffer, 0, buffer.limit(), buffer.position());
    }

    public AsciiCharBufferCharacterIterator(AsciiCharBuffer buffer, int begin, int end, int pos) {
        if (buffer == null)
            throw new NullPointerException();
        this.buffer = buffer;

        if (begin < 0 || begin > end || end > buffer.limit())
            throw new IllegalArgumentException("Invalid substring range");

        if (pos < begin || pos > end)
            throw new IllegalArgumentException("Invalid position");

        this.begin = begin;
        this.end = end;
        this.pos = pos;
    }

    public char first()
    {
        pos = begin;
        return current();
    }

    public char last()
    {
        if (end != begin) {
            pos = end - 1;
        } else {
            pos = end;
        }
        return current();
    }

    public char setIndex(int p)
    {
        if (p < begin || p > end)
            throw new IllegalArgumentException("Invalid index");
        pos = p;
        return current();
    }

    public char current()
    {
        if (pos >= begin && pos < end) {
            return buffer.get(pos);
        }
        else {
            return DONE;
        }
    }

    public char next()
    {
        if (pos < end - 1) {
            pos++;
            return buffer.get(pos);
        }
        else {
            pos = end;
            return DONE;
        }
    }

    public char previous()
    {
        if (pos > begin) {
            pos--;
            return buffer.get(pos);
        }
        else {
            return DONE;
        }
    }

    public int getBeginIndex()
    {
        return begin;
    }

    public int getEndIndex()
    {
        return end;
    }

    public int getIndex()
    {
        return pos;
    }

    public Object clone()
    {
        try {
            AsciiCharBufferCharacterIterator other = (AsciiCharBufferCharacterIterator) super.clone();
            return other;
        } catch (CloneNotSupportedException e) {
            throw new Error();
        }
    }
}

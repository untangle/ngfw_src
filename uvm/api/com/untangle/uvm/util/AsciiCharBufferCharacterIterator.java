/**
 * $Id$
 */
package com.untangle.uvm.util;

import java.text.CharacterIterator;

/**
 * Iterator for AsciiCharBufferCharacterIterator
 */
public final class AsciiCharBufferCharacterIterator implements CharacterIterator
{
    private AsciiCharBuffer buffer;
    private int begin;
    private int end;
    // invariant: begin <= pos <= end
    private int pos;

    /**
     * Initialize instance of AsciiCharBufferCharacterIterator.
     * @param  buffer AsciiCharBuffer to use.
     * @return        Instance of AsciiCharBufferCharacterIterator.
     */
    public AsciiCharBufferCharacterIterator(AsciiCharBuffer buffer)
    {
        this(buffer, 0, buffer.limit(), buffer.position());
    }

    /**
     * Initialize instance of AsciiCharBufferCharacterIterator.
     * @param  buffer AsciiCharBuffer to use.
     * @param  begin  integer of start of range.
     * @param  end    integer of end of range.
     * @param  pos    integer of current position.
     * @return        Instance of AsciiCharBufferCharacterIterator.
     */
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

    /**
     * Move position to first of range, return that character.
     * @return char at that position.
     */
    public char first()
    {
        pos = begin;
        return current();
    }

    /**
     * Move position to end of range, return that character.
     * @return char at that position.
     */
    public char last()
    {
        if (end != begin) {
            pos = end - 1;
        } else {
            pos = end;
        }
        return current();
    }

    /**
     * Specify the index position.
     * @param  p integer of position
     * @return   char at that position
     */
    public char setIndex(int p)
    {
        if (p < begin || p > end)
            throw new IllegalArgumentException("Invalid index");
        pos = p;
        return current();
    }

    /**
     * Return char at current position.
     * @return char at current position.
     */
    public char current()
    {
        if (pos >= begin && pos < end) {
            return buffer.get(pos);
        }
        else {
            return DONE;
        }
    }

    /**
     * Increment position, return char at that positon.
     * @return return char at new next position.
     */
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

    /**
     * Increment position, return char at that positon.
     * @return return char at new previus position.
     */
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

    /**
     * Return beginning index.
     * @return integer of beginning index.
     */
    public int getBeginIndex()
    {
        return begin;
    }

    /**
     * Return ending index.
     * @return integer of ending index.
     */
    public int getEndIndex()
    {
        return end;
    }

    /**
     * Return current position.
     * @return integer of current position.
     */
    public int getIndex()
    {
        return pos;
    }

    /**
     * Copy this ieterator.
     * @return Copied instance of this instance.
     */
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

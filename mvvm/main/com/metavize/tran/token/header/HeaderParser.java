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

package com.metavize.tran.token.header;

import static com.metavize.tran.util.Ascii.*;
import static com.metavize.tran.util.Rfc822Util.*;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import com.metavize.tran.token.ParseException;
import org.apache.log4j.Logger;

public class HeaderParser<T extends Header>
{
    private final T header;
    private final boolean failOnError;
    private final List<Field> rejectedFields = new LinkedList<Field>();
    private final Logger logger = Logger.getLogger(HeaderParser.class);


    // constructors -----------------------------------------------------------

    public HeaderParser(T header, boolean failOnError)
    {
        this.header = header;
        this.failOnError = failOnError;
    }

    public HeaderParser(T header)
    {
        this.header = header;
        this.failOnError = false;
    }

    // public methods ---------------------------------------------------------

    public boolean isCompleteHeader(ByteBuffer buf)
    {
        for (int i = buf.position(); i < buf.limit() - 4; i++) {
            if (CR == buf.get(i) && LF == buf.get(i + 1)
                && CR == buf.get(i + 2) && LF == buf.get(i + 3)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Parses a complete header from the buffer. Leaves the position
     * after the CRLF that separates the Header from the body.
     *
     * XXX add incremental parsing?
     *
     * @param buf buffer containing Header plus CRLF
     * @exception ParseException if an error occurs
     */
    public T parse(ByteBuffer buf) throws ParseException
    {
        while (2 <= buf.remaining()) {
            int p = buf.position();
            if (CR == buf.get(p) && LF == buf.get(p + 1)) {
                buf.position(p + 2);
                break;
            } else {
                Field field = parseField(buf);
                logger.debug("added field: " + field);
                try {
                    header.addField(field);
                } catch (IllegalFieldException exn) {
                    if (failOnError) {
                        throw new ParseException(exn);
                    } else {
                        rejectedFields.add(field);
                    }
                }
            }
        }

        return header;
    }

    public T getHeader()
    {
        return header;
    }

    public List<Field> getRejectedFields()
    {
        return new LinkedList(rejectedFields);
    }

    // private methods --------------------------------------------------------

    public Field parseField(ByteBuffer buf) throws ParseException
    {
        logger.debug("parse FIELD: " + buf);

        // key
        StringBuilder sb = new StringBuilder();
        while (buf.hasRemaining()) {
            char c = (char)buf.get();
            if (COLON == c) {
                break;
            } else if (33 > c || 126 < c) { /* RFC 2822 2.2 */
                throw new ParseException("illegal character: " + (byte)c);
            } else {
                sb.append(c);
            }
        }
        String key = sb.toString();
        logger.debug("GOT KEY: " + key);

        // eat space
        while (buf.hasRemaining()) {
            char c = (char)buf.get();
            if (SP != c) {
                buf.position(buf.position() - 1);
                break;
            }
        }

        // value
        sb = new StringBuilder();
        while (buf.hasRemaining()) {
            char c = (char)buf.get();
            if (127 < c) { /* RFC 2822 2.2 */
                // XXX setting for strictness
                // XXX setting to escape?
                sb.append(c);
                //throw new ParseException("non-ASCII character: " + (byte)c);
            } else if (LF == c) {
                throw new ParseException("LF without CR");
            } else if (CR == c) {
                if (!buf.hasRemaining()) {
                    throw new ParseException("unexpected end of data");
                } else if (LF != buf.get()) {
                    throw new ParseException("expected LF");
                } else {
                    if (eatSpace(buf)) { /* folded */
                        sb.append(SP);
                    } else {
                        break;
                    }
                }
            } else {
                sb.append(c);
            }
        }
        String value = sb.toString();
        logger.debug("GOT VALUE: " + value);

        return new Field(key, value);
    }
}

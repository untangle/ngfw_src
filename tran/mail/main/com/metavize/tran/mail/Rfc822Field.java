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
import static com.metavize.tran.mail.Rfc822Util.*;

import java.nio.ByteBuffer;

import com.metavize.tran.token.ParseException;
import org.apache.log4j.Logger;

public class Rfc822Field
{
    private String key;
    private String value;

    // constructors -----------------------------------------------------------

    public Rfc822Field(String key, String value)
    {
        if (null == key || null == value) {
            throw new IllegalArgumentException("null argument");
        }

        this.key = key;
        this.value = value;
    }

    // static factories -------------------------------------------------------

    public static Rfc822Field parse(ByteBuffer buf) throws ParseException
    {
        Logger logger = Logger.getLogger(Rfc822Field.class);

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

        return new Rfc822Field(key, value);
    }

    // accessors --------------------------------------------------------------

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return key + ": " + value;
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof Rfc822Field)) {
            return false;
        }

        Rfc822Field f = (Rfc822Field)o;
        return key.equals(f.key) && value.equals(f.value);
    }

    public int hashCode()
    {
        int result = 17;
        result = 37 * result + key.hashCode();
        result = 37 * result + value.hashCode();
        return result;
    }
}

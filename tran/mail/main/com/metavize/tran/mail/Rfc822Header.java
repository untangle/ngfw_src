/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Header.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.tran.mail;

import static com.metavize.tran.util.Ascii.*;
import static com.metavize.tran.util.BufferUtil.*;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import javax.mail.internet.ContentType;

import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.Token;
import org.apache.log4j.Logger;

/**
 * Holds an RFC 822 header, as used by HTTP and SMTP.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class Rfc822Header implements Token
{
    private final List<Rfc822Field> fields = new LinkedList<Rfc822Field>();
    private final Logger logger = Logger.getLogger(Rfc822Header.class);

    // constructors -----------------------------------------------------------

    public Rfc822Header() { }

    // static factories -------------------------------------------------------

    public static Rfc822Header parse(ByteBuffer buf) throws ParseException
    {
        Logger logger = Logger.getLogger(Rfc822Header.class);

        logger.debug("parse HEADER: " + buf);

        Rfc822Header header = new Rfc822Header();

        while (buf.hasRemaining()) {
            if (startsWith(buf, CRLF)) {
                break;
            } else {
                Rfc822Field field = Rfc822Field.parse(buf);
                logger.debug("added field: " + field);
                header.addField(field);
            }
        }

        logger.debug("HEADER done");

        return header;
    }

    // public methods ---------------------------------------------------------

    public List<Rfc822Field> getFields()
    {
        return fields;
    }

    public void addField(Rfc822Field field)
    {
        fields.add(field);
    }

    public void addField(String key, String value)
    {
        Rfc822Field f = new Rfc822Field(key, value);
        fields.add(f);
    }

    public void setField(String key, String value)
    {
        Rfc822Field f = getField(key);
        if (null == f) {
            f = new Rfc822Field(key, value);
            fields.add(f);
        } else {
            f.setValue(value);
        }
    }

    public Rfc822Field getField(String key)
    {
        for (Rfc822Field f : fields) {
            if (key.equalsIgnoreCase(f.getKey())) {
                return f;
            }
        }

        return null;
    }

    public ContentType getContentType() throws ParseException
    {
        Rfc822Field f = getField("Content-Type");

        ContentType ct = null;
        if (null != f) {
            try {
                ct = new ContentType(f.getValue());
            } catch (javax.mail.internet.ParseException exn) {
                logger.warn("ignoring Content-Type: " + f.getValue());
                return null;
            }
        }

        return ct;
    }

    public String getContentTransferEncoding()
    {
        Rfc822Field f = getField("Content-Transfer-Encoding");

        return null == f ? null : f.getValue();
    }

    public String getMimeVersion()
    {
        Rfc822Field f = getField("MIME-Version");

        return null == f ? null : f.getValue(); // XXX normalized value
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        int l = 2; // final CRLF
        for (Rfc822Field f : fields) {
            l += f.getKey().length();
            l += f.getValue().length();
            l += 4; // COLON SP and CRLF
        }

        ByteBuffer buf = ByteBuffer.allocate(l);

        for (Rfc822Field f : fields) {
            buf.put(f.getKey().getBytes());
            buf.put((byte)COLON);
            buf.put((byte)SP);
            buf.put(f.getValue().getBytes());
            buf.put((byte)CR);
            buf.put((byte)LF);
        }

        buf.put((byte)CR);
        buf.put((byte)LF);

        buf.flip();

        return buf;
    }
}

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

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class FieldStore
{
    private final List<Field> fields = new LinkedList<Field>();

    // constructors -----------------------------------------------------------

    public FieldStore() { }

    // public methods ---------------------------------------------------------

    public void add(Field field)
    {
        fields.add(field);
    }

    public void add(String key, String value)
    {
        fields.add(new Field(key, value));
    }

    public void setField(String key, String value)
    {
        for (ListIterator<Field> i = fields.listIterator(); i.hasNext(); ) {
            Field f = i.next();
            if (f.getKey().equalsIgnoreCase(key)) {
                i.set(new Field(key, value));
                return;
            }
        }

        fields.add(new Field(key, value));
    }

    // XXX perhaps we should try to preserve original folding instead
    // of refolding?
    public ByteBuffer getBytes()
    {
        int l = 2; // final CRLF
        for (Field f : fields) {
            l += f.getKey().length();
            l += f.getValue().length();
            l += 4; // COLON SP and CRLF
        }
        l = l + l / 4;

        ByteBuffer buf = ByteBuffer.allocate(l);

        for (Field f : fields) {
            String key = f.getKey();
            buf.put(key.getBytes());
            buf.put((byte)COLON);
            buf.put((byte)SP);
            l = key.length() + 2;

            byte[] value = f.getValue().getBytes();
            for (int i = 0; i < value.length; i++) {
                int j = nextSpace(value, i);
                int offset = j - i;

                if (78 < l + offset) {
                    buf.put((byte)CR);
                    buf.put((byte)LF);
                    buf.put((byte)HTAB);
                    l = 1;
                }

                buf.put(value, i, offset);
                i += offset;
                l += offset;

                if (78 > l && i < value.length) {
                    buf.put((byte)SP);
                }
            }
            buf.put((byte)CR);
            buf.put((byte)LF);
        }

        buf.put((byte)CR);
        buf.put((byte)LF);

        buf.flip();

        return buf;
    }

    private int nextSpace(byte[] str, int o)
    {
        for (int i = o ; i < str.length; i++) {
            if (SP == str[i]) {
                return i;
            }
        }

        return str.length;
    }
}

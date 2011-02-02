/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.type;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import com.untangle.node.util.UriUtil;

/**
 * Hibernate <code>UserType</code> for persisting
 * <code>URI</code> objects.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class UriUserType implements UserType
{
    // How big a varchar() do we get for default String fields.  This
    // should be elsewhere. XXX
    public static final int DEFAULT_STRING_SIZE = 255;

    private static final int[] SQL_TYPES = { Types.VARCHAR };
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    public int[] sqlTypes() { return SQL_TYPES; }
    public Class<URI> returnedClass() { return URI.class; }
    public boolean equals(Object x, Object y) { return x.equals(y); }
    public Object deepCopy(Object value) { return value; }
    public boolean isMutable() { return false; }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
        throws HibernateException, SQLException
    {
        String name = rs.getString(names[0]);

        if (rs.wasNull()) {
            return null;
        } else {
            // could have been truncated in middle of escape
            if (1 <= name.length() && '%' == name.charAt(name.length() - 1)) {
                name = name.substring(0, name.length() - 1);
            } else if (2 <= name.length() && '%' == name.charAt(name.length() - 2)) {
                name = UriUtil.escapeUri(name.substring(0, name.length() - 2));
            }
            try {
                return new URI(name);
            } catch (URISyntaxException exn) {
                throw new HibernateException(exn);
            }
        }
    }

    public void nullSafeSet(PreparedStatement ps, Object v, int i)
        throws HibernateException, SQLException
    {
        if (null == v) {
            ps.setNull(i, Types.VARCHAR);
        } else {
            // XXX we don't know the column length (it might not be default)
            // XXX should we break uri's into multiple columns? just path?
            String s = v.toString();

            byte[] ba = new byte[DEFAULT_STRING_SIZE];
            ByteBuffer bb = ByteBuffer.wrap(ba);
            CharsetEncoder ce = UTF_8.newEncoder();
            ce.encode(CharBuffer.wrap(s), bb, true);
            ByteArrayInputStream bais = new ByteArrayInputStream(ba, 0, bb.position());
            ps.setBinaryStream(i, bais, bb.position());
        }
    }

    public Object replace(Object original, Object target, Object owner)
    {
        return original;
    }

    public Object assemble(Serializable cached, Object owner)
    {
        return deepCopy(cached);
    }

    public Serializable disassemble(Object value)
    {
        return (Serializable)deepCopy(value);
    }

    public int hashCode(Object o)
    {
        return o.hashCode();
    }
}

/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MimeTypeUserType.java,v 1.1 2005/01/12 10:59:57 amread Exp $
 */

package com.metavize.mvvm.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import com.metavize.mvvm.tran.MimeType;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.UserType;

public class MimeTypeUserType implements UserType
{
    private static final int[] SQL_TYPES = { Types.VARCHAR };

    public int[] sqlTypes() { return SQL_TYPES; }
    public Class returnedClass() { return MimeType.class; }
    public boolean equals(Object x, Object y) { return x.equals(y); }
    public Object deepCopy(Object value) { return value; }
    public boolean isMutable() { return false; }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
        throws HibernateException, SQLException
    {
        String name = rs.getString(names[0]);
        return rs.wasNull() ? null : new MimeType(name);
    }

    public void nullSafeSet(PreparedStatement ps, Object v, int i)
        throws HibernateException, SQLException
    {
        if (null == v) {
            ps.setNull(i, Types.VARCHAR);
        } else {
            ps.setString(i, v.toString());
        }
    }
}

/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: VScannerUserType.java,v 1.1 2005/02/05 04:47:58 cng Exp $
 */

package com.metavize.tran.email;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.UserType;

public class VScannerUserType implements UserType
{
    private static final int[] SQL_TYPES = { Types.CHAR };

    public int[] sqlTypes() { return SQL_TYPES; }
    public Class returnedClass() { return VScanner.class; }
    public boolean equals(Object x, Object y) { return x == y; }
    public Object deepCopy(Object value) { return value; }
    public boolean isMutable() { return false; }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
        throws HibernateException, SQLException
    {
        String s = rs.getString(names[0]);
        if (rs.wasNull() || 1 != s.length())
        {
            return null;
        }
        else
        {
            return VScanner.getInstance(s.charAt(0));
        }
    }

    public void nullSafeSet(PreparedStatement ps, Object v, int i)
        throws HibernateException, SQLException
    {
        if (null == v)
        {
            ps.setString(i, "0"); // 0 means no value/null
        }
        else
        {
            VScanner a = (VScanner)v;
            ps.setString(i, Character.toString(a.getKey())); /* safe for all ascii */
        }

        return;
    }
}

/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.httpblocker;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.UserType;

public class ReasonUserType implements UserType
{
    private static final int[] SQL_TYPES = { Types.CHAR };

    public int[] sqlTypes() { return SQL_TYPES; }
    public Class returnedClass() { return Reason.class; }
    public boolean equals(Object x, Object y) { return x == y; }
    public Object deepCopy(Object v) { return v; }
    public boolean isMutable() { return false; }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
        throws HibernateException, SQLException
    {
        String s = rs.getString(names[0]);
        if (rs.wasNull() || 1 != s.length()) {
            return null;
        } else {
            return Reason.getInstance(s.charAt(0));
        }
    }

    public void nullSafeSet(PreparedStatement ps, Object v, int i)
        throws HibernateException, SQLException
    {
        if (null == v) {
            // 0 means no value/null
            ps.setString(i, "0");
        } else {
            Reason r = (Reason)v;
            if (Reason.NONE == r) {
                throw new IllegalStateException("i told you");
            }
            ps.setString(i, Character.toString(r.getKey()));
        }
    }
}


/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: InetAddressUserType.java,v 1.2 2005/02/01 09:59:22 amread Exp $
 */

package com.metavize.mvvm.type;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.UserType;

public class InetAddressUserType implements UserType
{
    private static final int[] SQL_TYPES = { Types.VARCHAR };

    public int[] sqlTypes() { return SQL_TYPES; }
    public Class returnedClass() { return InetAddress.class; }

    public boolean equals(Object x, Object y)
    {
        if (x == y) { return true; }
        if (x == null || y == null) { return false; }
        return x.equals(y);
    }

    public Object deepCopy(Object value) { return value; }
    public boolean isMutable() { return false; }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
        throws HibernateException, SQLException
    {
        if (rs.wasNull()) { return null; }

        String name = rs.getString(names[0]);
        try {
            return InetAddress.getByName(name);
        } catch (UnknownHostException exn) {
            throw new HibernateException(exn);
        }
    }

    public void nullSafeSet(PreparedStatement ps, Object v, int i)
        throws HibernateException, SQLException
    {
        if (null == v) {
            ps.setNull(i, Types.VARCHAR);
        } else {
            InetAddress addr = (InetAddress)v;
            ps.setString(i, addr.getHostAddress());
        }
    }
}

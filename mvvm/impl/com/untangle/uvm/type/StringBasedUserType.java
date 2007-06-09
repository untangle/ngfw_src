/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

// NOTE: for mutable objects, please at least override the deepCopy
public abstract class StringBasedUserType implements UserType, Serializable
{
    private static final int[] SQL_TYPES = { Types.VARCHAR };

    // abstract methods -------------------------------------------------------
    public abstract Class returnedClass();

    protected abstract String userTypeToString(Object v);
    protected abstract Object createUserType(String val) throws Exception;

    // UserType methods -------------------------------------------------------

    public int[] sqlTypes() { return SQL_TYPES; }

    public boolean equals(Object x, Object y)
    {
        if (x == y) return true;

        if (x == null || y == null) return false;

        return x.equals(y);
    }

    public Object deepCopy(Object value)
    {
        return value;
    }

    public boolean isMutable()
    {
        return false;
    }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
        throws HibernateException, SQLException
    {
        String name = rs.getString(names[0]);
        Object val;

        try {
            val = rs.wasNull() ? null : createUserType(name);
        } catch (Exception exn) {
            throw new HibernateException(exn);
        }

        return val;
    }

    public void nullSafeSet(PreparedStatement ps, Object v, int i)
        throws HibernateException, SQLException
    {
        if (null == v) {
            ps.setNull(i, Types.VARCHAR);
        } else {
            ps.setString(i, userTypeToString(v));
        }
    }

    public Object replace(Object original, Object target, Object owner)
    {
        return deepCopy(original);
    }

    public Object assemble(Serializable cached, Object owner)
    {
        return deepCopy(cached);
    }

    public Serializable disassemble(Object value)
    {
        return (Serializable)deepCopy(value);
    }

    public int hashCode(Object x)
    {
        /* Use the hash code of the string because two objects that    *
         * translate to the same string should be equivalent, if it    *
         * doesn't then, 'createUserType( userTypeToString( x )) == x' *
         * wouldn't always work, and that would lead to problems.      */
        return userTypeToString(x).hashCode();
    }
}

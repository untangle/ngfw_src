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

package com.untangle.uvm.type;

import java.awt.Color;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;

public class ColorUserType implements CompositeUserType
{
    public Class returnedClass() { return Color.class; }

    public boolean equals(Object x, Object y)
    {
        if (x == y) { return true; }
        if (null == x || null == y) { return false; }
        return x.equals(y);
    }

    public Object deepCopy(Object v) {
        Color c = (Color)v;

        return new Color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    public boolean isMutable() { return false; }

    public Object nullSafeGet(ResultSet rs, String[] names,
                              SessionImplementor si, Object owner)
        throws HibernateException, SQLException
    {
        if (rs.wasNull()) { return null; }
        int r = rs.getInt(names[0]);
        int g = rs.getInt(names[1]);
        int b = rs.getInt(names[2]);
        int a = rs.getInt(names[3]);

        return new Color(r, g, b, a);
    }

    public void nullSafeSet(PreparedStatement ps, Object v, int i,
                            SessionImplementor si)
        throws HibernateException, SQLException
    {
        if (null == v) {
            ps.setNull(i, Types.INTEGER);
            ps.setNull(++i, Types.INTEGER);
            ps.setNull(++i, Types.INTEGER);
            ps.setNull(++i, Types.INTEGER);
        } else {
            Color c = (Color)v;
            ps.setInt(i, c.getRed());
            ps.setInt(++i, c.getGreen());
            ps.setInt(++i, c.getBlue());
            ps.setInt(++i, c.getAlpha());
        }
    }

    public String[] getPropertyNames()
    {
        return new String[] { "red", "green", "blue", "alpha" };
    }

    public Type[] getPropertyTypes()
    {
        return new Type[] { Hibernate.INTEGER, Hibernate.INTEGER,
                            Hibernate.INTEGER, Hibernate.INTEGER };
    }

    public Object getPropertyValue(Object o, int i)
        throws HibernateException
    {
        Color c = (Color)o;
        switch (i) {
        case 0: return new Integer(c.getRed());
        case 1: return new Integer(c.getGreen());
        case 2: return new Integer(c.getBlue());
        case 3: return new Integer(c.getAlpha());
        default: throw new IllegalArgumentException("bad index: " + i);
        }
    }

    public void setPropertyValue(Object o, int i, Object v)
        throws HibernateException
    {
        throw new UnsupportedOperationException("immutable");
    }

    public Object assemble(Serializable cached, SessionImplementor session,
                           Object owner)
        throws HibernateException
    {
        return cached;
    }

    public Serializable disassemble(Object v, SessionImplementor session)
        throws HibernateException
    {
        return (Serializable)v;
    }

    public Object replace(Object original, Object target,
                          SessionImplementor session, Object owner)
    {
        return original;
    }

    public int hashCode(Object x)
    {
        return x.hashCode();
    }
}

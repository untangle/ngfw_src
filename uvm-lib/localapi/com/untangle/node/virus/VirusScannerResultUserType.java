/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.virus;

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

public class VirusScannerResultUserType implements CompositeUserType
{
    public Class returnedClass() { return VirusScannerResult.class; }

    public boolean equals(Object x, Object y)
    {
        if (x == y) { return true; }
        if (null == x || null == y) { return false; }
        return x.equals(y);
    }

    public Object deepCopy(Object v)
    {
        return v;
    }

    public boolean isMutable() { return false; }

    public Object nullSafeGet(ResultSet rs, String[] names,
                              SessionImplementor si, Object owner)
        throws HibernateException, SQLException
    {
        boolean clean = rs.getBoolean(names[0]);
        if (rs.wasNull()) { return null; }

        String virusName = rs.getString(names[1]);
        if (rs.wasNull()) { return null; }

        boolean virusCleaned  = rs.getBoolean(names[2]);
        if (rs.wasNull()) { return null; }

        return new VirusScannerResult(clean, virusName, virusCleaned);
    }

    public void nullSafeSet(PreparedStatement ps, Object v, int i,
                            SessionImplementor si)
        throws HibernateException, SQLException
    {
        if (null == v) {
            ps.setNull(i, Types.BOOLEAN);
            ps.setNull(++i, Types.VARCHAR);
            ps.setNull(++i, Types.BOOLEAN);
        } else {
            VirusScannerResult result = (VirusScannerResult)v;
            ps.setBoolean(i, result.isClean());
            ps.setString(++i, result.getVirusName());
            ps.setBoolean(++i, result.isVirusCleaned());
        }
    }

    public String[] getPropertyNames()
    {
        return new String[] { "clean", "virusName", "virusCleaned", };
    }

    public Type[] getPropertyTypes()
    {
        return new Type[] { Hibernate.BOOLEAN, Hibernate.STRING,
                            Hibernate.BOOLEAN };
    }

    public Object getPropertyValue(Object o, int i)
        throws HibernateException
    {
        VirusScannerResult result = (VirusScannerResult)o;
        switch (i) {
        case 0: return new Boolean(result.isClean());
        case 1: return new String(result.getVirusName());
        case 2: return new Boolean(result.isVirusCleaned());
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

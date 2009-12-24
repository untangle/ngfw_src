package com.untangle.node.cpd;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import com.untangle.node.cpd.CPDSettings.AuthenticationType;

public class AuthenticationTypeUserType implements UserType
{
	private static final int[] SQL_TYPES = { Types.VARCHAR };

	    public int[] sqlTypes() { return SQL_TYPES; }
	    public Class<AuthenticationType> returnedClass() { return AuthenticationType.class; }
	    public boolean equals(Object x, Object y) { return x == y; }
	    public Object deepCopy(Object v) { return v; }
	    public boolean isMutable() { return false; }

	    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
	        throws HibernateException, SQLException
	    {
	        String s = rs.getString(names[0]);
	        if (rs.wasNull() || 0 == s.length()) {
	            return null;
	        } else {
	            return AuthenticationType.valueOf(s);
	        }
	    }

	    public void nullSafeSet(PreparedStatement ps, Object v, int i)
	        throws HibernateException, SQLException
	    {
	        if (null == v) {
	            // 0 means no value/null
	            ps.setString(i, "");
	        } else {
	            AuthenticationType r = (AuthenticationType)v;
	            ps.setString(i, r.toString());
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

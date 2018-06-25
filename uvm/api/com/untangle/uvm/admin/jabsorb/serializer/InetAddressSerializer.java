/**
 * $Id$
 */
package com.untangle.uvm.admin.jabsorb.serializer;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

import org.jabsorb.serializer.AbstractSerializer;
import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.ObjectMatch;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;

/**
 * Serializer for InetAddress
 */
@SuppressWarnings({"serial","unchecked","rawtypes"})
public class InetAddressSerializer extends AbstractSerializer
{
    /**
     * Classes that this can serialise.
     */
    private static Class[] _serializableClasses = new Class[] { InetAddress.class, Inet4Address.class, Inet6Address.class };

    /**
     * Classes that this can serialise to.
     */
    private static Class[] _JSONClasses = new Class[] { String.class };

    /**
     * @see org.jabsorb.serializer.Serializer#getJSONClasses()
     * @return Class[]
     */
    public Class[] getJSONClasses()
    {
        return _JSONClasses;
    }

    /**
     * @see org.jabsorb.serializer.Serializer#getSerializableClasses()
     * @return Class[]
     */
    public Class[] getSerializableClasses()
    {
        return _serializableClasses;
    }
    
    /**
     * @see org.jabsorb.serializer.Serializer#marshall(org.jabsorb.serializer.SerializerState, java.lang.Object, java.lang.Object)
     * @param state
     * @param p
     * @param o
     * @return Object
     * @throws MarshallException
     */
    public Object marshall(SerializerState state, Object p, Object o)
        throws MarshallException
    {
        if( o == null ) {
            return "";
        } else if (o instanceof InetAddress) {
            return ((InetAddress)o).getHostAddress();
        }
        
        return null;
    }

    /**
     * @see org.jabsorb.serializer.Serializer#tryUnmarshall(org.jabsorb.serializer.SerializerState, java.lang.Class, java.lang.Object)
     * @param state
     * @param clazz
     * @param json
     * @return ObjectMatch
     * @throws UnmarshallException
     */
    public ObjectMatch tryUnmarshall(SerializerState state, Class clazz, Object json)
        throws UnmarshallException
    {

        state.setSerialized(json, ObjectMatch.OKAY);
        return ObjectMatch.OKAY;
    }

    /**
     * @see org.jabsorb.serializer.Serializer#unmarshall(org.jabsorb.serializer.SerializerState, java.lang.Class, java.lang.Object)
     * @param state
     * @param clazz
     * @param json
     * @return Object
     * @throws UnmarshallException
     */
    public Object unmarshall(SerializerState state, Class clazz, Object json)
        throws UnmarshallException
    {
        Object returnValue = null;
        String val = json instanceof String ? (String) json : json.toString();
        try {
            if ("".equals(val)) {
                returnValue = null;
                state.setSerialized(json, returnValue);
                return returnValue;
            }
            else
                returnValue = InetAddress.getByName(val);
        } catch (Exception e) {
            throw new UnmarshallException("Invalid \"InetAddress\" specified:" + val);
        }
        
        if (returnValue == null) {
            throw new UnmarshallException("invalid class " + clazz);
        }

        state.setSerialized(json, returnValue);
        return returnValue;
    }
}

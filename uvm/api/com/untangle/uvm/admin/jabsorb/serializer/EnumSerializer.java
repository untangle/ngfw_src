/**
 * $Id$
 */
package com.untangle.uvm.admin.jabsorb.serializer;

import org.jabsorb.serializer.AbstractSerializer;
import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.ObjectMatch;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;

/**
 * Serializer for enums
 */
@SuppressWarnings({"serial","unchecked","rawtypes"})
public class EnumSerializer extends AbstractSerializer
{
    /**
     * Classes that this can serialise to.
     */
    private static Class[] _JSONClasses = new Class[] { String.class };

    /**
     * Classes that this can serialise.
     */
    private static Class[] _serializableClasses = new Class[0];

    /**
     * True if this serializer can serialize the specified class
     * @param clazz
     * @param jsonClazz
     * @return bool
     */
    @Override
    public boolean canSerialize(Class clazz, Class jsonClazz)
    {
        return clazz.isEnum();
    }

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
        if (o instanceof Enum) {
            return o.toString();
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
    public ObjectMatch tryUnmarshall(SerializerState state, Class clazz, Object json) throws UnmarshallException
    {

        //        Class classes[] = json.getClass().getClasses();
        //        for (int i = 0; i < classes.length; i++) {
        //            if (classes[i].isEnum()) {
        //                state.setSerialized(json, ObjectMatch.OKAY);
        //                return ObjectMatch.OKAY;
        //            }
        //        }

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
        String val = json instanceof String ? (String) json : json.toString();
        if (clazz.isEnum()) {
            return Enum.valueOf(clazz, val);
        }
        return null;
    }

}

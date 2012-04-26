package com.untangle.uvm.webui.jabsorb.serializer;

import org.jabsorb.serializer.AbstractSerializer;
import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.ObjectMatch;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;

import com.untangle.uvm.node.GlobMatcher;

@SuppressWarnings({"serial","unchecked"})
public class GlobMatcherSerializer extends AbstractSerializer
{
	/**
	 * Classes that this can serialize to.
	 */
	private static Class[] _JSONClasses = new Class[] { String.class };

	/**
	 * Classes that this can serialize.
	 */
	private static Class[] _serializableClasses = new Class[] { GlobMatcher.class };

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jabsorb.serializer.Serializer#getJSONClasses()
	 */
	public Class[] getJSONClasses()
    {
		return _JSONClasses;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jabsorb.serializer.Serializer#getSerializableClasses()
	 */
	public Class[] getSerializableClasses()
    {
		return _serializableClasses;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jabsorb.serializer.Serializer#marshall(org.jabsorb.serializer.SerializerState,
	 *      java.lang.Object, java.lang.Object)
	 */
	public Object marshall(SerializerState state, Object p, Object o)
			throws MarshallException
    {
		if (o instanceof GlobMatcher) {
			return ((GlobMatcher) o).toString();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jabsorb.serializer.Serializer#tryUnmarshall(org.jabsorb.serializer.SerializerState,
	 *      java.lang.Class, java.lang.Object)
	 */
	public ObjectMatch tryUnmarshall(SerializerState state, Class clazz,
			Object json) throws UnmarshallException
    {
		state.setSerialized(json, ObjectMatch.OKAY);
		return ObjectMatch.OKAY;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jabsorb.serializer.Serializer#unmarshall(org.jabsorb.serializer.SerializerState,
	 *      java.lang.Class, java.lang.Object)
	 */
	public Object unmarshall(SerializerState state, Class clazz, Object json)
			throws UnmarshallException
    {
        Object returnValue = null;
        String val = json instanceof String ? (String) json : json.toString();
        try {
            returnValue = new GlobMatcher(val);
        } catch (Exception e) {
            throw new UnmarshallException("Invalid \"interface\" specified:"+ val);
        }
        
        if (returnValue == null) {
            throw new UnmarshallException("invalid class " + clazz);
        }
        state.setSerialized(json, returnValue);
        return returnValue;
	}

}

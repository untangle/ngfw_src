package com.untangle.uvm.webui.jabsorb.serializer;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;

import org.hibernate.collection.PersistentSet;
import org.jabsorb.serializer.ObjectMatch;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;
import org.jabsorb.serializer.impl.SetSerializer;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Serialises persistent lists
 * 
 */
@SuppressWarnings({"serial","unchecked"})
public class ExtendedSetSerializer extends SetSerializer
{
    /**
     * Unique serialisation id.
     */

    /**
     * Classes that this can serialise.
     */
    private static Class[] _serializableClasses = new Class[] { PersistentSet.class };

    public Class[] getSerializableClasses()
    {
        return _serializableClasses;
    }

    /*
     * Very similar with the superclass's tryUnmarshall implementation; the difference is in checking the list type
     * @see org.jabsorb.serializer.impl.SetSerializer#tryUnmarshall(org.jabsorb.serializer.SerializerState, java.lang.Class, java.lang.Object)
     */
    public ObjectMatch tryUnmarshall(SerializerState state, Class clazz, Object o)
        throws UnmarshallException
    {
	    JSONObject jso = (JSONObject) o;
	    String java_class;
	    try
            {
                java_class = jso.getString("javaClass");
            }
	    catch (JSONException e)
            {
                throw new UnmarshallException("Could not read javaClass", e);
            }
	    if (java_class == null)
            {
                throw new UnmarshallException("no type hint");
            }
	    if (!java_class.equals("org.hibernate.collection.PersistentSet"))
            {
                throw new UnmarshallException("not a Set");
            }
	    JSONObject jsonset;
	    try
            {
                jsonset = jso.getJSONObject("set");
            }
	    catch (JSONException e)
            {
                throw new UnmarshallException("set missing", e);
            }

	    if (jsonset == null)
            {
                throw new UnmarshallException("set missing");
            }

	    ObjectMatch m = new ObjectMatch(-1);
	    state.setSerialized(o, m);
	    Iterator i = jsonset.keys();
	    String key = null;

	    try
            {
                while (i.hasNext())
                    {
                        key = (String) i.next();
                        m.setMismatch(ser.tryUnmarshall(state, null, jsonset.get(key)).max(m).getMismatch());
                    }
            }
	    catch (UnmarshallException e)
            {
                throw new UnmarshallException("key " + key + " " + e.getMessage(), e);
            }
	    catch (JSONException e)
            {
                throw new UnmarshallException("key " + key + " " + e.getMessage(), e);
            }
	    return m;
    }

    /*
     * Very similar with the superclass's tryUnmarshall implementation; the difference is in checking the list type
     * @see org.jabsorb.serializer.impl.ListSerializer#tryUnmarshall(org.jabsorb.serializer.SerializerState, java.lang.Class, java.lang.Object)
     */
    public Object unmarshall(SerializerState state, Class clazz, Object o)
        throws UnmarshallException
    {
        JSONObject jso = (JSONObject) o;
        String java_class;
        try
            {
                java_class = jso.getString("javaClass");
            }
        catch (JSONException e)
            {
                throw new UnmarshallException("Could not read javaClass", e);
            }
        if (java_class == null)
            {
                throw new UnmarshallException("no type hint");
            }

        AbstractSet abset = null;
        if (java_class.equals("org.hibernate.collection.PersistentSet"))
            {
                abset = new HashSet();
            }
        else
            {
                throw new UnmarshallException("not a Set");
            }
        JSONObject jsonset;
        try
            {
                jsonset = jso.getJSONObject("set");
            }
        catch (JSONException e)
            {
                throw new UnmarshallException("set missing", e);
            }

        if (jsonset == null)
            {
                throw new UnmarshallException("set missing");
            }

        Iterator i = jsonset.keys();
        String key = null;
        state.setSerialized(o, abset);
        try
            {
                while (i.hasNext())
                    {
                        key = (String) i.next();
                        Object setElement = jsonset.get(key);
                        abset.add(ser.unmarshall(state, null, setElement));
                    }
            }
        catch (UnmarshallException e)
            {
                throw new UnmarshallException("key " + i + e.getMessage(), e);
            }
        catch (JSONException e)
            {
                throw new UnmarshallException("key " + key + " " + e.getMessage(), e);
            }
        return abset;
    }

}

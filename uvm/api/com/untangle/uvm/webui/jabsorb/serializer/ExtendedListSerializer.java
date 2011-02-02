package com.untangle.uvm.webui.jabsorb.serializer;

import java.util.AbstractList;
import java.util.ArrayList;

import org.hibernate.collection.PersistentList;
import org.jabsorb.serializer.ObjectMatch;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;
import org.jabsorb.serializer.impl.ListSerializer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Serialises persistent lists
 * 
 */
@SuppressWarnings({"serial","unchecked"})
public class ExtendedListSerializer extends ListSerializer
{
    /**
     * Classes that this can serialise.
     */
    private static Class[] _serializableClasses = new Class[] { PersistentList.class };

    public Class[] getSerializableClasses()
    {
        return _serializableClasses;
    }

    /*
     * Very similar with the superclass's tryUnmarshall implementation; the difference is in checking the list type
     * @see org.jabsorb.serializer.impl.ListSerializer#tryUnmarshall(org.jabsorb.serializer.SerializerState, java.lang.Class, java.lang.Object)
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
        if (!java_class.equals("org.hibernate.collection.PersistentList") &&
    		!java_class.equals("java.util.Arrays$ArrayList"))
            {
                throw new UnmarshallException("not a List");
            }
        JSONArray jsonlist;
        try
            {
                jsonlist = jso.getJSONArray("list");
            }
        catch (JSONException e)
            {
                throw new UnmarshallException("Could not read list: " + e.getMessage(), e);
            }
        if (jsonlist == null)
            {
                throw new UnmarshallException("list missing");
            }
        int i = 0;
        ObjectMatch m = new ObjectMatch(-1);
        state.setSerialized(o, m);
        try
            {
                for (; i < jsonlist.length(); i++)
                    {
                        m.setMismatch(ser.tryUnmarshall(state, null, jsonlist.get(i)).max(m).getMismatch());
                    }
            }
        catch (UnmarshallException e)
            {
                throw new UnmarshallException("element " + i + " " + e.getMessage(), e);
            }
        catch (JSONException e)
            {
                throw new UnmarshallException("element " + i + " " + e.getMessage(), e);
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
        AbstractList al;
        if (java_class.equals("org.hibernate.collection.PersistentList") ||
    		java_class.equals("java.util.Arrays$ArrayList"))
            {
                al = new ArrayList();
            }
        else
            {
                throw new UnmarshallException("not a List");
            }

        JSONArray jsonlist;
        try
            {
                jsonlist = jso.getJSONArray("list");
            }
        catch (JSONException e)
            {
                throw new UnmarshallException("Could not read list: " + e.getMessage(), e);
            }
        if (jsonlist == null)
            {
                throw new UnmarshallException("list missing");
            }
        state.setSerialized(o, al);
        int i = 0;
        try
            {
                for (; i < jsonlist.length(); i++)
                    {
                        al.add(ser.unmarshall(state, null, jsonlist.get(i)));
                    }
            }
        catch (UnmarshallException e)
            {
                throw new UnmarshallException("element " + i + " " + e.getMessage(), e);
            }
        catch (JSONException e)
            {
                throw new UnmarshallException("element " + i + " " + e.getMessage(), e);
            }
        return al;
    }

}

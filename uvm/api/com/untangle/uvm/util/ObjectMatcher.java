/**
 * $Id$
 */
package com.untangle.uvm.util;

import org.jabsorb.serializer.MarshallingMode;
import org.jabsorb.serializer.MarshallingModeContext;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.uvm.UvmContextFactory;

/**
 * Utility class for parsing JSON strings and arrays into Java objects after bean validation.
 */
public class ObjectMatcher {
    /**
         * Parse the Json and check for bean equality
         * @param json  The JSON string to parse
         * @param clazz The class type to unmarshall the JSON into.
         * @return The parsed Java object.
         * @throws JSONException    If there is an error parsing the JSON.
         * @throws UnmarshallException If there is an error unmarshalling the JSON into the specified class.
     */
    @SuppressWarnings("unchecked")
    public static <T> T parseJson(String json, Class<T> clazz) throws JSONException, UnmarshallException {
        try {
            // MarshallingMode.STANDARD_REST is only applicable for v2 API calls,
            // rest of the backend serialization should still be done by MarshallingMode.JABSORB
            MarshallingModeContext.push(MarshallingMode.JABSORB);

            SerializerState state = new SerializerState();
            Object jsonObject = new JSONObject(json);
            UvmContextFactory.context().getSerializer().tryUnmarshall(state, clazz, jsonObject);
            return (T) UvmContextFactory.context().getSerializer().fromJSON(json);
        } catch (UnmarshallException e) {
            throw new UnmarshallException("Failed to parse JSON " + e.getMessage());
        } finally {
            MarshallingModeContext.pop();
        }
    }

    /**
         * Parse the JsonArray and check for bean equality
         * @param json The JSON array string to parse.
         * @param arrayClazz The class type of the array elements to unmarshall the JSON into.
         * @return The parsed array of Java objects.
         * @throws JSONException    If there is an error parsing the JSON.
         * @throws UnmarshallException If there is an error unmarshalling the JSON into the specified array class.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] parseJsonArray(String json, Class<T[]> arrayClazz) throws JSONException, UnmarshallException {
        if (json == null || json.isEmpty() || "[]".equals(json)) {
            return null;
        }

        try {
            // MarshallingMode.STANDARD_REST is only applicable for v2 API calls,
            // rest of the backend serialization should still be done by MarshallingMode.JABSORB
            MarshallingModeContext.push(MarshallingMode.JABSORB);
            SerializerState state = new SerializerState();
            Object jsonObject = new JSONArray(json);
            UvmContextFactory.context().getSerializer().tryUnmarshall(state, arrayClazz, jsonObject);
            return (T[]) UvmContextFactory.context().getSerializer().fromJSON(json);
        }catch (UnmarshallException e) {
            throw new UnmarshallException("Failed to parse JSON " + e.getMessage());
        } finally {
            MarshallingModeContext.pop();
        }
    }
}

package com.untangle.uvm.webui.jabsorb.serializer;

import org.jabsorb.serializer.AbstractSerializer;
import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.ObjectMatch;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.uvm.security.User;

public class UserSerializer extends AbstractSerializer
{
    private static final int PASSWORD_LENGTH = 24;

    /**
     * Classes that this can serialise.
     */
    private static final Class[] _serializableClasses = new Class[] { User.class };

    /**
     * Classes that this can serialise to.
     */
    private static final Class[] _JSONClasses = new Class[] { JSONObject.class };

    public Class[] getJSONClasses()
    {
        return _JSONClasses;
    }

    public Class[] getSerializableClasses() {
        return _serializableClasses;
    }
	
    /*
     * (non-Javadoc)
     * 
     * @see org.jabsorb.serializer.Serializer#marshall(org.jabsorb.serializer.SerializerState,
     *      java.lang.Object, java.lang.Object)
     */
    public Object marshall(SerializerState state, Object p, Object o)
        throws MarshallException {
		
        if( o == null ) {
            return "";
        } else if (o instanceof User) {
            User u = (User)o;
            JSONObject jsUser = new JSONObject();
            
            try {
                jsUser.put( "id", u.getId());
                jsUser.put( "name", u.getName());
                jsUser.put( "email", u.getEmail());
                jsUser.put( "login", u.getLogin());
                jsUser.put( "notes", u.getNotes());
                jsUser.put( "sendAlerts", u.getSendAlerts());
                jsUser.put( "readOnly", u.isReadOnly());
                jsUser.put( "javaClass", User.class.getName());
                
                /* Calculate the password */
                
                byte[] password = u.getPassword();
                JSONArray jsPassword = new JSONArray();
                for ( byte b : password ) jsPassword.put( b );
                
                jsUser.put( "password", jsPassword );
                
                return jsUser;
            } catch ( JSONException e ) {
                throw new MarshallException( "Unable to marshal data", e );
            }
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
                                     Object o) throws UnmarshallException
    {
        JSONObject js = (JSONObject)o;
        String javaClass;

        try {
            javaClass = js.getString( "javaClass" );
        } catch ( JSONException e ) {
            throw new UnmarshallException( "no type hint", e );
        }

        if ( javaClass == null ) throw new UnmarshallException( "no type hint" );

        if ( !User.class.getName().equals( javaClass )) throw new UnmarshallException( "not a User" );
        
        state.setSerialized(o, ObjectMatch.OKAY);
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
        User returnValue = null;
        JSONObject jsUser = (JSONObject)json;

        try {
            String login = jsUser.getString( "login" );
            byte[] password = new byte[PASSWORD_LENGTH];
            JSONArray jsPassword = (JSONArray)jsUser.get("password");
            if ( jsPassword.length() != PASSWORD_LENGTH ) {
                throw new UnmarshallException( "Invalid password length" );
            }
            for ( int c = 0 ; c < PASSWORD_LENGTH ; c++ ) password[c] = (byte)jsPassword.getInt( c );
            String name = jsUser.getString( "name" );
            boolean readOnly = jsUser.getBoolean( "readOnly" );
            returnValue = new User( login, password, name, readOnly );

            long id = jsUser.optLong( "id", -1 );
            if ( id != -1 ) returnValue.setId( id );

            returnValue.setEmail( jsUser.getString( "email" ));
            returnValue.setNotes( jsUser.getString( "notes" ));
            returnValue.setSendAlerts( jsUser.getBoolean( "sendAlerts" ));
        } catch ( JSONException e ) {
            throw new UnmarshallException( "Unable to unmarshall user", e );
        }
        
        return returnValue;
    }
}

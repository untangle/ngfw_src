/**
 * $Id: JsonClient.java,v 1.00 2013/02/25 13:38:55 dmorris Exp $
 */
package com.untangle.uvm.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;

@SuppressWarnings("serial")
public class JsonClient
{
    /* Number of concurrent threads running */
    private static final int DEFAULT_MAX_NUMBER_CONNECTIONS = 5;
    
    /* Timeout to actually establish a connection */
    private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 10000;

    /* Timeout to read data from the client */
    private static final int DEFAULT_SO_TIMEOUT_MS = 90000;
    
    public static final String RESPONSE_STATUS = "status";
    public static final String RESPONSE_MESSAGE = "message";
    public static final int STATUS_SUCCESS = 104;

    private static final JsonClient INSTANCE = new JsonClient();

    private final MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();

    private final Logger logger = Logger.getLogger(getClass());

    private JsonClient()
    {
        HttpConnectionManagerParams params = this.connectionManager.getParams();
        params.setMaxTotalConnections( DEFAULT_MAX_NUMBER_CONNECTIONS );
        params.setConnectionTimeout( DEFAULT_CONNECTION_TIMEOUT_MS );
        params.setSoTimeout( DEFAULT_SO_TIMEOUT_MS );
    }

    public static void main(String[] args) throws Exception
    {
        JSONObject object = getInstance().call( args[0], null, new JSONObject( args[1] ));

        System.out.println( "Returned: " + object.toString() + "\n" );
    }

    public JSONObject call( String url, JSONObject object ) throws ConnectionException
    {
        return call( url, "json_request", object );
    }

    public JSONObject call( String url, String param, JSONObject object ) throws ConnectionException
    {
        // Create an instance of HttpClient.
        HttpClient client = new HttpClient( this.connectionManager );

        if ( object == null ) object = new JSONObject();

        // Create a method instance.
        PostMethod method = new PostMethod( url );

        RequestEntity entity = null;
        if (( param == null ) || ( param.length() == 0 )) {
            entity = new ByteArrayRequestEntity( object.toString().getBytes(),
                                                 "application/json; charset=UTF-8");
        } else {
            Part parts[] = { new StringPart( param, object.toString()) };
            entity = new MultipartRequestEntity( parts, method.getParams());
        }

        method.setRequestEntity( entity );

        try {
            // Execute the method.
            int statusCode = client.executeMethod(method);

            if (statusCode != HttpStatus.SC_OK) throw new ConnectionException( "Server returned an error." );

            // Read the response body.
            byte[] responseBody = method.getResponseBody();

            // Deal with the response.
            // Use caution: ensure correct character encoding and is not binary data
            return new JSONObject( new String(responseBody));
        } catch (HttpException e) {
            throw new ConnectionException( "Protocol violation", e );
        } catch (IOException e) {
            throw new ConnectionException( "Transport Error", e );
        } catch (JSONException e ) {
            throw new ConnectionException( "Server return an invalid JSON string.", e );
        } finally {
            // Release the connection.
            method.releaseConnection();
        }
    }

    public static  JsonClient getInstance()
    {
        return INSTANCE;
    }

    public static class ConnectionException extends Exception
    {

        public ConnectionException( String message, Throwable throwable )
        {
            super( message, throwable );
        }

        public ConnectionException( String message )
        {
            super( message );
        }
    }
}

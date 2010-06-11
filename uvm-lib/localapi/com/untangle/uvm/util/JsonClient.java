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

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.networking.NetworkException;

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

    private final MultiThreadedHttpConnectionManager connectionManager =
        new MultiThreadedHttpConnectionManager();

    private static final String ALPACA_BASE_URL = "http://localhost:3000/alpaca/";
    private static final String ALPACA_NONCE_FILE = "/etc/untangle-net-alpaca/nonce";

    private final Logger logger = Logger.getLogger(getClass());

    private JsonClient()
    {
        HttpConnectionManagerParams params = this.connectionManager.getParams();
        this.connectionManager.setMaxTotalConnections( DEFAULT_MAX_NUMBER_CONNECTIONS );
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

    public JSONObject callAlpaca( String component, String method, JSONObject object ) 
        throws ConnectionException, IOException, NetworkException
    {
        String url = ALPACA_BASE_URL + component + "/" + method + "?argyle=" + getNonce();
        return call( url, null, object );
    }

    public void callAlpacaAsync( String component, String method, JSONObject object ) 
        throws ConnectionException, IOException, NetworkException
    {
        String url = ALPACA_BASE_URL + component + "/" + method + "?argyle=" + getNonce();
        LocalUvmContextFactory.context().newThread( new AsyncRequestRunner( url, null, object )).start();
    }

    public void updateAlpacaSettings()
    {
        try {
            LocalUvmContext context = LocalUvmContextFactory.context();
            JSONObject object = new JSONObject();
            object.put( "language", context.languageManager().getLanguageSettings().getLanguage());
            object.put( "skin", context.skinManager().getSkinSettings().getAdministrationClientSkin());

            callAlpacaAsync( "uvm", "set_uvm_settings", object );
        } catch (JSONException e ) {
            logger.warn( "Unable to build json object" );
        } catch ( ConnectionException e ) {
            logger.warn( "Unable to build json object" );
        } catch ( IOException e ) {
            logger.warn( "Unable to build json object" );
        } catch ( NetworkException e ) {
            logger.warn( "Unable to build json object" );
        }
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

    public void callAsync( String url, String param, JSONObject object )
    {
        
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

    /* --------- private --------- */
    private String getNonce() throws IOException, NetworkException 
    {
        BufferedReader stream = null;
        try {
            stream = new BufferedReader( new FileReader( ALPACA_NONCE_FILE ));
            
            String nonce = stream.readLine();
            if ( nonce.length() < 3 ) {
                throw new NetworkException( "Invalid nonce in the file [" + ALPACA_NONCE_FILE + "]: ', " + 
                                            nonce + "'" );
            }

            return nonce;
        } finally {
            try {
                if ( stream != null ) stream.close();
            } catch ( IOException e ) {
                this.logger.warn( "Unable to close the nonce file: " + ALPACA_NONCE_FILE, e );
            }
        }
    }

    class AsyncRequestRunner implements Runnable
    {
        private String url;
        private String param;
        private JSONObject object;

        AsyncRequestRunner( String url, String param, JSONObject object )
        {
            this.url = url;
            this.param = param;
            this.object = object;
        }

        public void run()
        {
            try {
                getInstance().call( this.url, this.param, this.object );
            } catch ( Exception e ) {
                logger.warn( "Error while calling the alpaca", e );
            }
        }
    }
}

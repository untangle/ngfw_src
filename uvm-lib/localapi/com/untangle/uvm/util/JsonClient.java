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

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
 
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;

import org.apache.commons.httpclient.params.HttpMethodParams;

import org.json.JSONException;
import org.json.JSONObject;


public class JsonClient
{
    public static final JsonClient INSTANCE = new JsonClient();

    private final MultiThreadedHttpConnectionManager connectionManager = 
        new MultiThreadedHttpConnectionManager();

    private JsonClient()
    {
        this.connectionManager.getParams().setMaxTotalConnections( 5 );
    }

    public static void main(String[] args) throws Exception
    {
        JSONObject object = getInstance().call( args[0], new JSONObject( args[1] ));
        
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
        
        // Create a method instance.
        PostMethod method = new PostMethod( url );

        Part parts[] = { new StringPart( param, object.toString()) };
        method.setRequestEntity(new MultipartRequestEntity( parts, method.getParams()));
        
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

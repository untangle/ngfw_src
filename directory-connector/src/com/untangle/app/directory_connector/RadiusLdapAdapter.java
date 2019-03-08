/**
 * $Id: RadiusLdapAdapter.java,v 1.00 2017/03/03 19:30:10 dmorris Exp $
 * 
 * Copyright (c) 2003-2017 Untangle, Inc.
 *
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Untangle.
 */
package com.untangle.app.directory_connector;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import net.jradius.client.RadiusClient;
import net.jradius.client.auth.CHAPAuthenticator;
import net.jradius.client.auth.MSCHAPv1Authenticator;
import net.jradius.client.auth.MSCHAPv2Authenticator;
import net.jradius.client.auth.PAPAuthenticator;
import net.jradius.client.auth.RadiusAuthenticator;
import net.jradius.dictionary.Attr_Password;
import net.jradius.dictionary.Attr_UserName;
import net.jradius.exception.RadiusException;
import net.jradius.exception.TimeoutException;
import net.jradius.exception.UnknownAttributeException;
import net.jradius.packet.AccessAccept;
import net.jradius.packet.AccessRequest;
import net.jradius.packet.RadiusPacket;
import net.jradius.packet.attribute.AttributeFactory;
import net.jradius.packet.attribute.AttributeList;

import org.apache.log4j.Logger;

import com.untangle.app.directory_connector.RadiusSettings;

/**
 * Implementation of the Radius
 */
public class RadiusLdapAdapter
{
    /* This is the number of times that radius connector can fail before giving up */
    private static int MAX_FAIL_COUNT = 20;
    
    /* This is the number of retries */
    private static int NUM_RETRIES = 5;
    
    private final Logger logger = Logger.getLogger(getClass());
    
    private enum CommandStatus { UNCONFIGURED, ACCEPTED, REJECTED, UNREACHABLE_OR_WRONG_SHARED_SECRET, UNKNOWN_ERROR };
    
    private int failCount = 0;

    private final RadiusSettings radiusSettings;

    /**
     * Construct a new Radius adapter with the given settings
     * 
     * @param radiusSettings
     *  RADIUS settings
     */
    public RadiusLdapAdapter(RadiusSettings radiusSettings)
    {
        AttributeFactory.loadAttributeDictionary("net.jradius.dictionary.AttributeDictionaryImpl");
        this.radiusSettings = radiusSettings;
    }

    /**
     * Get the apppriate authenticator based on the RADIUS authentication method.
     *
     * @return
     *  Appropriate RADIUS authenticator.
     */
    private RadiusAuthenticator getNewAuthenticator()
    {
        if ( this.radiusSettings == null )
            return null;
        
        String authMethod = this.radiusSettings.getAuthenticationMethod();
        
        if ("CHAP".equals(authMethod)) {
            return new CHAPAuthenticator();
        } else if ("CLEARTEXT".equals(authMethod)) {
            return null;
        }
        else if ("PAP".equals(authMethod)) {
            return new PAPAuthenticator();
        }
        else if ("MSCHAPV1".equals(authMethod)) {
            return new MSCHAPv1Authenticator();
        }
        else if ("MSCHAPV2".equals(authMethod)) {
            return new MSCHAPv2Authenticator();
        }

        return null;
    }
    
    /**
     * Return current RADIUS settings.
     *
     * @return
     *  Current RADIUS settings
     */
    protected final RadiusSettings getSettings()
    {
        return radiusSettings;
    }

    /**
     * Authenticate using username and password.
     *
     * @param username
     *  User's name.
     * @param password
     *  User's password.
     * @return
     *  true if authenticated, false if not.
     */
    public boolean authenticate(String username, String password)
    {
        return authenticate(username, password, "");
    }

    /**
     * Authenticate using username, password, and credentials.
     *
     * @param username
     *  User's name.
     * @param password
     *  User's password.
     * @param credentials
     *  User's credentials.
     * @return
     *  true if authenticated, false if not.
     */
    public boolean authenticate(String username, String password, String credentials ) 
    {
        CommandStatus s = sendRadiusRequest( username, password, credentials );
        
        if ( s == CommandStatus.ACCEPTED ) {
            return true;
        }
        
        return false;
    }
    
    // public boolean test()
    // {
    //     CommandStatus s = sendRadiusRequest("a", "b", "");
    //     switch ( s ) {
    //     case ACCEPTED:
    //         return true;
    //     case REJECTED:
    //         return true;
    //     default:
    //         return false;
    //     }
    // }
    
    /**
     * Get the apprpriate radius client.
     *
     * @return
     *  Radius client
     * @throws SocketException
     *  If unable to connect.
     * @throws UnknownHostException
     *  If unable to determine host name
     */    
    private RadiusClient getNewClient() throws SocketException, UnknownHostException
    {
        RadiusClient client = null;

        synchronized ( this ) {
            DatagramSocket socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(radiusSettings.getServer());
            int port = radiusSettings.getAuthPort();
            int timeout = Integer.getInteger("com.untangle.app.directory_connector.radius.timeout-ms", 4000);
            timeout = timeout / 1000;
            socket.connect(address, port);
            socket.setSoTimeout(timeout);
            client = new RadiusClient(socket,address,radiusSettings.getSharedSecret(), port, port +1, timeout);

            if( client != null ){
                failCount = 0;
            }else{
                failCount++;
                if ( failCount > MAX_FAIL_COUNT ) {
                    logger.warn( "Attempted to create the radius client with over " + MAX_FAIL_COUNT + " attempts");
                    failCount = 0;
                }
            }

            return client;
        }  
    }

    /**
     * Perform RADIUS request.
     *
     * @param username
     *  Username request.
     * @param password
     *  Password request.
     * @param credentials
     *  Credentials request
     * @return 
     *  CommandStatus of the request.
     */
    private CommandStatus sendRadiusRequest(String username, String password, String credentials ) 
    {
        if ( !this.radiusSettings.isEnabled()) {
            return CommandStatus.UNCONFIGURED;
        }
        
        RadiusClient client = null;
        try {
            client = getNewClient();
        } catch ( SocketException e ) {
            logger.warn( "Unable to create client", e );
            return CommandStatus.UNKNOWN_ERROR;
        } catch (UnknownHostException e) {
            logger.warn( "Unable to create client", e );
            return CommandStatus.UNKNOWN_ERROR;
        }
        
        if ( client == null ) {
            return CommandStatus.UNCONFIGURED;
        }
        
        AttributeList al = new AttributeList();
        
        al.add(new Attr_UserName(username));
        al.add(new Attr_Password(password));
        AccessRequest request = new AccessRequest(client, al);

        RadiusPacket reply;
        CommandStatus response = CommandStatus.UNREACHABLE_OR_WRONG_SHARED_SECRET;
        try {
            response = CommandStatus.REJECTED;
            reply = client.authenticate(request, this.getNewAuthenticator(), NUM_RETRIES);

            if( reply != null ){
                if ( reply instanceof AccessAccept ) {
                    response = CommandStatus.ACCEPTED;
                }
                if ( reply.getIdentifier() != request.getIdentifier() ) {
                    logger.warn("RADIUS lookup response ID mismatch");
                    response = CommandStatus.UNKNOWN_ERROR;
                }            
            }
        } catch (UnknownAttributeException e) {
            logger.info( "Unknown attribute?", e );
        } catch ( TimeoutException e ) {
            logger.info( "Timeout connecting to radius server");
        }  catch (RadiusException e) {
            logger.info( "Radius Exception connecting to host.", e );
        }

        client.close();
        return response;
    }
}

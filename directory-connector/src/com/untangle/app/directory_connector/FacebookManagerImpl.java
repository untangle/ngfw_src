/*
 * $Id: FacebookManagerImpl.java 41234 2015-09-12 00:47:13Z dmorris $
 */
package com.untangle.app.directory_connector;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.UvmContextFactory;

/**
 * FacebookManagerImpl provides the API implementation of all RADIUS related functionality
 */
public class FacebookManagerImpl
{
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * This is just a copy of the current settings being used
     */
    private FacebookSettings settings;

    /**
     * The app that owns this manager
     */
    private DirectoryConnectorApp directoryConnector;

    public FacebookManagerImpl( FacebookSettings settings, DirectoryConnectorApp directoryConnector )
    {
        this.directoryConnector = directoryConnector;
        setSettings(settings);
    }

    public void setSettings( FacebookSettings settings )
    {
        this.settings = settings;
    }

    public FacebookSettings getSettings()
    {
        return this.settings;
    }
    
    public boolean authenticate( String username, String pwd )
    {
        if (username == null || username.equals("") || pwd == null || pwd.equals("")) 
            return false;
        if (!this.settings.getAuthenticationEnabled())
            return false;

        return FacebookAuthenticator.authenticate( username, pwd );
    }
}

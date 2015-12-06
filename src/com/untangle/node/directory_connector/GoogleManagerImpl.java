/*
 * $Id: GoogleManagerImpl.java 41234 2015-09-12 00:47:13Z dmorris $
 */
package com.untangle.node.directory_connector;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

/**
 * GoogleManagerImpl provides the API implementation of all RADIUS related functionality
 */
public class GoogleManagerImpl implements GoogleManager
{
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * This is just a copy of the current settings being used
     */
    private GoogleSettings settings;

    /**
     * The node that owns this manager
     */
    private DirectoryConnectorApp directoryConnector;

    /**
     * These hold the proc, reader, and writer for the drive process if it is active
     */
    private Process            driveProc = null;
    private OutputStreamWriter driveProcOut = null;
    private BufferedReader     driveProcIn  = null;
    
    public GoogleManagerImpl( GoogleSettings settings, DirectoryConnectorApp directoryConnector )
    {
        this.directoryConnector = directoryConnector;
        setSettings(settings);
    }

    public void setSettings( GoogleSettings settings )
    {
        this.settings = settings;

        /* FIXME */
        /* if refreshtoken is set */
        /*     if settings file newer than /var/lib/google-drive/.gd/credentials.json (or if it doesnt exist) */
        /*          write /var/lib/google-drive/.gd/credentials.json */
    }

    public boolean isGoogleDriveConnected()
    {
        if (settings.getDriveRefreshToken() != null && !settings.getDriveRefreshToken().equals(""))
            return true;
        else
            return false;
    }


    public void startAuthorizationProcess()
    {
        if (driveProcIn != null || driveProcOut != null || driveProc != null) {
            logger.warn("Shutting down previously running drive.");
            try { driveProcIn.close(); } catch (Exception ex) { }
            try { driveProcOut.close(); } catch (Exception ex) { }
            try { driveProc.destroy(); } catch (Exception ex) { }
            driveProcIn = null;
            driveProcOut = null;
            driveProc = null;
        }

        try {
            logger.info("Launching drive...");
            ProcessBuilder builder = new ProcessBuilder("/bin/sh","-c","cd /var/lib/google-drive ; /usr/bin/drive init");
            builder.redirectErrorStream(true);
            driveProc = builder.start();

            //driveProc = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            logger.error("Couldn't start drive", e);
            return;
        }

        driveProcOut = new OutputStreamWriter(driveProc.getOutputStream());
        driveProcIn  = new BufferedReader(new InputStreamReader(driveProc.getInputStream()));
    }

    public String getAuthorizationUrl( String windowProtocol, String windowLocation )
    {
        if (driveProcIn == null || driveProcOut == null || driveProc == null) {
            throw new RuntimeException("Authorization process not running.");
        }

        if (windowProtocol == null || windowLocation == null) {
            throw new RuntimeException("Invalid arguments." + windowProtocol + " " + windowLocation);
        }
        
        try {
            String line;
            while ((line=driveProcIn.readLine())!=null) {
                logger.info("drive parsing line: " + line);
                if (!line.contains("https"))
                    continue;

                //java.net.URI uri = new java.net.URI(line);
                URIBuilder builder = new URIBuilder(line);
                String state = windowProtocol + "//" + windowLocation + "/" + "oauth" + "/" + "oauth";
                builder.setParameter("state",state);
                builder.setParameter("approval_prompt","force");

                return builder.toString();
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to parse drive output.",e);
            return null;
        }
        
    }

    /**
     * Returns null on success or the error string
     */
    public String provideDriveCode( String code )
    {
        try {
            driveProcOut.write(code);
            driveProcOut.write("\n");
            driveProcOut.flush();
            driveProcOut.close();

            driveProcIn.close();

            driveProc.destroy();
        } catch (Exception e) {
            logger.error("Failed to write code to drive.",e);
            return e.toString();
        }
        
        return null;
    }
}

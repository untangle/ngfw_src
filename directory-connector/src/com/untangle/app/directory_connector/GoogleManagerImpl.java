/**
 * $Id: GoogleManagerImpl.java 41234 2015-09-12 00:47:13Z dmorris $
 */
package com.untangle.app.directory_connector;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;

/**
 * GoogleManagerImpl provides the API implementation of all RADIUS related functionality
 */
public class GoogleManagerImpl
{
    private static final String GOOGLE_DRIVE_PATH = "/var/lib/google-drive/";
    private static final String GOOGLE_DRIVE_TMP_PATH = "/tmp/google-drive/";
    
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * This is just a copy of the current settings being used
     */
    private GoogleSettings settings;

    /**
     * The app that owns this manager
     */
    private DirectoryConnectorApp directoryConnector;

    /**
     * These hold the proc, reader, and writer for the drive process if it is active
     */
    private Process            driveProc = null;
    private OutputStreamWriter driveProcOut = null;
    private BufferedReader     driveProcIn  = null;
    
    /**
     * Initialize Google authenticator.
     *
     * @param settings
     *  GoogleSettings object.
     * @param directoryConnector
     *  Directory Connector application.
     * @return
     *  GoogleManagerImpl object.
     */
    public GoogleManagerImpl( GoogleSettings settings, DirectoryConnectorApp directoryConnector )
    {
        this.directoryConnector = directoryConnector;
        setSettings(settings);
    }

    /**
     * Get Google Authenticator settings.
     *
     * @return Google autenticator settings
     */
    public GoogleSettings getSettings()
    {
        return this.settings;
    }

    /**
     * Configure Google authenticator settings.
     *
     * @param settings  Google authenticator settings.
     */
    public void setSettings( GoogleSettings settings )
    {
        this.settings = settings;

        if ( isGoogleDriveConnected() ) {
            String credentialsJson = "{\"client_id\":\"661509598543-1p2n8foedn1n0t7t767q9sgd0accml07.apps.googleusercontent.com\",\"client_secret\":\"eJDmfgIrqJvFvk5ZoH05CJz-\",\"refresh_token\":\"";
            credentialsJson += settings.getDriveRefreshToken();
            credentialsJson += "\"}";

            BufferedWriter bw = null;
            FileWriter fw = null;
            try {
                UvmContextFactory.context().execManager().execOutput("mkdir -p " + GOOGLE_DRIVE_PATH + ".gd/");
                fw = new FileWriter(new File(GOOGLE_DRIVE_PATH + ".gd/credentials.json"));
                bw = new BufferedWriter(fw);
                bw.write(credentialsJson);
                bw.close();
            } catch (Exception ex) {
                logger.warn("Error writing credentials.json.", ex);
            }finally{
                if(fw != null){
                    try{
                        fw.close();
                    }catch(IOException ex){
                        logger.error("Unable to close file", ex);
                    }
                }
                if(bw != null){
                    try{
                        bw.close();
                    }catch(IOException ex){
                        logger.error("Unable to close file", ex);
                    }
                }
            }
        } else {
            try {
                File creds = new File(GOOGLE_DRIVE_PATH + ".gd/credentials.json");
                if ( creds.exists() )
                    creds.delete();
            } catch (Exception ex) {
                logger.warn("Error deleting credentials.json.", ex);
            }
            
        }
    }

    /**
     * Determine if Google drive is connection.
     *
     * @return true if Google drive is configured, false otherwise.
     */
    public boolean isGoogleDriveConnected()
    {
        String token = settings.getDriveRefreshToken();

        if ( token == null )
            return false;
        token = token.replaceAll("\\s+","");
        if ( "".equals( token ))
            return false;

        return true;
    }

    /**
     * This returns the URL that the user should visit and click allow for the google connector app to be authorized.
     * Once the user clicks the allow button, they will be redirected to Untangle with the redirect_url. The untangle redirect_url
     * will redirect them to their local server oauth servlet (the IP is passed in the state variable).
     * The servlet will later call provideDriveCode() with the token
     *
     * @param windowProtocol TCP/IP protocol to use.
     * @param windowLocation domain/hostname
     * @return Built URL
     */
    public String getAuthorizationUrl( String windowProtocol, String windowLocation )
    {
        startAuthorizationProcess(GOOGLE_DRIVE_TMP_PATH);
        
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

                URIBuilder builder = new URIBuilder(line);
                String state = windowProtocol + "//" + windowLocation + "/" + "oauth" + "/" + "oauth";
                builder.setParameter("state",state);
                builder.setParameter("approval_prompt","force");

                logger.info("Providing authorization URL: " + builder.toString());
                stopAuthorizationProcess();
                return builder.toString();
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to parse drive output.",e);
            return null;
        }
        
    }

    /**
     * This launches the google drive command line app and provides the code
     * The google drive app will then fetch and save the refreshToken for future use.
     *
     * This also reads the refershToken and saves it in settings.
     *
     * @param code the code to send.
     * @return null on success or the error string
     */
    public String provideDriveCode( String code )
    {
        logger.info("Providing code [" + code + "] to drive");
        startAuthorizationProcess(GOOGLE_DRIVE_PATH);
        
        try {
            driveProcOut.write(code);
            driveProcOut.write("\n");
            driveProcOut.flush();
            driveProcOut.close();
        } catch (Exception e) {
            logger.error("Failed to write code to drive.",e);
            return e.toString();
        }

        /* wait up until ten seconds for drive to create credentials.json */
        String refreshToken = null;
        for ( int i = 0; i < 10 ; i++ ) {
            try { Thread.sleep(1000); } catch (Exception e) {}

            logger.info("Checking for refresh token...");
            refreshToken = UvmContextFactory.context().execManager().execOutput("python -m simplejson.tool " + GOOGLE_DRIVE_PATH + ".gd/credentials.json | grep refresh_token | awk '{print $2}' | sed 's/\"//g'");
            if ( refreshToken == null )
                continue;
            refreshToken = refreshToken.replaceAll("\\s+","");
            if ( "".equals(refreshToken) )
                continue;
            break;
        }

        /**
         * save the settings with the refresh token
         */
        if ( refreshToken != null && !"".equals(refreshToken) ) {
            refreshToken = refreshToken.replaceAll("\\s+","");
            logger.info("Refresh Token: " + refreshToken);
        
            DirectoryConnectorSettings directoryConnectorSettings = directoryConnector.getSettings();
            directoryConnectorSettings.getGoogleSettings().setDriveRefreshToken( refreshToken );
            directoryConnector.setSettings( directoryConnectorSettings );
        } else {
            logger.warn("Unable to parse refreshToken");
            return "Unable to parse refresh_token";
        }

        stopAuthorizationProcess();
        return null;
    }

    /**
     * Disconnect Google drive
     */
    public void disconnectGoogleDrive()
    {
        DirectoryConnectorSettings directoryConnectorSettings = directoryConnector.getSettings();
        directoryConnectorSettings.getGoogleSettings().setDriveRefreshToken( null );
        directoryConnector.setSettings( directoryConnectorSettings );
    }
    
    /**
     * Start Google authorization process
     * 
     * @param dir Directory to use
     */
    private void startAuthorizationProcess( String dir )
    {
        if (driveProcIn != null || driveProcOut != null || driveProc != null) {
            logger.warn("Shutting down previously running drive.");
            stopAuthorizationProcess();
        }

        try {
            logger.info("Launching drive...");
            ProcessBuilder builder = new ProcessBuilder("/bin/sh","-c","mkdir -p " + dir + " ; cd " + dir + " ; /usr/bin/drive init");
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

    /**
     * Stop the authorization process.
     */
    private void stopAuthorizationProcess()
    {
        try { driveProcIn.close(); } catch (Exception ex) { }
        try { driveProcOut.close(); } catch (Exception ex) { }
        try { driveProc.destroy(); } catch (Exception ex) { }
        driveProcIn = null;
        driveProcOut = null;
        driveProc = null;
    }

}

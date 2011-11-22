/*
 * $Id: GoogleSafeBrowsingList.java,v 1.00 2011/07/25 12:37:16 dmorris Exp $
 */
package com.untangle.node.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;

import com.untangle.uvm.util.Pulse;

/**
 * This is a google safe browsing set of hashes
 * 
 * The constructor is a filename containing the set of MD5 hashes
 * This will load that list and provide utility functions to query the set
 */
public class GoogleSafeBrowsingHashSet
{
    private int REFRESH_TIME = 1000 * 60 * 60 * 12; /* 12 hours */
    //private int REFRESH_TIME = 1000 * 60 * 10; /* 10 minutes */

    private final Logger logger = Logger.getLogger(getClass());

    private HashSet<String> md5HashSet = null; 
    private long loadDate = 0; /* the date of the file that was last loaded */
    
    private Pulse pulse = null;
    
    public GoogleSafeBrowsingHashSet( final String filename )
    {
        initializeGoogleHashList(filename);
        pulse = new Pulse("Google SafeBrowsing updater for " + filename, true,
                          new Runnable() {
                              public void run()
                              {
                                  initializeGoogleHashList(filename);
                              }
                          });
        this.pulse.start(REFRESH_TIME);
    }

    public boolean contains( String domain, String uri )
    {
        try {
            String canonicalUrl = GoogleUrlUtils.canonicalizeURL("http://" + domain + "/" + uri);
            logger.debug("Google SafeBrowsing lookup master: " + canonicalUrl);

            ArrayList<String> urlsToLookup = GoogleUrlUtils.getLookupURLs(canonicalUrl);
            if (urlsToLookup != null) {

                for (String url : urlsToLookup) {
                    logger.debug("Google SafeBrowsing lookup: " + url);
                    String md5 = null;
                    try {
                        MessageDigest md5Hasher = MessageDigest.getInstance("MD5");
                        String hashString;
                        md5Hasher.update(url.getBytes());
                        byte[] digest = md5Hasher.digest();
                        StringBuffer hexString = new StringBuffer();
                        for (int i = 0; i < digest.length; i++) {
                            hashString = Integer.toHexString(0xFF & digest[i]);
                            if (hashString.length() < 2)
                                hashString = "0" + hashString;
                            hexString.append(hashString);
                        }
                        md5 = hexString.toString();
                    } catch (NoSuchAlgorithmException e) {
                        logger.warn("Unable to find MD5 Algorithm", e);
                    }

                    if (md5 != null) {
                        logger.debug("Google SafeBrowsing lookup: " + url + " md5: \"" + md5 + "\" size: " + md5HashSet.size());
                        if (md5HashSet.contains(md5)) {
                            logger.info("Google SafeBrowsing Lookup: " + url + " HIT! (hash: " + md5 + ")");
                            return true;
                        }
                    } else {
                        logger.warn("Unable to compute hash: http://" + domain + "/" + uri);
                    }
                }

            }
        } catch (Exception e) {
            logger.warn("Google lookup failed", e);
        }

        return false;
    }
    

    private void initializeGoogleHashList( String filename )
    {
        File hashFile = new File(filename);
        long newDate = hashFile.lastModified();
        if (md5HashSet != null && loadDate != 0 && (newDate == loadDate)) {
            logger.info("Google SafeBrowsing (" + filename + ") already up to date. Skipping...");
            return;
        }

        this.md5HashSet = new HashSet<String>();
        this.loadDate = newDate;
        
        logger.info("Google SafeBrowsing (" + filename + ") updating...");
        try {
            BufferedReader in = new BufferedReader( new FileReader( filename ) );
            String hash;
            while ((hash = in.readLine()) != null) {
                this.md5HashSet.add(hash); //ignore return value (false if already present)
            }
            in.close();
        }
        catch (java.io.FileNotFoundException e) {
            logger.error("Error loading category from missing file: " + filename);
        }
        catch (IOException e) {
            logger.error("Error loading category from file: " + filename, e);
        }
        logger.info("Google SafeBrowsing (" + filename + ") updating... done");

        
    }


}

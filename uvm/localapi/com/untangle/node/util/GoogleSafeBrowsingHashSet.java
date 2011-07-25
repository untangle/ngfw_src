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
import java.io.FileReader;
import java.util.HashSet;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;

/**
 * This is a google safe browsing set of hashes
 * 
 * The constructor is a filename containing the set of MD5 hashes
 * This will load that list and provide utility functions to query the set
 */
public class GoogleSafeBrowsingHashSet
{
    private final Logger logger = Logger.getLogger(getClass());

    private static HashSet<String> md5HashSet = null;

    public GoogleSafeBrowsingHashSet( String filename )
    {
        initializeGoogleHashList(filename);
    }

    public boolean contains( String domain, String uri )
    {
        try {
            String canonicalUrl = GoogleUrlUtils.canonicalizeURL("http://" + domain + "/" + uri);
            logger.debug("Google lookup master: " + canonicalUrl);
            ArrayList<String> urlsToLookup = GoogleUrlUtils.getLookupURLs(canonicalUrl);
            if (urlsToLookup != null) {
                for (String url : urlsToLookup) {
                    logger.debug("Google lookup: " + url);
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
                        if (md5HashSet.contains(md5)) {
                            logger.info("Google lookup: " + url + " hash: " + md5 + " HIT!");
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
        if (this.md5HashSet != null)
            return;
        
        this.md5HashSet = new HashSet<String>();
        
        try {
            BufferedReader in = new BufferedReader( new FileReader( filename ) );
            String hash;
            while ((hash = in.readLine()) != null) {
                this.md5HashSet.add(hash); //ignore return value (false if already present)
            }
            in.close();
        }
        catch (IOException e) {
            logger.error("Error loading category from file: " + filename, e);
        }
        
    }


}

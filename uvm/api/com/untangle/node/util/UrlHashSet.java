/*
 * $Id: UrlHashSet.java,v 1.00 2011/07/25 12:49:53 dmorris Exp $
 */
package com.untangle.node.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import org.apache.log4j.Logger;

/**
 * This is a set of url
 * 
 * The constructor is a filename containing the set of URLs
 * This will load that list and provide utility functions to query the contents of the set 
 */
public class UrlHashSet
{
    private final Logger logger = Logger.getLogger(getClass());

    private HashSet<String> urlHashSet = null;

    public UrlHashSet( String filename )
    {
        initialize(filename);
    }

    public boolean contains( String domain, String uri )
    {
        /**
         * Check to see if this specific URL is blocked (with all possible subdomains)
         */
        for ( String dom = domain ; dom != null ; dom = UrlMatchingUtil.nextHost(dom) ) {
            if (urlHashSet.contains(dom + uri))
                return true;
        }

        /**
         * Also check to see if the entire domain (or subdomain) is blocked
         */
        for ( String dom = domain ; dom != null ; dom = UrlMatchingUtil.nextHost(dom) ) {
            if (urlHashSet.contains(dom + "/"))
                return true;
        }

        return false;
    }
    
    private void initialize( String filename )
    {
        if (this.urlHashSet != null)
            return;
        
        this.urlHashSet = new HashSet<String>();
        
        try {
            BufferedReader in = new BufferedReader( new FileReader( filename ) );
            String url;
            while ((url = in.readLine()) != null) {
                this.urlHashSet.add(url); //ignore return value (false if already present)
            }
            in.close();
        }
        catch (IOException e) {
            logger.error("Error loading category from file: " + filename, e);
        }
    }
}
/**
 * $Id$
 */
package com.untangle.node.webfilter;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.untangle.node.util.UrlMatchingUtil;

/**
 * This class has the Web Filter Lite (webfilter) specifics for the decision engine
 */
class WebFilterDecisionEngine extends DecisionEngine
{
    private static final Logger logger = Logger.getLogger( WebFilterDecisionEngine.class );

    private static final HashMap<String, Integer> urlDatabase; //unsynchronized because never modified
    private static final HashMap<Integer, String> idToCategoryName; //unsynchronized because never modified
        
    public WebFilterDecisionEngine( WebFilterBase node )
    {
        super(node);
    }

    // protected methods ------------------------------------------------------

    protected List<String> categorizeSite(String domain, int port, String uri)
    {
        List<String> results = null;
        LinkedList<Integer> categories = new LinkedList<Integer>();
        String url = domain + uri;

        logger.debug("Web Filter Category Lookup: " + url); 

        /**
         * First, lookup the specific URL
         */
        Integer category = urlDatabase.get(url);
        if (category != null)
            categories.add(category);

        /**
         * If no categorization has already been found
         * Next, lookup the domain and subdomains with URI
         * If the URL is "www.example.com/foo" lookup "www.example.com/foo" and "example.com/foo"
         */
        if (categories.size() == 0) {
            String dom;
            for ( dom = domain ; null != dom ; dom = UrlMatchingUtil.nextHost(dom) ) {
                category = urlDatabase.get(dom + uri);
                if (category != null) {
                    categories.add(category);
                    break;
                }
            }
        }

        /**
         * If no categorization has already been found
         * Next, lookup the domain and subdomains without URI
         * If the URL is "www.example.com/foo" lookup "www.example.com" and "example.com"
         */
        if (categories.size() == 0) {
            String dom;
            for ( dom = domain ; null != dom ; dom = UrlMatchingUtil.nextHost(dom) ) {
                category = urlDatabase.get(dom + "/");
                if (category != null) {
                    categories.add(category);
                    break;
                }
                category = urlDatabase.get(dom);
                if (category != null) {
                    categories.add(category);
                    break;
                }
            }
        }
        
        if (categories.size() == 0) {
            results = Collections.singletonList("uncategorized");
        } else {
            results = new ArrayList<String>();

            for (Integer categoryId : categories) {
                String categoryName = idToCategoryName.get(categoryId);
                if (categoryName == null)
                    logger.warn("Unknown category: " + categoryId);
                else {
                    logger.debug("Web Filter Category Lookup: " + url + " -> " + categoryName); 
                    results.add(categoryName);
                }
            }
        }

        logger.debug("Web Filter Category Lookup: " + url + " = " + results); 
        return results;
    }

    static
    {
        logger.info("Initializing urlDatabase...");

        HashMap<String, Integer> urlDatabaseTmp = new HashMap<String,Integer>();
        HashMap<Integer, String> idToCategoryNameTmp = new HashMap<Integer,String>();
        
        List<String>  fileNames = new ArrayList<String>();
        List<Integer> categoryIDs = new ArrayList<Integer>();
        
        fileNames.add("/usr/share/untangle-webfilter-init/aggressive-url");
        fileNames.add("/usr/share/untangle-webfilter-init/dating-url");
        fileNames.add("/usr/share/untangle-webfilter-init/drugs-url");
        fileNames.add("/usr/share/untangle-webfilter-init/ecommerce-url");
        fileNames.add("/usr/share/untangle-webfilter-init/gambling-url");
        fileNames.add("/usr/share/untangle-webfilter-init/hacking-url");
        fileNames.add("/usr/share/untangle-webfilter-init/jobsearch-url");
        fileNames.add("/usr/share/untangle-webfilter-init/mail-url");
        fileNames.add("/usr/share/untangle-webfilter-init/porn-url");
        fileNames.add("/usr/share/untangle-webfilter-init/proxy-url");
        fileNames.add("/usr/share/untangle-webfilter-init/socialnetworking-url");
        fileNames.add("/usr/share/untangle-webfilter-init/sports-url");
        fileNames.add("/usr/share/untangle-webfilter-init/vacation-url");
        fileNames.add("/usr/share/untangle-webfilter-init/violence-url");

        categoryIDs.add(1);
        categoryIDs.add(2);
        categoryIDs.add(3);
        categoryIDs.add(4);
        categoryIDs.add(5);
        categoryIDs.add(6);
        categoryIDs.add(7);
        categoryIDs.add(8);
        categoryIDs.add(9);
        categoryIDs.add(10);
        categoryIDs.add(11);
        categoryIDs.add(12);
        categoryIDs.add(13);
        categoryIDs.add(14);

        idToCategoryNameTmp.put(1,"aggression");
        idToCategoryNameTmp.put(2,"dating");
        idToCategoryNameTmp.put(3,"drugs");
        idToCategoryNameTmp.put(4,"ecommerce");
        idToCategoryNameTmp.put(5,"gambling");
        idToCategoryNameTmp.put(6,"hacking");
        idToCategoryNameTmp.put(7,"jobsearch");
        idToCategoryNameTmp.put(8,"mail");
        idToCategoryNameTmp.put(9,"porn");
        idToCategoryNameTmp.put(10,"proxy");
        idToCategoryNameTmp.put(11,"socialnetworking");
        idToCategoryNameTmp.put(12,"sports");
        idToCategoryNameTmp.put(13,"vacation");
        idToCategoryNameTmp.put(14,"violence");
        
        Integer categoryId;
        String fileName;

        for ( int i = 0 ; i < categoryIDs.size() ; i++ ) {
            categoryId = categoryIDs.get(i);
            fileName = fileNames.get(i);
            String categoryName = idToCategoryNameTmp.get(categoryId);
            
            int urlCount = 0;
            int stringLength = 0;
            logger.info("Loading Category \"" + categoryName + "\" from \"" + fileName + "\"");
            try {
                BufferedReader in = new BufferedReader(new FileReader(fileName));
                String url;
                while ((url = in.readLine()) != null) {
                    urlCount++;
                    stringLength += url.length();
                    Integer currentCategorization = urlDatabaseTmp.get(url);

                    if (currentCategorization != null) {
                        logger.debug( "Ignoring categorization for " + url + " -> " + categoryName + "(" + categoryId + ")" + " already categorized: " + idToCategoryNameTmp.get(currentCategorization) + "(" + currentCategorization + ")" );
                    } else {
                        logger.debug( "Adding   categorization for " + url + " -> " + categoryName + "(" + categoryId + ")");
                        urlDatabaseTmp.put(url, categoryId); 
                    }
                }
                in.close();
                logger.info("Loaded  Category \"" + categoryName + "\" from \"" + fileName + "\" : " + urlCount + " urls. " + stringLength + " characters.");

            }
            catch (IOException e) {
                logger.error("Error loading category from file: " + fileName, e);
            }
        }

        urlDatabase = urlDatabaseTmp;
        idToCategoryName = idToCategoryNameTmp;
        
        logger.info("Initializing urlDatabase... done.");
    }
}

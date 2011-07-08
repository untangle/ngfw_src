/*
 * $Id: WebFilterDecisionEngine.java,v 1.00 2011/07/07 12:12:27 dmorris Exp $
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * This class has the Web Filter Lite (webfilter) specifics for the decision engine
 *
 * @version 1.0
 */
class WebFilterDecisionEngine extends DecisionEngine
{
    private final Logger logger = Logger.getLogger(getClass());

    private boolean unconfigured = true;

    private static final File INIT_HOME = new File("/usr/share/untangle-webfilter-init/");

    private static HashMap<String,List<Integer>> urlDatabase = null;
    private static HashMap<Integer, String> idToCategoryName = null;
        
    public WebFilterDecisionEngine( WebFilterBase node )
    {
        super(node);

        initializeDB();
    }

    protected boolean getLookupSubdomains()
    {
        return true;
    }

    // protected methods ------------------------------------------------------

    protected List<String> categorizeSite(String dom, int port, String uri)
    {
        String url = dom + uri;

        logger.info("Web Filter Category Lookup: " + url); 

        List<Integer> categories = urlDatabase.get(url);
        List<String> results = null;

        if (categories == null || categories.size() == 0) {
            results = Collections.singletonList("Uncategorized");
        } else {
            results = new LinkedList<String>();

            for (Integer categoryId : categories) {
                String categoryName = idToCategoryName.get(categoryId);
                if (categoryName == null)
                    logger.warn("Unknown category: " + categoryId);
                else {
                    logger.info("Web Filter Category Lookup: " + url + " -> " + categoryName); 
                    results.add(categoryName);
                }
            }
        }

        return results;
    }

    private synchronized void initializeDB()
    {
        if (urlDatabase != null)
            return;
        if (idToCategoryName != null)
            return;

        logger.info("Initializing urlDatabase...");

        this.urlDatabase = new HashMap<String,List<Integer>>();
        this.idToCategoryName = new HashMap<Integer,String>();
        
        List<String>  fileNames = new LinkedList<String>();
        List<String>  categoryNames = new LinkedList<String>();
        List<Integer> categoryIDs = new LinkedList<Integer>();
        
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

        categoryNames.add("Hate and Aggression");
        categoryNames.add("Dating");
        categoryNames.add("Illegal Drugs");
        categoryNames.add("Shopping");
        categoryNames.add("Gambling");
        categoryNames.add("Hacking");
        categoryNames.add("Job Search");
        categoryNames.add("Web Mail");
        categoryNames.add("Pornography");
        categoryNames.add("Proxy Sites");
        categoryNames.add("Social Networking");
        categoryNames.add("Sports");
        categoryNames.add("Vacation");
        categoryNames.add("Violence");

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

        idToCategoryName.put(1,"aggression");
        idToCategoryName.put(2,"dating");
        idToCategoryName.put(3,"drugs");
        idToCategoryName.put(4,"ecommerce");
        idToCategoryName.put(5,"gambling");
        idToCategoryName.put(6,"hacking");
        idToCategoryName.put(7,"jobsearch");
        idToCategoryName.put(8,"mail");
        idToCategoryName.put(9,"porn");
        idToCategoryName.put(10,"proxy");
        idToCategoryName.put(11,"socialnetworking");
        idToCategoryName.put(12,"sports");
        idToCategoryName.put(13,"vacation");
        idToCategoryName.put(14,"violence");
        
        Integer categoryId;
        String fileName;
        int i = 0;

        for ( String categoryName : categoryNames ) {
            categoryId = categoryIDs.get(i);
            fileName = fileNames.get(i);

            logger.debug("Loading Category \"" + categoryName + "\" from \"" + fileName + "\"");
            try {
                BufferedReader in = new BufferedReader(new FileReader(fileName));
                String url;
                while ((url = in.readLine()) != null) {
                    List<Integer> currentCategorization = urlDatabase.get(url);

                    if (currentCategorization != null) {
                        //logger.debug("Adding   categorization for " + url + " -> " + categoryName + "(" + categoryId + ")");
                        currentCategorization.add(categoryId);
                    } else {
                        //logger.debug("Creating categorization for " + url + " -> " + categoryName + "(" + categoryId + ")");
                        List<Integer> newCategorization = new LinkedList<Integer>();
                        newCategorization.add(categoryId);
                        urlDatabase.put(url, newCategorization);
                    }
                }
            }
            catch (IOException e) {
                logger.error("Error loading category from file: " + fileName, e);
            }

            i++;
        }

        logger.info("Initializing urlDatabase... done.");
    }
}

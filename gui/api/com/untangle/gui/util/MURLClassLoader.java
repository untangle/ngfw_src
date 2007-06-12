/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.gui.util;

import java.net.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

public class MURLClassLoader extends URLClassLoader {

    private final Logger logger = Logger.getLogger(getClass());

    public MURLClassLoader(ClassLoader parent){
        super( new URL[0], parent );
    }

    // This now adds all mars for the node, including the base and
    // parents (if any)
    private void addMarsFor() {
        Set<URL> urls = new HashSet<URL>(Arrays.asList(getURLs()));

        for (String s : Util.getToolboxManager().getWebstartResources()) {
            try {
                URL mu = new URL(Util.getServerCodeBase().toString() + s);

                if (!urls.contains(mu)) {
                    addURL(mu);
                    urls.add(mu);
                }
            } catch (MalformedURLException exn) {
                System.out.println("Bad url: " + exn);
            }
        }
    }

    public synchronized Class mLoadClass(String className){

        Class returnClass = null;
        //logger.debug("--> Trying to load class: " + className + " with mar: " + marName);
        // try to load the class as normal
        try{
            returnClass = this.loadClass(className);
        } catch (ClassNotFoundException e) {
            returnClass = null;
        }
        if(returnClass != null){
            //logger.info("  |--> Loaded Class from Parent: " + returnClass.getClassLoader() );
            return returnClass;
        }


        // try to dynamically load the class
        try{
            this.addMarsFor();

            //URL[] availableURLs = Util.getClassLoader().getURLs();
            //for(int i=0; i<availableURLs.length; i++)
            //    logger.info( "  |--> Found: " + availableURLs[i].toString() );
            returnClass = this.loadClass(className);
            //if(returnClass != null)
            //    logger.info("  |--> Loaded Class from URL: " + className);
            //else
            //    logger.info("  |--> UNABLE TO LOAD: " + className);
            //logger.info("parent class loader: " + returnClass.getClassLoader() );
        }
        catch(Exception e){
            //e.printStackTrace();
            returnClass = null;
        }

        return returnClass;
    }

}





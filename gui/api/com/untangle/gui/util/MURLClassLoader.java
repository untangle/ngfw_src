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

import java.io.InputStream;
import java.net.*;
import java.util.List;

import com.untangle.mvvm.tran.TransformDesc;

import org.apache.log4j.Logger;

public class MURLClassLoader extends URLClassLoader {

    private final Logger logger = Logger.getLogger(getClass());
    
    public MURLClassLoader(ClassLoader parent){
        super( new URL[0], parent );
    }

    private void addMarIfNeeded(String marName) {
        try {
            URL marURL = new URL(Util.getServerCodeBase().toString() + marName + "-gui.jar");
            URL[] existingURLs = getURLs();
            for (URL url : existingURLs)
                if (url.equals(marURL))
                    return;
            //logger.debug("Adding " + marURL + " to class path");
            addURL(marURL);
        } catch(Exception e){
            //logger.info("Couldn't do it:" + e.getMessage());
            e.printStackTrace();
            //logger.info("  |--> Couldn't add mar: " + marName);
        }
    }

    // This now adds all mars for the transform, including the base and parents (if any)
    private void addMarsFor(TransformDesc desc) {
        String main = desc.getName();
        String base = desc.getTransformBase();
        List<String> parents = desc.getParents();
        if (parents != null)
            for (String parent : parents)
                addMarIfNeeded(parent);
        if (base != null)
            addMarIfNeeded(base);
        addMarIfNeeded(main);
    }

    /*
      public synchronized void addNewSimpleURL(URL newURL){
      super.addURL(newURL);
      logger.info("  |--> Added: " + newURL);
      }

      public Class loadClass(String className, boolean resolveClass){
      synchronized(this){
      Class returnClass = null;
      logger.info("@ loadClass( " + className + " , " + resolveClass + " ) ");
      try{
      returnClass = super.loadClass(className, resolveClass);
      }
      catch(Exception e){
      logger.info("  |--> Failed Load: " + className);
      e.printStackTrace();
      returnClass = null;
      }

      if(returnClass != null)
      logger.info("  |--> Loaded: " + className);
      else
      logger.info("  |--> Failed Load: " + className);

      return returnClass;
      }
      }

      public URL getResource(String resourceName){
      URL returnURL = null;
      logger.info("@ getResource( " + resourceName + " )" + "  ContextClassLoader: " + Thread.currentThread().getContextClassLoader() );
      try{
      returnURL = super.getResource(resourceName);
      }
      catch(Exception e){
      logger.info("  |--> Failed Load: " + resourceName);
      returnURL = null;
      }


      if(returnURL != null)
      logger.info("  |--> Loaded: " + resourceName);
      else
      logger.info("  |--> Failed Load: " + resourceName);


      return returnURL;
      }
    */

    public synchronized Class loadClass(String className, TransformDesc transformDesc){

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
            this.addMarsFor(transformDesc);

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


    public synchronized Class loadClass(String className, String jarName){

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
            this.addMarIfNeeded(jarName);
            returnClass = this.loadClass(className);
            
        }
        catch(Exception e){
            //e.printStackTrace();
            returnClass = null;
        }

        return returnClass;
    }    



}





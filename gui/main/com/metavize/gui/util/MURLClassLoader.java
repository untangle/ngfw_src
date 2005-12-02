/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.util;

import java.io.InputStream;
import java.net.*;

public class MURLClassLoader extends URLClassLoader {
    
    
    public MURLClassLoader(ClassLoader parent){
        super( new URL[0], parent );
    }
    
    public void addMar(String marName){
        try{
            super.addURL( new URL(Util.getServerCodeBase().toString() + marName + "-client.mar") );
	}
        catch(Exception e){
            //System.err.println("  |--> Couldn't add mar: " + marName);
        }
    }
    
    /*
    public synchronized void addNewSimpleURL(URL newURL){
        super.addURL(newURL);
        System.err.println("  |--> Added: " + newURL);
    }
    
    public Class loadClass(String className, boolean resolveClass){
        synchronized(this){
        Class returnClass = null;
        System.err.println("@ loadClass( " + className + " , " + resolveClass + " ) ");
        try{
            returnClass = super.loadClass(className, resolveClass);
        }
        catch(Exception e){
            System.err.println("  |--> Failed Load: " + className);
            e.printStackTrace();
            returnClass = null;
        }
        
        if(returnClass != null)
            System.err.println("  |--> Loaded: " + className);
        else
            System.err.println("  |--> Failed Load: " + className);
        
        return returnClass;
        }
    }
    
    public URL getResource(String resourceName){
        URL returnURL = null;
        System.err.println("@ getResource( " + resourceName + " )" + "  ContextClassLoader: " + Thread.currentThread().getContextClassLoader() );
        try{
            returnURL = super.getResource(resourceName);
        }
        catch(Exception e){
            System.err.println("  |--> Failed Load: " + resourceName);
            returnURL = null;
        }
        
        
        if(returnURL != null)
            System.err.println("  |--> Loaded: " + resourceName);
        else
            System.err.println("  |--> Failed Load: " + resourceName);
        
        
        return returnURL;
    }
    */
    
    public Class loadClass(String className, String marName){
        
        Class returnClass = null;
	//System.out.println("--> Trying to load class: " + className + " with mar: " + marName);
        // try to load the class as normal
        try{
            returnClass = this.loadClass(className);
        }
        catch(Exception e){
            //e.printStackTrace();
            returnClass = null;
        }
        if(returnClass != null){
            //System.err.println("  |--> Loaded Class from Parent: " + returnClass.getClassLoader() );
            return returnClass;
        }
            
        
        // try to dynamically load the class
        try{
            this.addMar(marName);
            //URL[] availableURLs = Util.getClassLoader().getURLs();
            //for(int i=0; i<availableURLs.length; i++)
            //    System.err.println( "  |--> Found: " + availableURLs[i].toString() );
            returnClass = this.loadClass(className);
            //if(returnClass != null)
            //    System.err.println("  |--> Loaded Class from URL: " + className);
            //else
            //    System.err.println("  |--> UNABLE TO LOAD: " + className);
	    //System.err.println("parent class loader: " + returnClass.getClassLoader() );
        }
        catch(Exception e){
            //e.printStackTrace();
            returnClass = null;
        }
        
        return returnClass;
        }
    
    
    
    
}



            

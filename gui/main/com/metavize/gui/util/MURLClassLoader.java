/*
 * MURLClassLoader.java
 *
 * Created on January 18, 2005, 6:33 PM
 */

package com.metavize.gui.util;

import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import java.io.InputStream;
import java.net.*;

/**
 *
 * @author inieves
 */
public class MURLClassLoader extends URLClassLoader {
    
    private URL codeBase = null;
    
    public MURLClassLoader(ClassLoader parent){
        super( new URL[0], parent );
        //System.err.println("@ Created new class loader with parent: " + parent);
        try {
            BasicService bs = (BasicService) ServiceManager.lookup("javax.jnlp.BasicService");
            codeBase = bs.getCodeBase();
        } catch (Exception x) {
            // Can't happen.
            throw new Error("JNLP missing");
        }
    }
    
    public void addMar(String marName){
        URL newURL = null;
        try{
            newURL = new URL(codeBase, marName + ".mar");
            //System.err.println("@ Adding URL to MURLClassLoader: " + newURL.toString() );
            super.addURL(newURL);
            //System.err.println("  |--> Added: " + newURL);
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



            

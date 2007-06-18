/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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

        for (String s : Util.getRemoteToolboxManager().getWebstartResources()) {
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





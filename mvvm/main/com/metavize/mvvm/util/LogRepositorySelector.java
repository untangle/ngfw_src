package com.metavize.mvvm.util;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.spi.RepositorySelector;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RootCategory;
import org.apache.log4j.xml.DOMConfigurator;

/** An implementation of the Log4j RepositorySelector that looks for chapter
 * example local log4j.xml files
 * @author Scott.Stark@jboss.org
 * @version $Revision: 1.1.1.1 $
 */
public class LogRepositorySelector implements RepositorySelector
{
   private static RepositorySelector theSelector;
   private static Object guard = new Object();
   private Hierarchy heirarchy;

   public static synchronized void init(String name)
   {
      if( theSelector == null )
      {
         Hierarchy heirarchy = new Hierarchy(new RootCategory(Level.DEBUG));
         // Locate the log4j.xml or log4j.properties config
         InputStream is = findConfig(name, heirarchy);
         if( is == null )
            throw new IllegalStateException("Failed to find any log4j.xml config");

         DOMConfigurator config = new DOMConfigurator();
         config.doConfigure(is, heirarchy);
         theSelector = new LogRepositorySelector(heirarchy);
         // Establish the RepositorySelector
         LogManager.setRepositorySelector(theSelector, guard);
      }
   }

    static {
       init("mvvm");
    }

   private LogRepositorySelector(Hierarchy heirarchy)
   {
      this.heirarchy = heirarchy;
   }

   public LoggerRepository getLoggerRepository()
   {
      return heirarchy;
   }

   private static InputStream findConfig(String name, Hierarchy heirarchy)
   {
      ClassLoader tcl = Thread.currentThread().getContextClassLoader();
      InputStream is = null;

      // First look for a resource: "name / log4j-suffix(name).xml"
      String prefix = "";
      String suffix = name;
      int dot = name.lastIndexOf('.');
      if( dot >= 0 )
      {
         prefix = name.substring(0, dot);
         suffix = name.substring(dot+1);
      }
      prefix = prefix.replace('.', '/');

      String log4jxml = prefix + "/log4j-" + suffix + ".xml";
      URL resURL = tcl.getResource(log4jxml);
      if( resURL != null )
      {
         try
         {
            is = resURL.openStream();
            System.out.println("Found resURL: "+resURL);
            return is;
         }
         catch(IOException e)
         {
         }
      }

      // Next look for resource name / + log4j.xml
      log4jxml = prefix + "/log4j.xml";
      resURL = tcl.getResource(log4jxml);
      if( resURL != null )
      {
         try
         {
            is = resURL.openStream();
         }
         catch(IOException e)
         {
         }
         //System.out.println("Found resURL: "+resURL);
         return is;
      }

      // Next look for just the log4j.xml res
      log4jxml = "log4j.xml";
      resURL = tcl.getResource(log4jxml);
      if( resURL != null )
      {
         try
         {
            is = resURL.openStream();
            //System.out.println("Found resURL: "+resURL);
         }
         catch(IOException e)
         {
         }
      }
      return is;
   }
}

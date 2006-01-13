/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: MvvmContextImpl.java 3986 2005-12-16 05:24:06Z amread $
 */

package com.metavize.mvvm.engine;

import com.metavize.mvvm.AppServerManager;
import com.metavize.mvvm.MvvmContextFactory;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.net.InetAddress;
import org.apache.log4j.Logger;

//name_


/**
 * TODO A work in progress (currently a disorganized mess of crap taken
 * from the old "main" and "TomcatManager" code.
 */
public class AppServerManagerImpl
  implements AppServerManager {

  public static final int DEFAULT_HTTPS_PORT = 443;

  public static int m_defHttpPort = 80;
  public int m_defHttpsPort = DEFAULT_HTTPS_PORT;
  public int m_externalHttpsPort = DEFAULT_HTTPS_PORT;
  

  private static AppServerManagerImpl s_instance;
  private final Logger m_logger =
    Logger.getLogger(AppServerManagerImpl.class);

  private TomcatManager m_tomcatManager;

  private AppServerManagerImpl() {

    m_tomcatManager = ((MvvmContextImpl)MvvmContextFactory.context()).getMain().getTomcatManager();

    File f = new File(System.getProperty("bunnicula.conf.dir") + "/mvvm.networking.properties");
    Properties networkingProperties = new Properties();

    if (f.exists()) {
      FileInputStream fis = null;
      try {
        m_logger.info("Loading mvvm.netaorking.properties from " + f);
        fis = new FileInputStream(f);
        networkingProperties.load(fis);
        try{fis.close();}catch(Exception ignore){}
      }
      catch(Exception ex) {
        m_logger.error("", ex);
        try{fis.close();}catch(Exception ignore){}
      }
    } /* This file may not exist */
  
    /* Retrieve the outside HTTPS port from the properties */
    try {
        String temp;
        if (( temp = networkingProperties.getProperty("mvvm.https.port")) != null ) {
            m_externalHttpsPort = Integer.parseInt( temp );
        } else {
            m_externalHttpsPort = DEFAULT_HTTPS_PORT;
        }
    } catch ( NumberFormatException e ) {
        m_logger.warn( "Invalid https port string. using default: " + DEFAULT_HTTPS_PORT );
        m_externalHttpsPort = DEFAULT_HTTPS_PORT;
    }

    /* Illegal range */
    if ( m_externalHttpsPort <= 0 || m_externalHttpsPort >= 0xFFFF || m_externalHttpsPort == 80 ) {
        m_externalHttpsPort = DEFAULT_HTTPS_PORT;
    }
  
/*
    File keysDir = new File(System.getProperty("bunnicula.conf.dir") +
      File.separator + "webServerSSLKeys");
    if(!keysDir.exists()) {
      keysDir.mkdirs();
    }
*/    
  
//    System.out.println("***DEBUG*** bunnicula.home: " + System.getProperty("bunnicula.home"));
//    java.io.File file = new java.io.File(System.getProperty("bunnicula.home") + "/conf/keystore");
//    System.out.println("***DEBUG*** Keystore exists: " + file.exists());
  }


  /**
   * Method to obtain the singleton AppServerManager
   */
  static synchronized AppServerManagerImpl getInstance() {
    if(s_instance == null) {
      s_instance = new AppServerManagerImpl();
    }
    return s_instance;
  }


  public void postInit(MvvmContextBase mvvmContext,
    TomcatManager tcm) {
    m_tomcatManager = tcm;
//    System.out.println("***DEBUG*** [AppServerManagerImpl][postInit()]");
    try {
//      System.out.println("Proxy restart of Tomcat");
//      logger.info("starting tomcat");
      m_tomcatManager.startTomcat(mvvmContext, m_defHttpPort, m_defHttpsPort, m_externalHttpsPort);
    }
    catch(Exception ex) {
      ex.printStackTrace(System.out);
    }
  }

  //===================================================
  // See Doc from interface
  //===================================================
  public void rebindExternalHttpsPort(int port) throws Exception {
    m_tomcatManager.rebindExternalHttpsPort(port);
  }

  //===================================================
  // See Doc from interface
  //===================================================
  public boolean loadWebApp(String urlBase,
    String rootDir) {
    return m_tomcatManager.loadWebApp(urlBase, rootDir);
  }

  //===================================================
  // See Doc from interface
  //===================================================  
  public boolean unloadWebApp(String contextRoot) {
    return m_tomcatManager.unloadWebApp(contextRoot);
  }


  private String getFQDN() {
    try {
      InetAddress[] allLocals = InetAddress.getAllByName("127.0.0.1");
      for(InetAddress addr : allLocals) {
        if(addr.getHostName().equalsIgnoreCase("localhost")) {
          continue;
        }
        return addr.getHostName();
      }
      m_logger.error("Unable to find local host name");
      return "mv-edgeguard";
    }
    catch(java.net.UnknownHostException ex) {
      m_logger.error("Unable to find local host name", ex);
      return "mv-edgeguard";
    }
  }
  
}

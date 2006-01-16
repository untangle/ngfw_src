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
import com.metavize.mvvm.security.RegistrationInfo;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.net.InetAddress;
import org.apache.log4j.Logger;

import com.metavize.mvvm.security.RFC2253Name;
import com.metavize.mvvm.security.CertInfo;
import com.metavize.tran.util.MVKeyStore;
import com.metavize.tran.util.OpenSSLWrapper;

//name_


/**
 * TODO A work in progress (currently a disorganized mess of crap taken
 * from the old "main" and "TomcatManager" code.
 */
public class AppServerManagerImpl
  implements AppServerManager {

  private static final String KS_STORE_PASS = "changeit";

  public static final int DEFAULT_HTTPS_PORT = 443;

  public static int m_defHttpPort = 80;
  public int m_defHttpsPort = DEFAULT_HTTPS_PORT;
  public int m_externalHttpsPort = DEFAULT_HTTPS_PORT;
  

  private static AppServerManagerImpl s_instance;
  private final Logger m_logger =
    Logger.getLogger(AppServerManagerImpl.class);

  private TomcatManager m_tomcatManager;
  private MVKeyStore m_keyStore;

  private AppServerManagerImpl() {

    //TODO Clean up stuff ported from "main"
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


  public void postInit(MvvmContextBase mvvmContext) {

    String effectiveHostname = getFQDN();
  
    //Open the KeyStore
    try {
      m_keyStore = MVKeyStore.open(System.getProperty("bunnicula.conf.dir") +
        File.separator + "keystore", KS_STORE_PASS, true);
    }
    catch(Exception ex) {
      m_logger.error("Exception opening KeyStore", ex);
    }

    //Check for the old key system (i.e. "tomcat" being the name
    //of the key).  If so, simply generate a new key based on whatever
    //is the current host name.
    try {
      if(!(m_keyStore.containsAlias(effectiveHostname))) {
        
        m_logger.debug("Upgrading keystore contents to contain a key for effective hostname \"" +
          effectiveHostname + "\"");

        String OU = "mv-customer-" + System.currentTimeMillis();
        RegistrationInfo ri = MvvmContextFactory.context().adminManager().getRegistrationInfo();
        if(ri != null && ri.getCompanyName() != null) {
          OU = ri.getCompanyName();
        }

        RFC2253Name dn = RFC2253Name.create();
        dn.add("OU", OU);

        regenCert(dn, ((365*5)+1));

        m_tomcatManager.setSecurityInfo("conf/keystore", KS_STORE_PASS, effectiveHostname);
      }
    }
    catch(Exception ex) {
      m_logger.error("Exception updating KeyStore", ex);
    }  

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


  //TODO bscott Sometime in the next two years we need a way for them
  //     to roll to a new key while maintaing their existing signed cert.
  
  //===================================================
  // See Doc from interface
  //=================================================== 
  public boolean regenCert(RFC2253Name dn,
    int durationInDays) {

    String effectiveHostname = getFQDN();
    
    try {
      int index = dn.indexOf("CN");
      if(index == -1) {
        index = 0;
      }
      else {
        dn.remove(index);
      }
      m_keyStore.generateKey(effectiveHostname, dn, durationInDays);

      m_tomcatManager.setSecurityInfo("conf/keystore", KS_STORE_PASS, effectiveHostname);
      return true;
    }
    catch(Exception ex) {
      m_logger.error("Unable to regen cert", ex);
      return false;
    }
  }

  //===================================================
  // See Doc from interface
  //=================================================== 
  public boolean importServerCert(byte[] cert, byte[] caCert) {

    CertInfo localCertInfo = null;

    try {
      localCertInfo = OpenSSLWrapper.getCertInfo(cert);
    }
    catch(Exception ex) {
      m_logger.error("Unable to get info from cert", ex);
    }

    //This is a hack, but if they don't have a CN what the heck are
    //they doing
    String cn = localCertInfo.getSubjectCN();
    if(cn == null) {
      m_logger.error("Received a cert without a CN? \"" +
        new String(cert) + "\"");
    }

    String reason = "";
    try {
      if(caCert != null) {
        reason = "Unable to import CA cert \"" + new String(caCert) + "\"";
        m_keyStore.importCert(caCert, cn + "-ca");
      }
      reason = "Unable to CA cert \"" + new String(cert) + "\"";
      m_keyStore.importCert(caCert, cn);
      m_tomcatManager.setSecurityInfo("conf/keystore", KS_STORE_PASS, getFQDN());
    }
    catch(Exception ex) {
      m_logger.error(reason, ex);
      return false;
    }

    return true;
    
  }

  //===================================================
  // See Doc from interface
  //=================================================== 
  public byte[] getCurrentServerCert() {
    String effectiveHostname = getFQDN();

    try {
      if(!m_keyStore.containsAlias(effectiveHostname)) {
        hostnameChanged(effectiveHostname);
      }
    }
    catch(Exception ex) {
      m_logger.error("Unable to list key store", ex);
    }

    try {
      return m_keyStore.exportEntry(effectiveHostname);
    }
    catch(Exception ex) {
      m_logger.error("Unable to retreive current cert", ex);
      return null;
    }
    
  }

  //===================================================
  // See Doc from interface
  //=================================================== 
  public byte[] generateCSR() {

    String effectiveHostname = getFQDN();
  
    try {
      return m_keyStore.createCSR(effectiveHostname);
    }
    catch(Exception ex) {
      m_logger.error("Exception generating a CSR", ex);
      return null;
    }
  }

  //===================================================
  // See Doc from interface
  //=================================================== 
  public CertInfo getCertInfo(byte[] certBytes) {
    try {
      return OpenSSLWrapper.getCertInfo(certBytes);
    }
    catch(Exception ex) {
      m_logger.error("Unable to get info from cert \"" +
        new String(certBytes) + "\"", ex);
      return null;
    }    
  }


  /**
   * Callback indicating that the hostname has changed
   */
  private void hostnameChanged(String newHostName) {
    String reason = "";
    try {

      if(
        (m_keyStore.containsAlias(newHostName)) &&
        (m_keyStore.getEntryType(newHostName) == MVKeyStore.MVKSEntryType.PrivKey)) {
        reason = "Unable to rebind tomcat to existing key with alias \"" +
          newHostName + "\"";
        m_tomcatManager.setSecurityInfo("conf/keystore", KS_STORE_PASS, newHostName);
      }
      else {
        reason = "Unable to get current cert alias";
        String currentCertName = m_tomcatManager.getKeyAlias();
        reason = "Unable to export current cert";
        byte[] oldCert = m_keyStore.exportEntry(currentCertName);
        reason = "Unable to get current cert info";
        CertInfo ci = OpenSSLWrapper.getCertInfo(m_keyStore.exportEntry(currentCertName));
        RFC2253Name oldDN = ci.subjectDN;
        regenCert(oldDN, ((365*5)+1));
      }
    }
    catch(Exception ex) {
      m_logger.error(reason, ex);
    }
  }
  


  private String getFQDN() {
    try {
      InetAddress[] allLocals = InetAddress.getAllByName("127.0.0.1");
      for(InetAddress addr : allLocals) {
        m_logger.error("***DEBUG*** Hostname: " + addr.getHostName());
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

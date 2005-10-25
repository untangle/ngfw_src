/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.mail.impl.safelist;

import com.metavize.tran.mail.papi.safelist.SafelistTransformView;
import com.metavize.tran.mail.papi.safelist.SafelistEndUserView;
import com.metavize.tran.mail.papi.safelist.SafelistAdminView;
import com.metavize.tran.mail.papi.safelist.SafelistManipulation;
import com.metavize.tran.mail.papi.safelist.NoSuchSafelistException;
import com.metavize.tran.mail.papi.safelist.SafelistActionFailedException;
import com.metavize.tran.mail.papi.safelist.SafelistSettings;
import com.metavize.tran.mime.EmailAddress;
import java.util.List;
import java.util.ArrayList;
import org.apache.log4j.Logger;


//TODO bscott This is *obviously* a silly fake implemementation,
//existing only for basic testing.  Someone needs to "make this
//real"

/**
 * Implementation of the safelist stuff
 */
public class SafelistManager
  implements SafelistAdminView,
    SafelistEndUserView,
    SafelistTransformView {

  private final Logger m_logger =
    Logger.getLogger(SafelistManager.class);

  private java.util.HashMap<String, java.util.ArrayList<String>>
    m_listsByUser = new java.util.HashMap<String, java.util.ArrayList<String>>();
  private java.util.HashSet<String>
    m_masterList = new java.util.HashSet<String>();


  public SafelistManager() {
    readSafelist();
    rebuildMaster();
  }


  /**
   * The Safelist manager "cheats" and lets the MailTranformImpl
   * maintain the persistence for settings/
   */
  public void setSettings(SafelistSettings settings) {
    
  }
    
  //-------------------- SafelistTransformView ------------------------

  //See doc on SafelistTransformView.java
  public boolean isSafelisted(EmailAddress envelopeSender,
    EmailAddress mimeFrom,
    List<EmailAddress> recipients) {

    if(envelopeSender != null) {
      if(m_masterList.contains(envelopeSender.getAddress().toLowerCase())) {
        return true;
      }
    }
    if(mimeFrom != null) {
      if(m_masterList.contains(mimeFrom.getAddress().toLowerCase())) {
        return true;
      }
    }    
    
    return false;
  }
  

  //--------------------- SafelistManipulation -----------------------

  //See doc on SafelistManipulation.java
  public String[] addToSafelist(String safelistOwnerAddress,
    String toAdd)
    throws NoSuchSafelistException, SafelistActionFailedException {
    java.util.ArrayList<String> list = getOrCreateList(safelistOwnerAddress);
    list.add(toAdd.toLowerCase());
    saveList();
    rebuildMaster();
    return ulta(list);
  }

  //See doc on SafelistManipulation.java
  public String[] removeFromSafelist(String safelistOwnerAddress,
    String toRemove)
    throws NoSuchSafelistException, SafelistActionFailedException {
    java.util.ArrayList<String> list = getOrCreateList(safelistOwnerAddress);
    list.remove(toRemove.toLowerCase());
    saveList();
    rebuildMaster();
    return ulta(list);
  }

  //See doc on SafelistManipulation.java
  public String[] replaceSafelist(String safelistOwnerAddress,
    String...listContents)
    throws NoSuchSafelistException, SafelistActionFailedException {
    java.util.ArrayList<String> list = getOrCreateList(safelistOwnerAddress);
    list.clear();
    for(String s : listContents) {
      list.add(s.toLowerCase());
    }
    saveList();
    rebuildMaster();
    return ulta(list);
  }

  //See doc on SafelistManipulation.java
  public String[] getSafelistContents(String safelistOwnerAddress)
    throws NoSuchSafelistException, SafelistActionFailedException {
    return ulta(getOrCreateList(safelistOwnerAddress));
  }

  //See doc on SafelistManipulation.java
  public boolean hasOrCanHaveSafelist(String address) {
    return true;
  }
    
  //See doc on SafelistManipulation.java
  public void test(){}
  
  
  //--------------------- SafelistAdminView -----------------------

  //See doc on SafelistAdminView.java
  public List<String> listSafelists()
    throws SafelistActionFailedException {
    return new java.util.ArrayList<String>(m_listsByUser.keySet());
  }

  //See doc on SafelistAdminView.java    
  public void deleteSafelist(String safelistOwnerAddress)
    throws SafelistActionFailedException {
    m_listsByUser.remove(safelistOwnerAddress.toLowerCase());
    saveList();
    rebuildMaster();
  }

  //See doc on SafelistAdminView.java
  public void createSafelist(String newListOwnerAddress)
    throws SafelistActionFailedException {
    getOrCreateList(newListOwnerAddress);
    saveList();
    rebuildMaster();
  }

  //See doc on SafelistAdminView.java
  public boolean safelistExists(String safelistOwnerAddress)
    throws SafelistActionFailedException {
    return m_listsByUser.containsKey(safelistOwnerAddress.toLowerCase());
  }  
  
  
  //--------------------- SafelistEndUserView -----------------------


  private java.util.ArrayList<String> getOrCreateList(String address) {
    java.util.ArrayList<String> ret = m_listsByUser.get(address.toLowerCase());
    if(ret == null) {
      ret = new java.util.ArrayList<String>();
      m_listsByUser.put(address.toLowerCase(), ret);
    }
    return ret;
  }

  private void rebuildMaster() {
    java.util.HashSet<String> newSet = new java.util.HashSet<String>();
    for(java.util.ArrayList<String> ul : m_listsByUser.values()) {
      for(String s : ul) {
        newSet.add(s.toLowerCase());
      }
    }
    m_masterList = newSet;
  }

  private void readSafelist() {
    java.io.FileInputStream fIn = null;
    try {
      java.io.File dir = new java.io.File(new java.io.File(System.getProperty("bunnicula.home")), "quarantine");
      if(!dir.exists()) {
        dir.mkdirs();
      }
      fIn = new java.io.FileInputStream(new java.io.File(dir, "FakeSafelist.ser"));
      java.io.ObjectInputStream objIn = new java.io.ObjectInputStream(fIn);
      m_listsByUser = (java.util.HashMap<String, java.util.ArrayList<String>>) objIn.readObject();
      fIn.close();
    }
    catch(Exception ex) {
      m_logger.error("", ex);
      com.metavize.tran.util.IOUtil.close(fIn);
      m_listsByUser = new java.util.HashMap<String, java.util.ArrayList<String>>();
    }
  }

  private void saveList() {
    java.io.FileOutputStream fOut = null;
    try {
      java.io.File dir = new java.io.File(new java.io.File(System.getProperty("bunnicula.home")), "quarantine");
      if(!dir.exists()) {
        dir.mkdirs();
      }
      fOut = new java.io.FileOutputStream(new java.io.File(dir, "FakeSafelist.ser"));
      java.io.ObjectOutputStream objOut = new java.io.ObjectOutputStream(fOut);
      objOut.writeObject(m_listsByUser);
      objOut.flush();
      objOut.close();
      fOut.close();
    }
    catch(Exception ex) {
      m_logger.error("", ex);
      com.metavize.tran.util.IOUtil.close(fOut);
    }
  }

  private String[] ulta(java.util.ArrayList<String> l) {
    return (String[]) l.toArray(new String[l.size()]);
  }
}
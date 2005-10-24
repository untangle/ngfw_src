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

/**
 * Implementation of the safelist stuff
 */
public class SafelistManager
  implements SafelistAdminView,
    SafelistEndUserView,
    SafelistTransformView {

  private final Logger m_logger =
    Logger.getLogger(SafelistManager.class);


  public SafelistManager() {
    
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
    
    return false;
  }
  

  //--------------------- SafelistManipulation -----------------------

  //See doc on SafelistManipulation.java
  public void addToSafelist(String safelistOwnerAddress,
    String toAdd)
    throws NoSuchSafelistException, SafelistActionFailedException {
  }

  //See doc on SafelistManipulation.java
  public void removeFromSafelist(String safelistOwnerAddress,
    String toRemove)
    throws NoSuchSafelistException, SafelistActionFailedException {
  }

  //See doc on SafelistManipulation.java
  public void replaceSafelist(String safelistOwnerAddress,
    String...listContents)
    throws NoSuchSafelistException, SafelistActionFailedException {
  }

  //See doc on SafelistManipulation.java
  public String[] getSafelistContents(String safelistOwnerAddress)
    throws NoSuchSafelistException, SafelistActionFailedException {
    return new String[0];
  }

  //See doc on SafelistManipulation.java
  public boolean hasOrCanHaveSafelist(String address) {
    return false;
  }
    
  //See doc on SafelistManipulation.java
  public void test(){}
  
  
  //--------------------- SafelistAdminView -----------------------

  //See doc on SafelistAdminView.java
  public List<String> listSafelists()
    throws SafelistActionFailedException {
    return new ArrayList<String>();
  }

  //See doc on SafelistAdminView.java    
  public void deleteSafelist(String safelistOwnerAddress)
    throws SafelistActionFailedException {
  }

  //See doc on SafelistAdminView.java
  public void createSafelist(String newListOwnerAddress)
    throws SafelistActionFailedException {
  }

  //See doc on SafelistAdminView.java
  public boolean safelistExists(String safelistOwnerAddress)
    throws SafelistActionFailedException {
    return false;
  }  
  
  
  //--------------------- SafelistEndUserView -----------------------    

}
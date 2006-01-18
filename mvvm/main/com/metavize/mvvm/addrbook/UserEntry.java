/*
 * Copyright (c) 2003, 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: $
 */

package com.metavize.mvvm.addrbook;
import java.util.List;


/**
 * Lightweight class to encapsulate an entry (user)
 * in the Address Book service.
 *
 */
public class UserEntry
  implements java.io.Serializable {

  private String m_displayName;
  private String m_uid;
  private String m_email;
  private RepositoryType m_storedIn;

  public UserEntry() {
    this(null, null, null, RepositoryType.NONE);
  }

  public UserEntry(String uid,
    String displayName,
    String email,
    RepositoryType storedIn) {

    m_displayName = displayName;
    m_uid = uid;
    m_email = email;
    m_storedIn = storedIn;
  }

  /**
   * Get the uniqueID within the scope
   * of the {@link #getStoredIn repository}
   *
   * @return the unique id
   */
  public String getUID() {
    return m_uid;
  }

  public void setUID(String uid) {
    uid = m_uid;
  }

  /**
   * Get a display name for the user (i.e. "Tom Jones").  This
   * may be null.
   *
   * @return the display name.
   */  
  public String getDisplayName() {
    return m_displayName;
  }

  public void setDisplayName(String name) {
    m_displayName = name;
  }

  /**
   * Get the email address associated with the given user.  This
   * may be null.
   *
   * @return the address
   */   
  public String getEmail() {
    return m_email;
  }
  
  public void setEmail(String email) {
    m_email = email;
  }

  /**
   * Get the type of repository in-which this entry is stored.  Note
   * that this property only applies for instances of UserEntry
   * retreived from the {@link AddressBook AddressBook}.
   *
   * @return the repository
   */
  public RepositoryType getStoredIn() {
    return m_storedIn;
  }

  public void setStoredIn(RepositoryType type) {
    m_storedIn = type;
  }


  /**
   * Equality test based on uid (case sensitive - although I'm not sure
   * that is always true) and RepositoryType.
   */
  public boolean equals(Object obj) {
    UserEntry other = (UserEntry) obj;
    return makeNotNull(other.getUID()).equals(makeNotNull(m_uid)) &&
      makeNotNull(other.getStoredIn()).equals(makeNotNull(m_storedIn));
  }

  private Object makeNotNull(Object obj) {
    return obj==null?"":obj;
  }
  
}



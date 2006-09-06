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

package com.metavize.mvvm.snmp;

/**
 * For those not familiar with SNMP, here is a bit of an explanation
 * of the relationship between properties:
 * <br><br>
 *   <ul>
 *   <li>{@link #setEnabled Enabled} turns the entire SNMP support on/off.  If
 *   this property is false, the remainder of the properties have no
 *   significance.
 *   </li>
 *   <li>
 *   {@link #getCommunityString CommunityString} is like a crappy UID/PWD combination.  This
 *   represents the UID/PWD of someone who can read the contents of the MIB on the MVVM
 *   machine.  This user has <b>read only access</b>.  There is no read/write access
 *   possible with our implementation (for safety reasons).
 *   </li>
 *   <li>
 *   The property {@link #getPort Port} property is the port on-which the MVVM
 *   will listen for UDP SNMP messages.  The default is 161.
 *   </li>
 *   <li>
 *   {@link #getSysContact SysContact} and {@link #getSysLocation SysLocation} are
 *   related in that they are informational properties, useful only if someone is
 *   monitoring the MVVM as one of many devices.  These fields are in no way required,
 *   but may make management easier.
 *   </li>
 *   <li>
 *   {@link #setSendTraps SendTraps} controls if SNMP traps are sent from the MVVM.  If this
 *   is true, then {@link #setTrapHost TrapHost}, {@link #setTrapPort TrapPort}, and
 *   {@link #setTrapCommunity TrapCommunity} must be set.
 *   </li>
 * </ul>
 * <br><br>
 * If Snmp {@link #isEnabled is enabled}, the {@link #getCommunityString CommunityString}
 * must be set (everything else can be defaulted).  If {@link #isSendTraps traps are enabled},
 * then {@link #setTrapHost TrapHost} and {@link #setTrapCommunity TrapCommunity} must be
 * set.
 *
 * @hibernate.class
 * table="SNMP_SETTINGS"
 */
public class SnmpSettings
  implements java.io.Serializable {
  
  private static final long serialVersionUID =
    7597805105233436527L;

  /**
   * The standard port for "normal" agent messages, as
   * per RFC 1157 sect 4.  The value is 161
   */
  public static final int STANDARD_MSG_PORT = 161;

  /**
   * The standard port for trap messages, as per RFC 1157
   * sect 4.  The value is 162
   */
  public static final int STANDARD_TRAP_PORT = 162;

  private Long m_id;
  private boolean m_enabled;
  private int m_port;
  private String m_communityString;
  private String m_sysContact;
  private String m_sysLocation;
  private boolean m_sendTraps;
  private String m_trapHost;
  private String m_trapCommunity;
  private int m_trapPort;

  /**
    * @hibernate.id
    * column="SNMP_SETTINGS_ID"
    * generator-class="native"
    */
  private Long getId() {
    return m_id;
  }

  private void setId(Long id) {
    m_id = id;
  }

  public void setEnabled(boolean enabled) {
    m_enabled = enabled;
  }
  /**
    * @hibernate.property
    * column="ENABLED"
    */  
  public boolean isEnabled() {
    return m_enabled;
  }

  
  /**
    * @hibernate.property
    * column="PORT"
    */
  public int getPort() {
    return m_port;
  }
  public void setPort(int port) {
    m_port = port;
  }

  /**
    * This cannot be blank ("") or null
    *
    * @return the community String
    * @hibernate.property
    * column="COM_STR"
    */
  public String getCommunityString() {
    return m_communityString;
  }
  public void setCommunityString(String s) {
    m_communityString = s;
  }

  /**
    * @return the contact info
    * @hibernate.property
    * column="SYS_CONTACT"
    */
  public String getSysContact() {
    return m_sysContact;
  }
  public void setSysContact(String s) {
    m_sysContact = s;
  }

  /**
    * @return the system location
    * @hibernate.property
    * column="SYS_LOCATION"
    */
  public String getSysLocation() {
    return m_sysLocation;
  }
  public void setSysLocation(String s) {
    m_sysLocation = s;
  }    

  

  public void setSendTraps(boolean sendTraps) {
    m_sendTraps = sendTraps;
  }
  /**
    * @hibernate.property
    * column="SEND_TRAPS"
    */  
  public boolean isSendTraps() {
    return m_sendTraps;
  }


  public void setTrapHost(String trapHost) {
    m_trapHost = trapHost;
  }
  /**
    * @return the trap host
    * @hibernate.property
    * column="TRAP_HOST"
    */  
  public String getTrapHost() {
    return m_trapHost;
  }


  public void setTrapCommunity(String tc) {
    m_trapCommunity = tc;
  }
  /**
    * @return the trap community String
    * @hibernate.property
    * column="TRAP_COM"
    */    
  public String getTrapCommunity() {
    return m_trapCommunity;
  }


  public void setTrapPort(int tp) {
    m_trapPort = tp;
  }
  /**
    * @hibernate.property
    * column="TRAP_PORT"
    */    
  public int getTrapPort() {
    return m_trapPort;
  }

}

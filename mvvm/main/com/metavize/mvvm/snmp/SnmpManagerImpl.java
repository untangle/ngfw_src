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

import org.apache.log4j.Logger;
import com.metavize.mvvm.util.TransactionWork;
import org.hibernate.Query;
import org.hibernate.Session;
import com.metavize.mvvm.MvvmContextFactory;

//TODO bscott The template for the snmpd.conf file should
//            be a Velocity template

/**
 * Implementation of the SnmpManager
 *
 */
public class SnmpManagerImpl
  implements SnmpManager {

  private static final SnmpManagerImpl s_instance =
    new SnmpManagerImpl();
  private final Logger m_logger =
    Logger.getLogger(SnmpManagerImpl.class);
  private SnmpSettings m_settings;

  private SnmpManagerImpl() {
  
    TransactionWork tw = new TransactionWork() {
      public boolean doWork(Session s) {
        Query q = s.createQuery("from SnmpSettings");
        m_settings = (SnmpSettings)q.uniqueResult();
        
        if(m_settings == null) {
          m_settings = new SnmpSettings();

          m_settings.setEnabled(false);
          m_settings.setPort(SnmpSettings.STANDARD_MSG_PORT);
          m_settings.setCommunityString("CHANGE_ME");
          m_settings.setSysContact("MY_CONTACT_INFO");
          m_settings.setSysLocation("MY_LOCATION");
          m_settings.setSendTraps(false);
          m_settings.setTrapHost("MY_TRAP_HOST");
          m_settings.setTrapCommunity("MY_TRAP_COMMUNITY");
          m_settings.setTrapPort(SnmpSettings.STANDARD_TRAP_PORT);
          
          s.save(m_settings);
        }
        return true;
      }

      public Object getResult() { return null; }
    };
    MvvmContextFactory.context().runTransaction(tw);

    m_logger.info("Initialized SnmpManager");
  }

  public static SnmpManagerImpl snmpManager() {
    return s_instance;
  }

  public SnmpSettings getSnmpSettings() {
    return m_settings;
  }

  public void setSnmpSettings(final SnmpSettings settings) {
    TransactionWork tw = new TransactionWork() {
      public boolean doWork(Session s) {
        s.saveOrUpdate(settings);
        return true;
      }
  
      public Object getResult() { return null; }
    };
    MvvmContextFactory.context().runTransaction(tw);
    m_settings = settings;

    //Now, the hard part.  Changing around the SNMPD
    
  }
}


/*

This snippit controls sending SNMP traps (v1).  Better make
sure the port is correct

###########################################################################
#
# snmpd.conf
#
#   - created by the snmpconf configuration program
#
###########################################################################
# SECTION: Trap Destinations
#
#   Here we define who the agent will send traps to.

# trapsink: A SNMPv1 trap receiver
#   arguments: host [community] [portnum]

trapsink  myV1TrapReceiveHost myV1TrapCommunity 162

# authtrapenable: Should we send traps when authentication failures occur
#   arguments: 1 | 2   (1 = yes, 2 = no)

authtrapenable  1

*/



/*

*********** To force listening on only one of 'n' interfaces ****************

    Normally, the agent will bind to the specified port on all interfaces
  on the system, and accept request received from any of them.  With
  version 4.2, the '-p' option can be used to listen on individual
  interfaces.  For example,
  
      snmpd -p 161@127.0.0.1

  will listen (on the standard port) on the loopback interface only, and

      snmpd -p 6161@10.0.0.1

  will listen on port 6161, on the (internal network) interface with address
  10.0.0.1.   If you want to listen on multiple interfaces (but not all),
  then simply repeat this option for each one:

    snmpd -p 161@127.0.0.1 -p 6161@10.0.0.1

  The v5 Net-SNMP agent has a similar facility, but does not use the '-p'
  command line option flag.  Instead, the ports and/or interfaces to listen
  on are simply listed on the command line, following any other options.  Also,
  the syntax of port and interface is slightly different (interface:port).
    So the three examples above would be

      snmpd 127.0.0.1:161
      snmpd 127.0.0.1:6161
      snmpd 127.0.0.1:161 127.0.0.1:6161

  The AgentX port option ('-x') works in much the same way, using the
  "host:port" syntax (in both 4.2 and 5.0 lines - and yes, this *is* an
  inconsistency in 4.2!)





*/

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

 package com.metavize.tran.firewall;

 import java.io.Serializable;

import com.metavize.mvvm.logging.PipelineEvent;
import com.metavize.mvvm.logging.SyslogBuilder;
import com.metavize.mvvm.tran.PipelineEndpoints;

 /**
  * Log event for the firewall.
  *
  * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
  * @version 1.0
  * @hibernate.class
  * table="TR_FIREWALL_EVT"
  * mutable="false"
  */

 public class FirewallEvent extends PipelineEvent implements Serializable
 {
     private boolean wasBlocked;
     private int     ruleIndex;
     private FirewallRule rule;

     // Constructors
     /**
      * Hibernate constructor
      */
     public FirewallEvent()
     {
     }

     public FirewallEvent( PipelineEndpoints pe,  FirewallRule rule, boolean wasBlocked, int ruleIndex )
     {
         super(pe);

         this.wasBlocked = wasBlocked;
         this.ruleIndex  = ruleIndex;
         this.rule       = rule;
     }

     /**
      * Whether or not the session was blocked.
      *
      * @return If the session was passed or blocked.
      * @hibernate.property
      * column="WAS_BLOCKED"
      */
     public boolean getWasBlocked()
     {
         return wasBlocked;
     }

     public void setWasBlocked( boolean wasBlocked )
     {
         this.wasBlocked = wasBlocked;
     }

     /**
      * Rule index, when this event was triggered.
      *
      * @return current rule index for the rule that triggered this event.
      * @hibernate.property
      * column="RULE_INDEX"
      */
     public int getRuleIndex()
     {
         return ruleIndex;
     }

     public void setRuleIndex( int ruleIndex )
     {
         this.ruleIndex = ruleIndex;
     }

     /**
      * Firewall rule that triggered this event
      *
      * @return firewall rule that triggered this event
      * @hibernate.many-to-one
      * class="com.metavize.tran.firewall.FirewallRule"
      * column="RULE_ID"
      */
     public FirewallRule getRule()
     {
         return rule;
     }

     public void setRule( FirewallRule rule )
     {
         this.rule = rule;
     }

     protected void doSyslog(SyslogBuilder sb)
     {
         sb.addField("reason", "rule #" + getRuleIndex());
         sb.addField("blocked", wasBlocked);
     }
}

/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.firewall;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.logging.StatisticEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;

/**
 * Log event for a Firewall statistics.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_firewall_statistic_evt", schema="events")
public class FirewallStatisticEvent extends StatisticEvent
{
    /* Number of outbound firewall sessions */
    /**
     * Number of TCP Sessions blocked by the default action,
     * by a rule, passsed by the default, and
     * passed by a rule */
    private int tcpBlockedDefault;
    private int tcpBlockedRule;
    private int tcpPassedDefault;
    private int tcpPassedRule;

    private int udpBlockedDefault;
    private int udpBlockedRule;
    private int udpPassedDefault;
    private int udpPassedRule;

    private int icmpBlockedDefault;
    private int icmpBlockedRule;
    private int icmpPassedDefault;
    private int icmpPassedRule;

    // Constructors
    public FirewallStatisticEvent()
    {
        this.tcpBlockedDefault = 0;
        this.tcpBlockedRule = 0;
        this.tcpPassedDefault = 0;
        this.tcpPassedRule = 0;

        this.udpBlockedDefault = 0;
        this.udpBlockedRule = 0;
        this.udpPassedDefault = 0;
        this.udpPassedRule = 0;

        this.icmpBlockedDefault = 0;
        this.icmpBlockedRule = 0;
        this.icmpPassedDefault = 0;
        this.icmpPassedRule = 0;
    }

    public FirewallStatisticEvent( int tcpBlockedDefault,  int tcpBlockedRule,
            int tcpPassedDefault,  int tcpPassedRule,
            int udpBlockedDefault,  int udpBlockedRule,
            int udpPassedDefault,  int udpPassedRule,
            int icmpBlockedDefault, int icmpBlockedRule,
            int icmpPassedDefault, int icmpPassedRule )
    {
        this.tcpBlockedDefault  = tcpBlockedDefault;
        this.tcpBlockedRule     = tcpBlockedRule;
        this.tcpPassedDefault   = tcpPassedDefault;
        this.tcpPassedRule      = tcpPassedRule;

        this.udpBlockedDefault  = udpBlockedDefault;
        this.udpBlockedRule     = udpBlockedRule;
        this.udpPassedDefault   = udpPassedDefault;
        this.udpPassedRule      = udpPassedRule;

        this.icmpBlockedDefault = icmpBlockedDefault;
        this.icmpBlockedRule    = icmpBlockedRule;
        this.icmpPassedDefault  = icmpPassedDefault;
        this.icmpPassedRule     = icmpPassedRule;
    }

    /**
     * Number of tcp sessions blocked by the default rule.
     *
     * @return Number of tcp sessions blocked by the default.
     */
     @Column(name="tcp_block_default", nullable=false)
     public int getTcpBlockedDefault()
     {
         return this.tcpBlockedDefault;
     }

     public void setTcpBlockedDefault( int tcpBlockedDefault )
     {
         this.tcpBlockedDefault = tcpBlockedDefault;
     }

     public void incrTcpBlockedDefault()
     {
         this.tcpBlockedDefault++;
     }

     /**
      * Number of tcp sessions blocked by a rule.
      *
      * @return Number of tcp sessions blocked by a rule.
      */
     @Column(name="tcp_block_rule", nullable=false)
     public int getTcpBlockedRule()
     {
         return this.tcpBlockedRule;
     }

     public void setTcpBlockedRule( int tcpBlockedRule )
     {
         this.tcpBlockedRule = tcpBlockedRule;
     }

     public void incrTcpBlockedRule()
     {
         this.tcpBlockedRule++;
     }

     /**
      * Number of tcp sessions passed by the default rule.
      *
      * @return Number of tcp sessions passed by the default.
      */
     @Column(name="tcp_pass_default", nullable=false)
     public int getTcpPassedDefault()
     {
         return this.tcpPassedDefault;
     }

     public void setTcpPassedDefault( int tcpPassedDefault )
     {
         this.tcpPassedDefault = tcpPassedDefault;
     }

     public void incrTcpPassedDefault()
     {
         this.tcpPassedDefault++;
     }

     /**
      * Number of tcp sessions passed by a rule.
      *
      * @return Number of tcp sessions passed by a rule.
      */
     @Column(name="tcp_pass_rule", nullable=false)
     public int getTcpPassedRule()
     {
         return this.tcpPassedRule;
     }

     public void setTcpPassedRule( int tcpPassedRule )
     {
         this.tcpPassedRule = tcpPassedRule;
     }

     public void incrTcpPassedRule()
     {
         this.tcpPassedRule++;
     }


     /**
      * Number of udp sessions blocked by the default rule.
      *
      * @return Number of udp sessions blocked by the default.
      */
     @Column(name="udp_block_default", nullable=false)
     public int getUdpBlockedDefault()
     {
         return this.udpBlockedDefault;
     }

     public void setUdpBlockedDefault( int udpBlockedDefault )
     {
         this.udpBlockedDefault = udpBlockedDefault;
     }

     public void incrUdpBlockedDefault()
     {
         this.udpBlockedDefault++;
     }

     /**
      * Number of udp sessions blocked by a rule.
      *
      * @return Number of udp sessions blocked by a rule.
      */
     @Column(name="udp_block_rule", nullable=false)
     public int getUdpBlockedRule()
     {
         return this.udpBlockedRule;
     }

     public void setUdpBlockedRule( int udpBlockedRule )
     {
         this.udpBlockedRule = udpBlockedRule;
     }

     public void incrUdpBlockedRule()
     {
         this.udpBlockedRule++;
     }

     /**
      * Number of udp sessions passed by the default rule.
      *
      * @return Number of udp sessions passed by the default.
      */
     @Column(name="udp_pass_default", nullable=false)
     public int getUdpPassedDefault()
     {
         return this.udpPassedDefault;
     }

     public void setUdpPassedDefault( int udpPassedDefault )
     {
         this.udpPassedDefault = udpPassedDefault;
     }

     public void incrUdpPassedDefault()
     {
         this.udpPassedDefault++;
     }

     /**
      * Number of udp sessions passed by a rule.
      *
      * @return Number of udp sessions passed by a rule.
      */
     @Column(name="udp_pass_rule", nullable=false)
     public int getUdpPassedRule()
     {
         return this.udpPassedRule;
     }

     public void setUdpPassedRule( int udpPassedRule )
     {
         this.udpPassedRule = udpPassedRule;
     }

     public void incrUdpPassedRule()
     {
         this.udpPassedRule++;
     }

     /**
      * Number of icmp sessions blocked by the default rule.
      *
      * @return Number of icmp sessions blocked by the default.
      */
     @Column(name="icmp_block_default", nullable=false)
     public int getIcmpBlockedDefault()
     {
         return this.icmpBlockedDefault;
     }

     public void setIcmpBlockedDefault( int icmpBlockedDefault )
     {
         this.icmpBlockedDefault = icmpBlockedDefault;
     }

     public void incrIcmpBlockedDefault()
     {
         this.icmpBlockedDefault++;
     }

     /**
      * Number of icmp sessions blocked by a rule.
      *
      * @return Number of icmp sessions blocked by a rule.
      */
     @Column(name="icmp_block_rule", nullable=false)
     public int getIcmpBlockedRule()
     {
         return this.icmpBlockedRule;
     }

     public void setIcmpBlockedRule( int icmpBlockedRule )
     {
         this.icmpBlockedRule = icmpBlockedRule;
     }

     public void incrIcmpBlockedRule()
     {
         this.icmpBlockedRule++;
     }

     /**
      * Number of icmp sessions passed by the default rule.
      *
      * @return Number of icmp sessions passed by the default.
      */
     @Column(name="icmp_pass_default", nullable=false)
     public int getIcmpPassedDefault()
     {
         return this.icmpPassedDefault;
     }

     public void setIcmpPassedDefault( int icmpPassedDefault )
     {
         this.icmpPassedDefault = icmpPassedDefault;
     }

     public void incrIcmpPassedDefault()
     {
         this.icmpPassedDefault++;
     }

     /**
      * Number of icmp sessions passed by a rule.
      *
      * @return Number of icmp sessions passed by a rule.
      */
     @Column(name="icmp_pass_rule", nullable=false)
     public int getIcmpPassedRule()
     {
         return this.icmpPassedRule;
     }

     public void setIcmpPassedRule( int icmpPassedRule )
     {
         this.icmpPassedRule = icmpPassedRule;
     }

     public void incrIcmpPassedRule()
     {
         this.icmpPassedRule++;
     }

     /**
      * Returns true if any of the stats are non-zero, whenever all the stats are zero,
      * a new log event is not created.
      */
     public boolean hasStatistics()
     {
         return (( tcpBlockedDefault  + tcpBlockedRule  + tcpPassedDefault  + tcpPassedRule +
                 udpBlockedDefault  + udpBlockedRule  + udpPassedDefault  + udpPassedRule +
                 icmpBlockedDefault + icmpBlockedRule + icmpPassedDefault + icmpPassedRule ) > 0 );
     }

     // Syslog methods ---------------------------------------------------------

     public void appendSyslog(SyslogBuilder sb)
     {
         sb.startSection("info");
         sb.addField("tcp-blk-def", tcpBlockedDefault);
         sb.addField("tcp-blk-rule", tcpBlockedRule);
         sb.addField("tcp-pass-def", tcpPassedDefault);
         sb.addField("tcp-pass-rule", tcpPassedRule);
         sb.addField("udp-blk-def", udpBlockedDefault);
         sb.addField("udp-blk-rule", udpBlockedRule);
         sb.addField("udp-pass-def", udpPassedDefault);
         sb.addField("udp-pass-rule", udpPassedRule);
         sb.addField("icmp-blk-def", icmpBlockedDefault);
         sb.addField("icmp-blk-rule", icmpBlockedRule);
         sb.addField("icmp-pass-def", icmpPassedDefault);
         sb.addField("icmp-pass-rule", icmpPassedRule);
     }

     @Transient
     public String getSyslogId()
     {
         return "Statistic";
     }

     @Transient
     public SyslogPriority getSyslogPriority()
     {
         return SyslogPriority.INFORMATIONAL; // statistics or normal operation
     }
}

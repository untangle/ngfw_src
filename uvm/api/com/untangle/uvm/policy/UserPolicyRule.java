/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.policy;

import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.apache.log4j.Logger;

import com.untangle.uvm.node.IPSessionDesc;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.intf.IntfMatcher;
import com.untangle.uvm.node.firewall.ip.IPMatcher;
import com.untangle.uvm.node.firewall.port.PortMatcher;
import com.untangle.uvm.node.firewall.protocol.ProtocolMatcher;
import com.untangle.uvm.node.firewall.time.DayOfWeekMatcher;
import com.untangle.uvm.node.firewall.user.UserMatcher;

/**
 * These are the policy rules that are created by the user.  
 *
 * @author
 * @version 1.0
 */
@Entity
@Table(name="u_user_policy_rule", schema="settings")
@SuppressWarnings("serial")
public class UserPolicyRule extends PolicyRule
{
    private static final Logger logger = Logger.getLogger(UserPolicyRule.class);
    
    private static SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("H:m");
        
    /* settings */
    private ProtocolMatcher protocol;

    /* True if this matches client interface */
    private IntfMatcher clientIntf = IntfMatcher.getAnyMatcher();

    /* True if this matches the server interface */
    private IntfMatcher serverIntf = IntfMatcher.getAnyMatcher();

    private IPMatcher clientAddr;
    private IPMatcher serverAddr;

    private PortMatcher clientPort;
    private PortMatcher serverPort;

    private String startTimeString;
    private String endTimeString;

    private DayOfWeekMatcher dayOfWeek = DayOfWeekMatcher.getAnyMatcher();

    private boolean invertEntireDuration;

    private UserMatcher user = UserMatcher.getAnyMatcher();

    // constructors -----------------------------------------------------------

    public UserPolicyRule() { }

    public UserPolicyRule(IntfMatcher clientIntf, IntfMatcher serverIntf,
                          Policy policy, ProtocolMatcher protocol,
                          IPMatcher clientAddr, IPMatcher serverAddr,
                          PortMatcher clientPort, PortMatcher serverPort,
                          String startTimeString, String endTimeString,
                          DayOfWeekMatcher dayOfWeek, UserMatcher user,
                          boolean live, boolean invertEntireDuration)
    {
        super(live, policy);
        this.clientIntf = clientIntf;
        this.serverIntf = serverIntf;
        this.protocol = protocol;
        this.clientAddr = clientAddr;
        this.serverAddr = serverAddr;
        this.clientPort = clientPort;
        this.serverPort = serverPort;
        this.startTimeString = startTimeString;
        this.endTimeString = endTimeString;
        this.dayOfWeek = dayOfWeek;
        this.user = user;
        this.invertEntireDuration = invertEntireDuration;
    }

    // PolicyRule methods -----------------------------------------------------

    public boolean matches(IPSessionDesc sd)
    {
        boolean basicOk =
            isLive()
            && clientIntf.isMatch(sd.clientIntf())
            && serverIntf.isMatch(sd.serverIntf())
            && protocol.isMatch(sd.protocol())
            && clientAddr.isMatch(sd.clientAddr())
            && serverAddr.isMatch(sd.serverAddr())
            && clientPort.isMatch(sd.clientPort())
            && serverPort.isMatch(sd.serverPort());

        if (!basicOk)
            return false;

        boolean invertDuration = false;
        // Note that we assume we get back something from the database with
        // meaningless fields other than time ones.
        Calendar now = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        Date startTime = null;
        Date endTime = null;
        try {
            synchronized (TIME_FORMATTER) {
                startTime = TIME_FORMATTER.parse(this.startTimeString);
                endTime = TIME_FORMATTER.parse(this.endTimeString);
            }
        } catch (java.text.ParseException e) {
            logger.warn("Invalid date in policy rule (" + startTimeString + "," + endTimeString + ")",e);
            startTime = null;
            endTime = null;
        } catch (java.lang.NumberFormatException e) {
            logger.warn("Invalid date in policy rule (" + startTimeString + "," + endTimeString + ")",e);
            startTime = null;
            endTime = null;
        } finally {
            /**
             * If there was any problem just match on all times
             */
            if (startTime == null || endTime == null) {
                try {
                    synchronized (TIME_FORMATTER) {
                        startTime = TIME_FORMATTER.parse("00:00");
                        endTime = TIME_FORMATTER.parse("23:59");
                    }
                }
                catch (Exception e) {
                    logger.warn("Invalid date in policy rule (00:00,23:59)",e);
                }
            }
        }
        start.setTime(startTime);
        start.set(Calendar.YEAR, now.get(Calendar.YEAR));
        start.set(Calendar.MONTH, now.get(Calendar.MONTH));
        start.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
        end.setTime(endTime);
        end.set(Calendar.YEAR, now.get(Calendar.YEAR));
        end.set(Calendar.MONTH, now.get(Calendar.MONTH));
        end.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

        // Special case to handle 3:00 -> 2:59, for instance.
        if (end.before(start)) {
            Calendar temp = end;
            end = start;
            start = temp;
            start.add(Calendar.MINUTE, 1);
            end.add(Calendar.MINUTE, -1);
            invertDuration = true;
        }

        int nowhour = now.get(Calendar.HOUR_OF_DAY);
        int starthour = start.get(Calendar.HOUR_OF_DAY);
        int endhour = end.get(Calendar.HOUR_OF_DAY);

        boolean beforeStart =
            // Check the start
            (nowhour < starthour ||
             (nowhour == starthour && now.get(Calendar.MINUTE) < start.get(Calendar.MINUTE)));

        // Check the end.
        boolean afterEnd =
            (nowhour > endhour ||
             (nowhour == endhour && now.get(Calendar.MINUTE) > end.get(Calendar.MINUTE)));

        boolean reject = (beforeStart || afterEnd) ^ invertDuration;

        if (!reject) {
            Date dnow = now.getTime();
            reject = !dayOfWeek.isMatch(dnow);
        }

        boolean durationTest = invertEntireDuration ? reject : !reject;

        boolean userMatch = user.isMatch(sd.user());

        return durationTest && userMatch;
    }

    // accessors --------------------------------------------------------------

    /**
     * source IntfMatcher
     *
     * @return the source IP matcher.
     */
    @Column(name="client_intf_matcher")
    @Type(type="com.untangle.uvm.type.firewall.IntfMatcherUserType")
    public IntfMatcher getClientIntf()
    {
        return clientIntf;
    }

    public void setClientIntf( IntfMatcher clientIntf )
    {
        this.clientIntf = clientIntf;
    }

    /**
     * destination IntfMatcher
     *
     * @return the destination IP matcher.
     */
    @Column(name="server_intf_matcher")
    @Type(type="com.untangle.uvm.type.firewall.IntfMatcherUserType")
    public IntfMatcher getServerIntf()
    {
        return serverIntf;
    }

    public void setServerIntf( IntfMatcher serverIntf )
    {
        this.serverIntf = serverIntf;
    }

    /**
     * Protocol matcher
     *
     * @return the protocol matcher.
     */
    @Column(name="protocol_matcher")
    @Type(type="com.untangle.uvm.type.firewall.ProtocolMatcherUserType")
    public ProtocolMatcher getProtocol()
    {
        return protocol;
    }

    public void setProtocol( ProtocolMatcher protocol )
    {
        this.protocol = protocol;
    }

    /**
     * User matcher
     *
     * @return the user matcher.
     */
    @Column(name="user_matcher")
    @Type(type="com.untangle.uvm.type.firewall.UserMatcherUserType")
    public UserMatcher getUser()
    {
        return user;
    }

    public void setUser( UserMatcher user )
    {
        this.user = user;
    }

    /**
     * Time the session may not start before
     *
     * @return the time of day that the session must start after
     */
    @Column(name="start_time_string")
    public String getStartTimeString()
    {
        return startTimeString;
    }

    public void setStartTimeString(String startTime)
    {
        this.startTimeString = startTime;
    }

    /**
     * Time the session may not start after
     *
     * @return the time of day that the session must start before
     */
    @Column(name="end_time_string")
    public String getEndTimeString()
    {
        return endTimeString;
    }

    public void setEndTimeString(String endTime)
    {
        this.endTimeString = endTime;
    }
    
    /**
     * Day of Week matcher
     *
     * @return the day of week matcher.
     */
    @Column(name="day_of_week_matcher")
    @Type(type="com.untangle.uvm.type.firewall.DayOfWeekMatcherUserType")
    public DayOfWeekMatcher getDayOfWeek()
    {
        return dayOfWeek;
    }

    public void setDayOfWeek( DayOfWeekMatcher dayOfWeek )
    {
        this.dayOfWeek = dayOfWeek;
    }

    /**
     * client address IPMatcher
     *
     * @return the client address IP matcher.
     */
    @Column(name="client_ip_matcher")
    @Type(type="com.untangle.uvm.type.firewall.IPMatcherUserType")
    public IPMatcher getClientAddr()
    {
        return clientAddr;
    }

    public void setClientAddr(IPMatcher clientAddr)
    {
        this.clientAddr = clientAddr;
    }

    /**
     * server address IPMatcher
     *
     * @return the server address IP matcher.
     */
    @Column(name="server_ip_matcher")
    @Type(type="com.untangle.uvm.type.firewall.IPMatcherUserType")
    public IPMatcher getServerAddr()
    {
        return serverAddr;
    }

    public void setServerAddr(IPMatcher serverAddr)
    {
        this.serverAddr = serverAddr;
    }

    /**
     * client port PortMatcher
     *
     * @return the client port matcher.
     */
    @Column(name="client_port_matcher")
    @Type(type="com.untangle.uvm.type.firewall.PortMatcherUserType")
    public PortMatcher getClientPort()
    {
        return clientPort;
    }

    public void setClientPort(PortMatcher clientPort)
    {
        this.clientPort = clientPort;
    }

    /**
     * server port PortMatcher
     *
     * @return the server port matcher.
     */
    @Column(name="server_port_matcher")
    @Type(type="com.untangle.uvm.type.firewall.PortMatcherUserType")
    public PortMatcher getServerPort()
    {
        return serverPort;
    }

    public void setServerPort(PortMatcher serverPort)
    {
        this.serverPort = serverPort;
    }

    /**
     * Specifies if we should invert sense of the entire duration.
     * So a duration of 0900 -> 1700 M,T,W,R,F when inverted would
     * mean:
     *  all Saturday, all Sunday, and Monday through Friday
     *  from 0000 -> 0859 and 1701 -> 2359
     *
     * @return true if entire duration is inverted
     */
    @Column(name="invert_entire_duration", nullable=false)
    public boolean isInvertEntireDuration()
    {
        return invertEntireDuration;
    }

    public void setInvertEntireDuration(boolean invertEntireDuration)
    {
        this.invertEntireDuration = invertEntireDuration;
    }
    
    @Transient
    public boolean isSameRow(UserPolicyRule pr)
    {
        return getId().equals(pr.getId());
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof UserPolicyRule)) {
            return false;
        } else {
            UserPolicyRule pr = (UserPolicyRule)o;
            return ((policy == null ? pr.policy == null : policy.equals(pr.policy)) &&
                    (clientIntf == null ? pr.clientIntf == null : clientIntf.equals(pr.clientIntf)) &&
                    (serverIntf == null ? pr.serverIntf == null : serverIntf.equals(pr.serverIntf)) &&
                    (protocol == null ? pr.protocol == null : protocol.equals(pr.protocol)) &&
                    (clientAddr == null ? pr.clientAddr == null : clientAddr.equals(pr.clientAddr)) &&
                    (serverAddr == null ? pr.serverAddr == null : serverAddr.equals(pr.serverAddr)) &&
                    (clientPort == null ? pr.clientPort == null : clientPort.equals(pr.clientPort)) &&
                    (serverPort == null ? pr.serverPort == null : serverPort.equals(pr.serverPort)));
        }
    }

    public int hashCode()
    {
        // Should be fixed to include other stuff, once those are fixed. XXX
        return (null == policy ? 0 : policy.hashCode()) + clientIntf.toDatabaseString().hashCode() * 7 + serverIntf.toDatabaseString().hashCode() * 5;
    }
}

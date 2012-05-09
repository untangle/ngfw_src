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

package com.untangle.uvm.snmp;

import java.io.Serializable;

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
 *   represents the UID/PWD of someone who can read the contents of the MIB on the UVM
 *   machine.  This user has <b>read only access</b>.  There is no read/write access
 *   possible with our implementation (for safety reasons).
 *   </li>
 *   <li>
 *   The property {@link #getPort Port} property is the port on-which the UVM
 *   will listen for UDP SNMP messages.  The default is 161.
 *   </li>
 *   <li>
 *   {@link #getSysContact SysContact} and {@link #getSysLocation SysLocation} are
 *   related in that they are informational properties, useful only if someone is
 *   monitoring the UVM as one of many devices.  These fields are in no way required,
 *   but may make management easier.
 *   </li>
 *   <li>
 *   {@link #setSendTraps SendTraps} controls if SNMP traps are sent from the UVM.  If this
 *   is true, then {@link #setTrapHost TrapHost}, {@link #setTrapPort TrapPort}, and
 *   {@link #setTrapCommunity TrapCommunity} must be set.
 *   </li>
 * </ul>
 * <br><br>
 * If Snmp {@link #isEnabled is enabled}, the {@link #getCommunityString CommunityString}
 * must be set (everything else can be defaulted).  If {@link #isSendTraps traps are enabled},
 * then {@link #setTrapHost TrapHost} and {@link #setTrapCommunity TrapCommunity} must be
 * set.
 */
@SuppressWarnings("serial")
public class SnmpSettings implements Serializable {

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

    public Long getId() {
        return m_id;
    }

    public void setId(Long id) {
        m_id = id;
    }

    public boolean isEnabled() {
        return m_enabled;
    }

    public void setEnabled(boolean enabled) {
        m_enabled = enabled;
    }

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
     */
    public String getCommunityString() {
        return m_communityString;
    }

    public void setCommunityString(String s) {
        m_communityString = s;
    }

    /**
     * @return the contact info
     */
    public String getSysContact() {
        return m_sysContact;
    }

    public void setSysContact(String s) {
        m_sysContact = s;
    }

    /**
     * @return the system location
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

    public boolean isSendTraps() {
        return m_sendTraps;
    }

    public void setTrapHost(String trapHost) {
        m_trapHost = trapHost;
    }

    /**
     * @return the trap host
     */
    public String getTrapHost() {
        return m_trapHost;
    }

    public void setTrapCommunity(String tc) {
        m_trapCommunity = tc;
    }

    /**
     * @return the trap community String
     */
    public String getTrapCommunity() {
        return m_trapCommunity;
    }


    public void setTrapPort(int tp) {
        m_trapPort = tp;
    }

    public int getTrapPort() {
        return m_trapPort;
    }

}

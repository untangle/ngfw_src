/*
 * $HeadURL: svn://chef/work/src/uvm-lib/api/com/untangle/uvm/RadiusServerSettings.java $
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

package com.untangle.uvm.addrbook;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Settings for Radius
 */
@Entity
@Table(name="u_radius_server_settings")
public class RadiusServerSettings implements Serializable
{
    public enum AuthenticationMethod { CLEARTEXT, PAP, CHAP, MSCHAPV1, MSCHAPV2};


    private boolean isEnabled = false;
    private Long id;
    private String server;
    private int port = 1812;
    private String sharedSecret;
    
    private String authenticationMethodValue = AuthenticationMethod.PAP.toString();
    
    public RadiusServerSettings() { }

    public RadiusServerSettings(boolean isEnabled,
            String server,
            int port,
            String sharedSecret,
            AuthenticationMethod authenticationMethod) {
        this.setEnabled(isEnabled);
        this.server = server;
        this.port = port;
        this.sharedSecret = sharedSecret;
        setAuthenticationMethod(authenticationMethod);
    }

    @SuppressWarnings("unused")
    @Id
    @Column(name="settings_id")
    @GeneratedValue
    private Long getId() {
        return id;
    }

    @SuppressWarnings("unused")
    private void setId(Long id) {
        this.id = id;
    }
    
    @Column(name="enabled")
    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    @Column(name="server")
    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    @Column(name="port", nullable=false)
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Column(name="shared_secret")
    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    @SuppressWarnings("unused")
    @Column(name="auth_method")
    private String getAuthenticationMethodValue()
    {
        return this.authenticationMethodValue;
    }
    
    @SuppressWarnings("unused")
    private void setAuthenticationMethodValue(String newValue ) {
        this.authenticationMethodValue = newValue;
    }
    
    @Transient
    public AuthenticationMethod getAuthenticationMethod()
    {
        return AuthenticationMethod.valueOf(this.authenticationMethodValue);
    }
    
    public void setAuthenticationMethod(AuthenticationMethod newValue ) {
        this.authenticationMethodValue = newValue.toString();
    }
}

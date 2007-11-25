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

package com.untangle.gui.configuration;

import java.lang.reflect.Constructor;
import java.util.List;
import javax.swing.JPanel;

import com.untangle.gui.node.CompoundSettings;
import com.untangle.gui.util.Util;
import com.untangle.uvm.addrbook.*;
import com.untangle.uvm.user.WMISettings;
import org.apache.log4j.Logger;


public class DirectoryCompoundSettings implements CompoundSettings {


    private final Logger logger = Logger.getLogger(getClass());

    // ADDRESS BOOK SETTINGS //
    private AddressBookSettings addressBookSettings;

    public AddressBookSettings getAddressBookSettings(){ return addressBookSettings; }

    public void setAddressBookSettings(AddressBookSettings inAddressBookSettings){ addressBookSettings = inAddressBookSettings; }

    // ADDRESS BOOK CONFIGURATION //
    private AddressBookConfiguration addressBookConfiguration;
    public AddressBookConfiguration getAddressBookConfiguration(){ return addressBookConfiguration; }
    public void setAddressBookConfiguration(AddressBookConfiguration inAddressBookConfiguration){ addressBookConfiguration = inAddressBookConfiguration; }

    // LOCAL USER ENTRIES //
    List<UserEntry> localUserList;
    public List<UserEntry> getLocalUserList(){ return localUserList; }
    public void setLocalUserList(List<UserEntry> inUserEntryList){ localUserList = inUserEntryList; }

    // WMI Settings
    private WMISettings wmiSettings;
    public WMISettings getWMISettings(){ return wmiSettings; }
    public void setWMISettings(WMISettings inWmiSettings){ wmiSettings = inWmiSettings; }

    // JPanels
    private JPanel localJPanel;
    public JPanel getLocalJPanel(){ return localJPanel; }
    private JPanel adJPanel;
    public JPanel getRemoteADJPanel(){ return adJPanel; }
    private JPanel adlsJPanel;
    public JPanel getRemoteADLSJPanel(){ return adlsJPanel; }


    public void save() throws Exception {
        addressBookSettings.setAddressBookConfiguration(addressBookConfiguration);
        Util.getAddressBook().setAddressBookSettings(addressBookSettings);
        Util.getAddressBook().setLocalUserEntries(localUserList);
        Util.getPhoneBook().setWMISettings(wmiSettings);
    }

    public void refresh() throws Exception {
        addressBookSettings = Util.getAddressBook().getAddressBookSettings();
        addressBookConfiguration = addressBookSettings.getAddressBookConfiguration();
        localUserList = Util.getAddressBook().getLocalUserEntries();
        wmiSettings = Util.getPhoneBook().getWMISettings();

        if (localJPanel==null) {
            try {
                // LOCAL DIRECTORY ////////
                Class localJPanelClass = Util.getClassLoader().mLoadClass( "com.untangle.gui.configuration.DirectoryLocalJPanel" );
                Constructor localJPanelConstructor = localJPanelClass.getConstructor( new Class[]{} );
                localJPanel = (JPanel) localJPanelConstructor.newInstance(new Object[]{});
            }
            catch (Exception e) {
                logger.warn("Unable to load: Local Directory", e);
                throw e;
            }
        }

        if (adJPanel==null) {
            try {
                // REMOTE ACTIVE DIRECTORY ////////
                Class adJPanelClass = Util.getClassLoader().mLoadClass( "com.untangle.gui.configuration.DirectoryRemoteADJPanel" );
                Constructor adJPanelConstructor = adJPanelClass.getConstructor( new Class[]{} );
                adJPanel = (JPanel) adJPanelConstructor.newInstance(new Object[]{});
            }
            catch (Exception e) {
                logger.warn("Unable to load: Remote Directory", e);
                throw e;
            }
        }

        if (adlsJPanel==null) {
            try {
                // REMOTE ACTIVE DIRECTORY ////////
                Class adlsJPanelClass = Util.getClassLoader().mLoadClass( "com.untangle.gui.configuration.AdlsJPanel" );
                Constructor adlsJPanelConstructor = adlsJPanelClass.getConstructor( new Class[]{} );
                adlsJPanel = (JPanel) adlsJPanelConstructor.newInstance(new Object[]{});
            }
            catch (Exception e) {
                logger.warn("Unable to load: Remote Directory", e);
                throw e;
            }
        }

    }

    public void validate() throws Exception {

    }

}

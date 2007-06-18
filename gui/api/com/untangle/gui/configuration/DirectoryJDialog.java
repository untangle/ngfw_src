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

import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

import com.untangle.gui.node.Changeable;
import com.untangle.gui.node.CompoundSettings;
import com.untangle.gui.node.Refreshable;
import com.untangle.gui.node.Savable;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.widgets.premium.*;
import com.untangle.uvm.license.ProductIdentifier;
import org.apache.log4j.Logger;


public class DirectoryJDialog extends MConfigJDialog {

    private static final String NAME_DIRECTORY_CONFIG  = "User Directory Config";
    private static final String NAME_LOCAL_DIRECTORY   = "Local Directory";
    private static final String NAME_REMOTE_ACTIVE_DIRECTORY   = "Remote Active Directory (AD) Server";
    private static final String NAME_PREMIUM_PURCHASE   = "Feature Not Available";

    private final Logger logger = Logger.getLogger(getClass());

    public DirectoryJDialog( Frame parentFrame ) {
        super(parentFrame);
        setTitle(NAME_DIRECTORY_CONFIG);
        setHelpSource("user_directory_config");
        if (Util.getIsPremium(ProductIdentifier.ADDRESS_BOOK)) {
            Class compoundSettingsClass = Util.getClassLoader().mLoadClass( "com.untangle.gui.configuration.DirectoryCompoundSettings" );
            try {
                Constructor compoundSettingsConstructor = compoundSettingsClass.getConstructor( new Class[]{} );
                compoundSettings = (CompoundSettings) compoundSettingsConstructor.newInstance( new Object[]{} );
            }
            catch(Exception e) {
                logger.warn("Unable to load: Directory Settings",e);
            }
        }
    }

    public DirectoryJDialog( Dialog parentDialog ) {
        super(parentDialog);
        setTitle(NAME_DIRECTORY_CONFIG);
        setHelpSource("user_directory_config");
        if (Util.getIsPremium(ProductIdentifier.ADDRESS_BOOK)) {
            Class compoundSettingsClass = Util.getClassLoader().mLoadClass( "com.untangle.gui.configuration.DirectoryCompoundSettings" );
            try {
                Constructor compoundSettingsConstructor = compoundSettingsClass.getConstructor( new Class[]{} );
                compoundSettings = (CompoundSettings) compoundSettingsConstructor.newInstance( new Object[]{} );
            }
            catch(Exception e) {
                logger.warn("Unable to load: Directory Settings",e);
            }
        }
    }

    protected Dimension getMinSize(){
        return new Dimension(640, 675);
    }

    protected void generateGui(){
        if (Util.getIsPremium(ProductIdentifier.ADDRESS_BOOK)) {

            // LOCAL DIRECTORY ////////
            try {
                Method localJPanelMethod = compoundSettings.getClass().getDeclaredMethod("getLocalJPanel", new Class[]{});
                JPanel localJPanel = (JPanel) localJPanelMethod.invoke(compoundSettings, new Object[]{});

                addTab(NAME_LOCAL_DIRECTORY, null, localJPanel);
                addSavable(NAME_LOCAL_DIRECTORY, (Savable) localJPanel);
                addRefreshable(NAME_LOCAL_DIRECTORY, (Refreshable) localJPanel);
                ((Changeable) localJPanel).setSettingsChangedListener(this);
            }
            catch(Exception e) {
                logger.warn("Unable to load: Local Directory Panel",e);
            }

            // REMOTE ACTIVE DIRECTORY ////////
            try {
                Method adJPanelMethod = compoundSettings.getClass().getDeclaredMethod("getRemoteADJPanel", new Class[]{});
                JPanel adJPanel = (JPanel) adJPanelMethod.invoke(compoundSettings, new Object[]{});
                addScrollableTab(null, NAME_REMOTE_ACTIVE_DIRECTORY, null, adJPanel, false, true);
                addSavable(NAME_REMOTE_ACTIVE_DIRECTORY, (Savable) adJPanel);
                addRefreshable(NAME_REMOTE_ACTIVE_DIRECTORY, (Refreshable) adJPanel);
                ((Changeable) adJPanel).setSettingsChangedListener(this);
            }
            catch(Exception e) {
                logger.warn("Unable to load: Remote Directory Panel",e);
            }

        }
        else {
            PremiumJPanel directoryPremiumJPanel = new PremiumJPanel();
            addTab(NAME_PREMIUM_PURCHASE, null, directoryPremiumJPanel);
        }

    }

}

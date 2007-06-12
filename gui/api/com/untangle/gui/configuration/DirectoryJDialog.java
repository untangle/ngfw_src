/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
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
import org.apache.log4j.Logger;


public class DirectoryJDialog extends MConfigJDialog {

    private static final String DIRECTORY_JAR_NAME  = "charon";

    private static final String NAME_DIRECTORY_CONFIG  = "User Directory Config";
    private static final String NAME_LOCAL_DIRECTORY   = "Local Directory";
    private static final String NAME_REMOTE_ACTIVE_DIRECTORY   = "Remote Active Directory (AD) Server";
    private static final String NAME_PREMIUM_PURCHASE   = "Feature Not Available";

    private final Logger logger = Logger.getLogger(getClass());

    public DirectoryJDialog( Frame parentFrame ) {
        super(parentFrame);
        setTitle(NAME_DIRECTORY_CONFIG);
        setHelpSource("user_directory_config");
        if (Util.getIsPremium()) {
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
        if (Util.getIsPremium()) {
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
        if (Util.getIsPremium()) {

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

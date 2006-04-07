/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.upgrade;

import java.awt.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.coloredTable.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.policy.*;
import com.metavize.mvvm.toolbox.InstallProgress;
import com.metavize.mvvm.toolbox.MackageDesc;
import com.metavize.mvvm.toolbox.UpgradeSettings;

public class UpgradeJDialog extends MConfigJDialog {

    public static final String NAME_TITLE    = "Upgrade Config";
    public static final String NAME_UPGRADE  = "Upgrade";
    public static final String NAME_SETTINGS = "Settings";

    public UpgradeJDialog(Frame parentFrame) {
	super(parentFrame);
	setTitle(NAME_TITLE);
	setResizable(false);
	compoundSettings = new UpgradeCompoundSettings();
    }

    protected Dimension getMinSize(){
	return new Dimension(660,480);
    }

    protected Dimension getMaxSize(){
	return new Dimension(660,480);
    }

    public void generateGui(){
        // PROCEDURE //
	UpgradeProcessJPanel upgradeProcessJPanel = new UpgradeProcessJPanel();
	addTab(NAME_UPGRADE, null, upgradeProcessJPanel);
	addRefreshable(NAME_UPGRADE, upgradeProcessJPanel);

        // SETTINGS //
	UpgradeSettingsJPanel upgradeSettingsJPanel = new UpgradeSettingsJPanel();
	addTab(NAME_SETTINGS, null, upgradeSettingsJPanel);
        addSavable(NAME_SETTINGS, upgradeSettingsJPanel);
        addRefreshable(NAME_SETTINGS, upgradeSettingsJPanel);
    }

}

/*
 * $HeadURL:$
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



package com.untangle.node.reporting.gui;


import com.untangle.gui.node.*;



public class MNodeControlsJPanel extends com.untangle.gui.node.MNodeControlsJPanel{

    private static final String NAME_DIRECTORY = "IP Address to User Map";
    private static final String NAME_EMAIL_RECIPIENTS = "Recipients";
    private static final String NAME_VIEW = "View Reports";
    private static final String NAME_GENERATION_OPTIONS = "Generation Settings";

    public MNodeControlsJPanel(MNodeJPanel mNodeJPanel) {
        super(mNodeJPanel);
    }

    public void generateGui(){
        // LAUNCH BUTTON /////
        BrowserLaunchJPanel browserLaunchJPanel = new BrowserLaunchJPanel();
        addTab(NAME_VIEW, null, browserLaunchJPanel);

        // REPORTS EMAILING RECIPIENTS LIST /////
        EmailConfigJPanel emailConfigJPanel = new EmailConfigJPanel();
        addTab(NAME_EMAIL_RECIPIENTS, null, emailConfigJPanel);
        addSavable(NAME_EMAIL_RECIPIENTS, emailConfigJPanel);
        addRefreshable(NAME_EMAIL_RECIPIENTS, emailConfigJPanel);
        emailConfigJPanel.setSettingsChangedListener(this);

        // DIRECTORY ///////
        DirectoryConfigJPanel directoryConfigJPanel = new DirectoryConfigJPanel();
        addTab(NAME_DIRECTORY, null, directoryConfigJPanel);
        addSavable(NAME_DIRECTORY, directoryConfigJPanel);
        addRefreshable(NAME_DIRECTORY, directoryConfigJPanel);
        directoryConfigJPanel.setSettingsChangedListener(this);

        // GENERATION SETTINGS //
        GenerationConfigJPanel generationConfigJPanel = new GenerationConfigJPanel();
        addScrollableTab(null, NAME_GENERATION_OPTIONS, null, generationConfigJPanel, false, true);
        addSavable(NAME_GENERATION_OPTIONS, generationConfigJPanel);
        addRefreshable(NAME_GENERATION_OPTIONS, generationConfigJPanel);
        generationConfigJPanel.setSettingsChangedListener(this);
    }

}

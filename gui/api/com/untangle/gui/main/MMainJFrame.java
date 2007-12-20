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

package com.untangle.gui.main;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.*;
import java.net.MalformedURLException;
import java.net.URL;
import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import com.untangle.gui.configuration.*;
import com.untangle.gui.node.*;
import com.untangle.gui.pipeline.*;
import com.untangle.gui.store.*;
import com.untangle.gui.upgrade.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.uvm.*;
import com.untangle.uvm.node.*;
import com.untangle.uvm.policy.*;
import com.untangle.uvm.security.*;
import com.untangle.uvm.toolbox.MackageDesc;

public class MMainJFrame extends javax.swing.JFrame {
    // CONSTANTS
    private static final Dimension MIN_SIZE = new Dimension(1000, 480);
    private static final Dimension MAX_SIZE = new Dimension(2560, 1600); // the 30-inch cinema display max

    private ImageIcon mainLeftBackground;
    private ImageIcon mainRightBackground;

    public MMainJFrame() {
        Util.setMMainJFrame(this);

        // INIT GUI
        mainLeftBackground = new ImageIcon(getClass().getResource("/com/untangle/gui/main/MainBackgroundLeft_222x100.png"));
        mainRightBackground = new ImageIcon(getClass().getResource("/com/untangle/gui/main/MainBackgroundRight_1398x100.png"));
        initComponents();

        mTabbedPane.addChangeListener(new TabSelectionChangeListener());

        storeJScrollPane.getVerticalScrollBar().setUnitIncrement(5);
        toolboxJScrollPane.getVerticalScrollBar().setUnitIncrement(5);
        configurationJScrollPane.getVerticalScrollBar().setUnitIncrement(5);
        storeJScrollPane.getVerticalScrollBar().setFocusable(false);
        toolboxJScrollPane.getVerticalScrollBar().setFocusable(false);
        configurationJScrollPane.getVerticalScrollBar().setFocusable(false);
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-1024)/2, (screenSize.height-768)/2, 1024, 768);

        PolicyStateMachine policyStateMachine = new PolicyStateMachine(mTabbedPane,Util.getMRackJPanel(),toolboxJScrollPane,
                                                                       utilToolboxJPanel,policyToolboxJPanel,coreToolboxJPanel,
                                                                       storeScrollJPanel,Util.getMPipelineJPanel().getJScrollPane());
        Util.addShutdownable("PolicyStateMachine", policyStateMachine);
        untangleJButton.addActionListener(policyStateMachine);

        // UPDATE/UPGRADE
        UpdateCheckThread updateCheckThread = new UpdateCheckThread();
        Util.addShutdownable("UpdateCheckThread", updateCheckThread);
    }

    private class BackgroundJPanel extends JPanel {
        int iconLeftWidth = mainLeftBackground.getIconWidth();
        int iconLeftHeight = mainLeftBackground.getIconHeight();
        int iconRightWidth = mainRightBackground.getIconWidth();
        int iconRightHeight = mainRightBackground.getIconHeight();
        Rectangle leftClipMask  = new Rectangle(0,0,iconLeftWidth,Integer.MAX_VALUE);
        Rectangle rightClipMask = new Rectangle(iconLeftWidth,0,Integer.MAX_VALUE,Integer.MAX_VALUE);

        public void paintComponent(Graphics g){
            Rectangle clipRect = g.getClipBounds();
            Rectangle leftClip  = leftClipMask.intersection(clipRect);
            Rectangle rightClip = rightClipMask.intersection(clipRect);

            if(!leftClip.isEmpty()){
                int xStart = leftClip.x/iconLeftWidth;
                int xEnd   = (leftClip.x+leftClip.width)/iconLeftWidth;
                int yStart = leftClip.y/iconLeftHeight;
                int yEnd   = (leftClip.y+leftClip.height)/iconLeftHeight;
                for(int x=xStart; x<=xEnd; x++){
                    for(int y=yStart; y<=yEnd; y++){
                        mainLeftBackground.paintIcon(this, g, x*iconLeftWidth, y*iconLeftHeight);
                    }
                }
            }

            if(!rightClip.isEmpty()){
                int xStart = (rightClip.x-iconLeftWidth)/iconRightWidth;
                int xEnd   = ((rightClip.x-iconLeftWidth)+rightClip.width)/iconRightWidth;
                int yStart = rightClip.y/iconRightHeight;
                int yEnd   = (rightClip.y+rightClip.height)/iconRightHeight;
                for(int x=xStart; x<=xEnd; x++){
                    for(int y=yStart; y<=yEnd; y++){
                        mainRightBackground.paintIcon(this, g, iconLeftWidth + x*iconRightWidth, y*iconRightHeight);
                    }
                }
            }
        }
    }


    private class TabSelectionChangeListener implements ChangeListener {
        private int lastSelection = 0;
        public TabSelectionChangeListener(){}
        public void stateChanged(ChangeEvent e){
            int newSelection = mTabbedPane.getSelectedIndex();
            String lastTitle = mTabbedPane.getTitleAt(lastSelection);
            lastTitle = lastTitle.substring(9, lastTitle.length()-11);
            mTabbedPane.setTitleAt(lastSelection, lastTitle);
            String newTitle = mTabbedPane.getTitleAt(newSelection);
            newTitle = "<html><b>" + newTitle + "</b></html>";
            mTabbedPane.setTitleAt(newSelection, newTitle);
            lastSelection = newSelection;
        }
    }


    public Dimension getMinimumSize(){ return MIN_SIZE; } // used for form resizing

    class AAJButton extends JButton {
        public void paintComponent(Graphics g){
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            super.paintComponent(g);
        }
    }

    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        controlsJPanel = new javax.swing.JPanel();
        untangleJButton = new javax.swing.JButton();
        mTabbedPane = new javax.swing.JTabbedPane();
        storeJPanel = new javax.swing.JPanel();
        storeJScrollPane = new javax.swing.JScrollPane();
        storeScrollJPanel = new javax.swing.JPanel();
        storeSpacerJPanel = new javax.swing.JPanel();
        toolboxJPanel = new javax.swing.JPanel();
        toolboxJScrollPane = new javax.swing.JScrollPane();
        toolboxScrollJPanel = new javax.swing.JPanel();
        policyToolboxJPanel = new javax.swing.JPanel();
        coreToolboxJPanel = new javax.swing.JPanel();
        utilToolboxJPanel = new javax.swing.JPanel();
        toolboxSpacerJPanel = new javax.swing.JPanel();
        configurationJPanel = new javax.swing.JPanel();
        configurationJScrollPane = new javax.swing.JScrollPane();
        jPanel8 = new javax.swing.JPanel();
        networkJButton = new AAJButton();
        remoteJButton = new AAJButton();
        emailJButton = new AAJButton();
        directoryJButton = new AAJButton();
        backupJButton = new AAJButton();
        maintenanceJButton = new AAJButton();
        upgradeJButton = new AAJButton();
        aboutJButton = new AAJButton();
        configurationSpacerJPanel1 = new javax.swing.JPanel();
        helpJButton = new AAJButton();
        mPipelineJPanel = new com.untangle.gui.pipeline.MPipelineJPanel();
        backgroundJPanel = new BackgroundJPanel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Untangle Client");
        setFocusCycleRoot(false);
        setIconImage((new javax.swing.ImageIcon( this.getClass().getResource("/com/untangle/gui/main/Logo16x16.png"))).getImage());
        addComponentListener(new java.awt.event.ComponentAdapter() {
                public void componentResized(java.awt.event.ComponentEvent evt) {
                    formComponentResized(evt);
                }
            });
        addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent evt) {
                    exitForm(evt);
                }
            });

        controlsJPanel.setLayout(new java.awt.GridBagLayout());

        controlsJPanel.setFocusable(false);
        controlsJPanel.setMaximumSize(new java.awt.Dimension(220, 2147483647));
        controlsJPanel.setMinimumSize(new java.awt.Dimension(220, 427));
        controlsJPanel.setOpaque(false);
        controlsJPanel.setPreferredSize(new java.awt.Dimension(220, 410));
        untangleJButton.setBorderPainted(false);
        untangleJButton.setContentAreaFilled(false);
        untangleJButton.setDoubleBuffered(true);
        untangleJButton.setFocusPainted(false);
        untangleJButton.setFocusable(false);
        untangleJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        untangleJButton.setMargin(new java.awt.Insets(1, 3, 3, 3));
        untangleJButton.setMaximumSize(new java.awt.Dimension(150, 96));
        untangleJButton.setMinimumSize(new java.awt.Dimension(150, 96));
        untangleJButton.setPreferredSize(new java.awt.Dimension(150, 96));
        try {
            URL cb = Util.getServerCodeBase();
            String proto = cb.getProtocol();
            String host = cb.getHost();
            int port = cb.getPort();
            URL url = new URL(proto + "://" + host + ":" + port + "/images/BrandingLogo.gif");
            untangleJButton.setIcon(new ImageIcon(url));
        } catch (MalformedURLException exn) {
            untangleJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/main/Logo150x96.png")));
        }
        untangleJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    untangleJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(40, 0, 40, 0);
        controlsJPanel.add(untangleJButton, gridBagConstraints);

        mTabbedPane.setDoubleBuffered(true);
        mTabbedPane.setFocusable(false);
        mTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        mTabbedPane.setMinimumSize(new java.awt.Dimension(177, 177));
        mTabbedPane.setPreferredSize(new java.awt.Dimension(200, 160));
        storeJPanel.setLayout(new java.awt.GridBagLayout());

        storeJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, " Click to Learn More", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 0, 11)));
        storeJPanel.setFocusable(false);
        storeJPanel.setFont(new java.awt.Font("Arial", 0, 11));
        storeJPanel.setMaximumSize(new java.awt.Dimension(189, 32767));
        storeJPanel.setMinimumSize(new java.awt.Dimension(189, 134));
        storeJScrollPane.setBorder(null);
        storeJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        storeJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        storeJScrollPane.setDoubleBuffered(true);
        storeJScrollPane.setFocusable(false);
        storeJScrollPane.setFont(new java.awt.Font("Arial", 0, 12));
        storeJScrollPane.getViewport().setOpaque(false);
        storeScrollJPanel.setLayout(new java.awt.GridBagLayout());

        storeScrollJPanel.setFocusable(false);
        storeSpacerJPanel.setFocusable(false);
        storeSpacerJPanel.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        storeScrollJPanel.add(storeSpacerJPanel, gridBagConstraints);

        storeJScrollPane.setViewportView(storeScrollJPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        storeJPanel.add(storeJScrollPane, gridBagConstraints);

        mTabbedPane.addTab("<html><b>Library</b></html>", storeJPanel);

        toolboxJPanel.setLayout(new java.awt.GridBagLayout());

        toolboxJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, " Click to Install into Rack", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 0, 11)));
        toolboxJPanel.setFocusable(false);
        toolboxJPanel.setMaximumSize(new java.awt.Dimension(189, 32767));
        toolboxJPanel.setMinimumSize(new java.awt.Dimension(189, 134));
        toolboxJScrollPane.setBorder(null);
        toolboxJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        toolboxJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        toolboxJScrollPane.setDoubleBuffered(true);
        toolboxJScrollPane.setFocusable(false);
        toolboxJScrollPane.setOpaque(false);
        toolboxJScrollPane.getViewport().setOpaque(false);
        toolboxScrollJPanel.setLayout(new java.awt.GridBagLayout());

        toolboxScrollJPanel.setFocusable(false);
        toolboxScrollJPanel.setOpaque(false);
        policyToolboxJPanel.setLayout(new javax.swing.BoxLayout(policyToolboxJPanel, javax.swing.BoxLayout.Y_AXIS));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        toolboxScrollJPanel.add(policyToolboxJPanel, gridBagConstraints);

        coreToolboxJPanel.setLayout(new java.awt.GridLayout(1, 1));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        toolboxScrollJPanel.add(coreToolboxJPanel, gridBagConstraints);

        utilToolboxJPanel.setLayout(new java.awt.GridLayout(1, 1));

        utilToolboxJPanel.setFocusable(false);
        utilToolboxJPanel.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        toolboxScrollJPanel.add(utilToolboxJPanel, gridBagConstraints);

        toolboxSpacerJPanel.setFocusable(false);
        toolboxSpacerJPanel.setMinimumSize(new java.awt.Dimension(0, 0));
        toolboxSpacerJPanel.setOpaque(false);
        toolboxSpacerJPanel.setPreferredSize(new java.awt.Dimension(0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        toolboxScrollJPanel.add(toolboxSpacerJPanel, gridBagConstraints);

        toolboxJScrollPane.setViewportView(toolboxScrollJPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        toolboxJPanel.add(toolboxJScrollPane, gridBagConstraints);

        mTabbedPane.addTab("My Apps", toolboxJPanel);

        configurationJPanel.setLayout(new java.awt.GridBagLayout());

        configurationJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, " Click to Configure", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 0, 11)));
        configurationJPanel.setFocusable(false);
        configurationJPanel.setMaximumSize(new java.awt.Dimension(189, 134));
        configurationJPanel.setMinimumSize(new java.awt.Dimension(189, 134));
        configurationJPanel.setPreferredSize(new java.awt.Dimension(189, 134));
        configurationJScrollPane.setBorder(null);
        configurationJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        configurationJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        configurationJScrollPane.setDoubleBuffered(true);
        configurationJScrollPane.setFocusable(false);
        jPanel8.setLayout(new java.awt.GridBagLayout());

        jPanel8.setFocusable(false);
        networkJButton.setFont(new java.awt.Font("Arial", 0, 12));
        networkJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/main/IconConfigNetwork36x36.png")));
        networkJButton.setText("<html>Networking</html>");
        networkJButton.setDoubleBuffered(true);
        networkJButton.setFocusPainted(false);
        networkJButton.setFocusable(false);
        networkJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        networkJButton.setMargin(new java.awt.Insets(3, 3, 3, 2));
        networkJButton.setMaximumSize(new java.awt.Dimension(810, 370));
        networkJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    networkJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 1, 3, 3);
        jPanel8.add(networkJButton, gridBagConstraints);

        remoteJButton.setFont(new java.awt.Font("Arial", 0, 12));
        remoteJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/main/IconConfigAdmin36x36.png")));
        remoteJButton.setText("<html>Remote Admin</html>");
        remoteJButton.setDoubleBuffered(true);
        remoteJButton.setFocusPainted(false);
        remoteJButton.setFocusable(false);
        remoteJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        remoteJButton.setMargin(new java.awt.Insets(3, 3, 3, 2));
        remoteJButton.setMaximumSize(new java.awt.Dimension(810, 370));
        remoteJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    remoteJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 1, 3, 3);
        jPanel8.add(remoteJButton, gridBagConstraints);

        emailJButton.setFont(new java.awt.Font("Arial", 0, 12));
        emailJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/main/IconConfigEmail36x36.png")));
        emailJButton.setText("<html>Email</html>");
        emailJButton.setDoubleBuffered(true);
        emailJButton.setFocusPainted(false);
        emailJButton.setFocusable(false);
        emailJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        emailJButton.setMargin(new java.awt.Insets(3, 3, 3, 2));
        emailJButton.setMaximumSize(new java.awt.Dimension(810, 370));
        emailJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    emailJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 1, 3, 3);
        jPanel8.add(emailJButton, gridBagConstraints);

        directoryJButton.setFont(new java.awt.Font("Arial", 0, 12));
        directoryJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/main/IconConfigDirectory36x36.png")));
        directoryJButton.setText("<html>User Directory</html>");
        directoryJButton.setDoubleBuffered(true);
        directoryJButton.setFocusPainted(false);
        directoryJButton.setFocusable(false);
        directoryJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        directoryJButton.setMargin(new java.awt.Insets(3, 3, 3, 2));
        directoryJButton.setMaximumSize(new java.awt.Dimension(810, 370));
        directoryJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    directoryJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 1, 3, 3);
        jPanel8.add(directoryJButton, gridBagConstraints);

        backupJButton.setFont(new java.awt.Font("Arial", 0, 12));
        backupJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/main/IconConfigBackup36x36.png")));
        backupJButton.setText("<html>Backup/Restore</html>");
        backupJButton.setDoubleBuffered(true);
        backupJButton.setFocusPainted(false);
        backupJButton.setFocusable(false);
        backupJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        backupJButton.setMargin(new java.awt.Insets(3, 3, 3, 2));
        backupJButton.setMaximumSize(new java.awt.Dimension(810, 370));
        backupJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    backupJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 1, 3, 3);
        jPanel8.add(backupJButton, gridBagConstraints);

        maintenanceJButton.setFont(new java.awt.Font("Arial", 0, 12));
        maintenanceJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/main/IconConfigSupport36x36.png")));
        maintenanceJButton.setText("<html>Support</html>");
        maintenanceJButton.setDoubleBuffered(true);
        maintenanceJButton.setFocusPainted(false);
        maintenanceJButton.setFocusable(false);
        maintenanceJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        maintenanceJButton.setMargin(new java.awt.Insets(3, 3, 3, 2));
        maintenanceJButton.setMaximumSize(new java.awt.Dimension(810, 370));
        maintenanceJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    maintenanceJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 1, 3, 3);
        jPanel8.add(maintenanceJButton, gridBagConstraints);

        upgradeJButton.setFont(new java.awt.Font("Default", 0, 12));
        upgradeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/main/IconConfigUpgrade36x36.png")));
        upgradeJButton.setText("<html>Upgrade</html>");
        upgradeJButton.setDoubleBuffered(true);
        upgradeJButton.setFocusPainted(false);
        upgradeJButton.setFocusable(false);
        upgradeJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        upgradeJButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        upgradeJButton.setMargin(new java.awt.Insets(3, 3, 3, 2));
        upgradeJButton.setMaximumSize(new java.awt.Dimension(114, 42));
        upgradeJButton.setMinimumSize(new java.awt.Dimension(96, 48));
        upgradeJButton.setOpaque(false);
        upgradeJButton.setPreferredSize(new java.awt.Dimension(96, 48));
        upgradeJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    upgradeJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 1, 3, 3);
        jPanel8.add(upgradeJButton, gridBagConstraints);

        aboutJButton.setFont(new java.awt.Font("Arial", 0, 12));
        aboutJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/main/IconConfigSetup36x36.png")));
        aboutJButton.setText("<html>Setup Info</html>");
        aboutJButton.setDoubleBuffered(true);
        aboutJButton.setFocusPainted(false);
        aboutJButton.setFocusable(false);
        aboutJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        aboutJButton.setMargin(new java.awt.Insets(3, 3, 3, 2));
        aboutJButton.setMaximumSize(new java.awt.Dimension(810, 370));
        aboutJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    aboutJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 1, 3, 3);
        jPanel8.add(aboutJButton, gridBagConstraints);

        configurationSpacerJPanel1.setFocusable(false);
        configurationSpacerJPanel1.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        jPanel8.add(configurationSpacerJPanel1, gridBagConstraints);

        configurationJScrollPane.setViewportView(jPanel8);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        configurationJPanel.add(configurationJScrollPane, gridBagConstraints);

        mTabbedPane.addTab("Config", configurationJPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        controlsJPanel.add(mTabbedPane, gridBagConstraints);

        helpJButton.setFont(new java.awt.Font("Default", 0, 12));
        helpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/main/IconHelp36x36.png")));
        helpJButton.setText("Help");
        helpJButton.setDoubleBuffered(true);
        helpJButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        helpJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        helpJButton.setOpaque(false);
        helpJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    helpJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        controlsJPanel.add(helpJButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(controlsJPanel, gridBagConstraints);

        mPipelineJPanel.setOpaque(false);
        //((com.untangle.gui.pipeline.MPipelineJPanel)mPipelineJPanel).setMFilterJPanel((com.untangle.gui.filter.MFilterJPanel)mFilterJPanel);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(mPipelineJPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(backgroundJPanel, gridBagConstraints);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-1024)/2, (screenSize.height-768)/2, 1024, 768);
    }// </editor-fold>//GEN-END:initComponents

    private void directoryJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_directoryJButtonActionPerformed
        try{
            directoryJButton.setEnabled(false);
            DirectoryJDialog directoryJDialog = new DirectoryJDialog(this);
            directoryJDialog.setVisible(true);
        }
        catch(Exception e){
            try{ Util.handleExceptionWithRestart("Error showing directory", e); }
            catch(Exception f){ Util.handleExceptionNoRestart("Error showing directory", f); }
        }
        finally{
            directoryJButton.setEnabled(true);
        }
    }//GEN-LAST:event_directoryJButtonActionPerformed

    private void helpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpJButtonActionPerformed
        try{
            String focus = mTabbedPane.getTitleAt(mTabbedPane.getSelectedIndex()).toLowerCase().replace(" ", "_");
            focus = focus.substring(9,focus.length()-11);
            URL newURL = new URL( "http://www.untangle.com/docs/get.php?"
                                  + "version=" + Version.getVersion()
                                  + "&source=rack"
                                  + "&focus=" + focus);
            ((BasicService) ServiceManager.lookup("javax.jnlp.BasicService")).showDocument(newURL);
        }
        catch(Exception f){
            Util.handleExceptionNoRestart("Error showing help for rack.", f);
        }
    }//GEN-LAST:event_helpJButtonActionPerformed

    private void emailJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_emailJButtonActionPerformed
        try{
            emailJButton.setEnabled(false);
            EmailJDialog emailJDialog = new EmailJDialog(this);
            emailJDialog.setVisible(true);
        }
        catch(Exception e){
            try{ Util.handleExceptionWithRestart("Error showing email", e); }
            catch(Exception f){ Util.handleExceptionNoRestart("Error showing email", f); }
        }
        finally{
            emailJButton.setEnabled(true);
        }
    }//GEN-LAST:event_emailJButtonActionPerformed

    private void untangleJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_untangleJButtonActionPerformed
    }//GEN-LAST:event_untangleJButtonActionPerformed



    private void maintenanceJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_maintenanceJButtonActionPerformed
        try{
            maintenanceJButton.setEnabled(false);
            MaintenanceJDialog maintenanceJDialog = new MaintenanceJDialog(this);
            maintenanceJDialog.setVisible(true);
        }
        catch(Exception e){
            try{ Util.handleExceptionWithRestart("Error showing remote maintenance settings", e); }
            catch(Exception f){ Util.handleExceptionNoRestart("Error showing remote maintenance settings", f); }
        }
        finally{
            maintenanceJButton.setEnabled(true);
        }
    }//GEN-LAST:event_maintenanceJButtonActionPerformed

    private void remoteJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remoteJButtonActionPerformed
        try{
            remoteJButton.setEnabled(false);
            RemoteJDialog remoteJDialog = new RemoteJDialog(this);
            remoteJDialog.setVisible(true);
        }
        catch(Exception e){
            try{ Util.handleExceptionWithRestart("Error showing remote administration settings", e); }
            catch(Exception f){ Util.handleExceptionNoRestart("Error showing remote administration settings", f); }
        }
        finally{
            remoteJButton.setEnabled(true);
        }
    }//GEN-LAST:event_remoteJButtonActionPerformed

    private void backupJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupJButtonActionPerformed
        try{
            backupJButton.setEnabled(false);
            BackupJDialog backupRestoreJDialog = new BackupJDialog(this);
            backupRestoreJDialog.setVisible(true);
        }
        catch(Exception e){
            try{ Util.handleExceptionWithRestart("Error showing backup and restore panel", e);}
            catch(Exception f){Util.handleExceptionNoRestart("Error showing backup and restore panel", f);}
        }
        finally{
            backupJButton.setEnabled(true);
        }
    }//GEN-LAST:event_backupJButtonActionPerformed

    private void networkJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_networkJButtonActionPerformed
        try{
            URL scb = Util.getServerCodeBase();
            String q = "?" + Util.getRemoteAdminManager().generateAuthNonce();
            String alpacaNonce = Util.getRemoteAdminManager().getAlpacaNonce();
            q += null == alpacaNonce ? "" : "&argyle=" + alpacaNonce;
            URL url = new URL("http://" + scb.getHost() + "/alpaca/" + q);
            ((BasicService) ServiceManager.lookup("javax.jnlp.BasicService")).showDocument(url);
        }
        catch(Exception e){
            try{ Util.handleExceptionWithRestart("Error showing network settings", e); }
            catch(Exception f){ Util.handleExceptionNoRestart("Error showing network settings", f); }
        }
        finally{
            networkJButton.setEnabled(true);
        }
    }//GEN-LAST:event_networkJButtonActionPerformed


    private void aboutJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutJButtonActionPerformed
        try{
            aboutJButton.setEnabled(false);
            int modifiers = evt.getModifiers();
            boolean showBrandingPanel = ((modifiers & evt.SHIFT_MASK) > 0) && ((modifiers & evt.CTRL_MASK) > 0);
            AboutJDialog.setShowBrandingPanel( showBrandingPanel );
            AboutJDialog aboutJDialog = new AboutJDialog(this);
            aboutJDialog.setVisible(true);
        }
        catch(Exception e){
            try{ Util.handleExceptionWithRestart("Error showing about", e); }
            catch(Exception f){ Util.handleExceptionNoRestart("Error showing about", f); }
        }
        finally{
            aboutJButton.setEnabled(true);
        }
    }//GEN-LAST:event_aboutJButtonActionPerformed

    public void setVisible(boolean isVisible){
        super.setVisible(isVisible);
        Util.resizeCheck(this, MIN_SIZE, MAX_SIZE);
    }

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        Util.resizeCheck(this, MIN_SIZE, MAX_SIZE);
    }//GEN-LAST:event_formComponentResized

    private void upgradeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upgradeJButtonActionPerformed
        try{
            upgradeJButton.setEnabled(false);
            UpgradeJDialog upgradeJDialog =  new UpgradeJDialog(this);
            upgradeJDialog.setVisible(true);
        }
        catch(Exception e){
            try{ Util.handleExceptionWithRestart("Error checking for upgrades on server", e); }
            catch(Exception f){ Util.handleExceptionNoRestart("Error checking for upgrades on server", f); }
        }
        finally{
            upgradeJButton.setEnabled(true);
        }
    }//GEN-LAST:event_upgradeJButtonActionPerformed

    private void exitForm(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_exitForm
        Util.exit(0);
    }//GEN-LAST:event_exitForm



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton aboutJButton;
    private javax.swing.JPanel backgroundJPanel;
    private javax.swing.JButton backupJButton;
    private javax.swing.JPanel configurationJPanel;
    private javax.swing.JScrollPane configurationJScrollPane;
    private javax.swing.JPanel configurationSpacerJPanel1;
    private javax.swing.JPanel controlsJPanel;
    private javax.swing.JPanel coreToolboxJPanel;
    private javax.swing.JButton directoryJButton;
    private javax.swing.JButton emailJButton;
    private javax.swing.JButton helpJButton;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel mPipelineJPanel;
    private javax.swing.JTabbedPane mTabbedPane;
    private javax.swing.JButton maintenanceJButton;
    private javax.swing.JButton networkJButton;
    private javax.swing.JPanel policyToolboxJPanel;
    private javax.swing.JButton remoteJButton;
    private javax.swing.JPanel storeJPanel;
    private javax.swing.JScrollPane storeJScrollPane;
    private javax.swing.JPanel storeScrollJPanel;
    private javax.swing.JPanel storeSpacerJPanel;
    private javax.swing.JPanel toolboxJPanel;
    private javax.swing.JScrollPane toolboxJScrollPane;
    private javax.swing.JPanel toolboxScrollJPanel;
    private javax.swing.JPanel toolboxSpacerJPanel;
    private javax.swing.JButton untangleJButton;
    private javax.swing.JButton upgradeJButton;
    private javax.swing.JPanel utilToolboxJPanel;
    // End of variables declaration//GEN-END:variables



    private class UpdateCheckThread extends Thread implements Shutdownable {
        private volatile boolean stop = false;
        public UpdateCheckThread(){
            super("MVCLIENT-UpdateCheckThread");
            this.setDaemon(true);
            this.setContextClassLoader(Util.getClassLoader());
            this.start();
        }
        public void doShutdown(){
            if(!stop){
                stop = true;
                interrupt();
            }
        }
        public void run() {
            MackageDesc[] mackageDescs;

            // FORCE THE SERVER TO UPDATE ONCE
            try{
                Util.getRemoteToolboxManager().update();
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error updating upgrades on server", e);
            }

            while(!stop){
                try{
                    // CHECK FOR UPGRADES
                    mackageDescs = Util.getRemoteToolboxManager().upgradable();
                    if( Util.isArrayEmpty(mackageDescs) ){
                        Util.setUpgradeCount(0);
                    }
                    else{
                        Util.setUpgradeCount(mackageDescs.length);
                    }
                    Thread.sleep(Util.UPGRADE_THREAD_SLEEP_MILLIS);
                }
                catch(InterruptedException e){ continue; }
                catch(Exception e){
                    if( !isInterrupted() ){
                        Util.handleExceptionNoRestart("Error checking for upgrades on server", e);
                        try{ Thread.currentThread().sleep(10000); }  catch(Exception f){}
                    }
                }
            }
            Util.printMessage("UpdateCheckThread Stopped");
        }
    }


}




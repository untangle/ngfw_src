/*
 * UpgradeJDialog.java
 *
 * Created on July 16, 2004, 6:04 AM
 */

package com.metavize.gui.widgets.configWindow;

import javax.swing.*;
import javax.swing.text.*;

import java.text.*;
import java.util.*;


import javax.swing.table.*;
import java.awt.*;
import javax.swing.event.*;

import com.metavize.gui.util.*;
import com.metavize.gui.main.MMainJFrame;
import com.metavize.gui.widgets.coloredTable.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.security.PasswordUtil;
import com.metavize.mvvm.*;


/**
 *
 * @author  inieves
 */
public class ConfigJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener, MTableChangeListener {

    protected Dimension MIN_SIZE = new Dimension(640, 480);
    protected Dimension MAX_SIZE = new Dimension(1600, 1200);

    private MColoredJTable mColoredJTable;
    

    /** Creates new form UpgradeJDialog */
    public ConfigJDialog(java.awt.Frame parent) {
        super(parent, true);

        this.initComponents();
        this.addWindowListener(this);   

        this.addComponentListener( 
				  new java.awt.event.ComponentAdapter() {
				      public void componentResized(java.awt.event.ComponentEvent evt) {
					  dialogResized();
				      }
				  } );
	
    }

    private void dialogResized(){
        Util.resizeCheck(this, MIN_SIZE, MAX_SIZE);
    }

    public void removeActionButtons(){
	reloadJButton.setVisible(false);
	saveJButton.setVisible(false);
    }

    public void setTable(MColoredJTable mColoredJTable){
        this.mColoredJTable = mColoredJTable;
    }

    public void damageControl(Object reference){
        contentJTabbedPane.setEnabled(false);
    }
    public void dataChangedInvalid(Object reference){}
    public void dataChangedValid(Object reference){}
    public void dataRefreshed(Object reference){}
    
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        contentJTabbedPane = new javax.swing.JTabbedPane();
        contentJPanel = new javax.swing.JPanel();
        closeJButton = new javax.swing.JButton();
        reloadJButton = new javax.swing.JButton();
        saveJButton = new javax.swing.JButton();
        backgroundJLabel = new com.metavize.gui.widgets.MTiledIconLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("");
        setModal(true);
        contentJTabbedPane.setDoubleBuffered(true);
        contentJTabbedPane.setFocusable(false);
        contentJTabbedPane.setFont(new java.awt.Font("Default", 0, 12));
        contentJPanel.setLayout(new java.awt.BorderLayout());

        contentJPanel.setFocusable(false);
        contentJTabbedPane.addTab("Control Panel", contentJPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
        getContentPane().add(contentJTabbedPane, gridBagConstraints);

        closeJButton.setFont(new java.awt.Font("Default", 0, 12));
        closeJButton.setText("<html><b>Close</b> Window</html>");
        closeJButton.setDoubleBuffered(true);
        closeJButton.setFocusPainted(false);
        closeJButton.setFocusable(false);
        closeJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 50;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 15, 0);
        getContentPane().add(closeJButton, gridBagConstraints);

        reloadJButton.setFont(new java.awt.Font("Arial", 0, 12));
        reloadJButton.setText("<html><b>Reload</b> Settings</html>");
        reloadJButton.setDoubleBuffered(true);
        reloadJButton.setFocusPainted(false);
        reloadJButton.setMaximumSize(new java.awt.Dimension(2147483647, 900));
        reloadJButton.setPreferredSize(null);
        reloadJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reloadJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 50;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 15);
        getContentPane().add(reloadJButton, gridBagConstraints);

        saveJButton.setFont(new java.awt.Font("Arial", 0, 12));
        saveJButton.setText("<html><b>Save</b> Settings</html>");
        saveJButton.setDoubleBuffered(true);
        saveJButton.setFocusPainted(false);
        saveJButton.setMaximumSize(new java.awt.Dimension(2147483647, 900));
        saveJButton.setPreferredSize(null);
        saveJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 40;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 15);
        getContentPane().add(saveJButton, gridBagConstraints);

        backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/images/DarkGreyBackground1600x100.png")));
        backgroundJLabel.setDoubleBuffered(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(backgroundJLabel, gridBagConstraints);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-640)/2, (screenSize.height-480)/2, 640, 480);
    }//GEN-END:initComponents

    private void saveJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveJButtonActionPerformed
        doSaveJButtonActionPerformed(evt);
    }//GEN-LAST:event_saveJButtonActionPerformed

    protected void doSaveJButtonActionPerformed(java.awt.event.ActionEvent evt){
	mColoredJTable.getCellEditor().stopCellEditing();
        ((MSortedTableModel)mColoredJTable.getModel()).commit();
    }

    private void reloadJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadJButtonActionPerformed
        doReloadJButtonActionPerformed(evt);
    }//GEN-LAST:event_reloadJButtonActionPerformed

    protected void doReloadJButtonActionPerformed(java.awt.event.ActionEvent evt){
	mColoredJTable.getCellEditor().stopCellEditing();
        ((MSortedTableModel)mColoredJTable.getModel()).refresh();    
    }


    private void closeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeJButtonActionPerformed
        windowClosing(null);
    }//GEN-LAST:event_closeJButtonActionPerformed

    

    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        //this.setVisible(false);
        this.dispose();
    }

    public void windowClosed(java.awt.event.WindowEvent windowEvent) {}
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {}



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel backgroundJLabel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton closeJButton;
    protected javax.swing.JPanel contentJPanel;
    protected javax.swing.JTabbedPane contentJTabbedPane;
    protected javax.swing.JButton reloadJButton;
    protected javax.swing.JButton saveJButton;
    // End of variables declaration//GEN-END:variables

}








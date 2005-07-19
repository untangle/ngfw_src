/*
 * MCasingJPanel.java
 *
 * Created on February 22, 2005, 1:10 PM
 */

package com.metavize.tran.spyware.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;
import com.metavize.tran.spyware.*;

import java.awt.*;

/**
 *
 * @author  inieves
 */
public class UrlConfigJPanel extends javax.swing.JPanel implements Savable, Refreshable {

    
    public UrlConfigJPanel() {
        initComponents();
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        // URL ENABLED ///////////
        boolean isUrlEnabled = urlEnabledRadioButton.isSelected();
        
	// SAVE SETTINGS ////////////
	if( !validateOnly ){
            SpywareSettings spywareSettings = (SpywareSettings) settings;
            spywareSettings.setUrlBlacklistEnabled(isUrlEnabled);
        }

    }

    public void doRefresh(Object settings){
        
        // URL ENABLED /////////
        SpywareSettings spywareSettings = (SpywareSettings) settings;
        boolean isUrlEnabled = spywareSettings.getUrlBlacklistEnabled();
        if( isUrlEnabled )
            urlEnabledRadioButton.setSelected(true);
        else
            urlDisabledRadioButton.setSelected(true); 
    }
    
    
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        ftpButtonGroup = new javax.swing.ButtonGroup();
        contentJPanel = new javax.swing.JPanel();
        urlEnabledRadioButton = new javax.swing.JRadioButton();
        urlDisabledRadioButton = new javax.swing.JRadioButton();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(563, 120));
        setMinimumSize(new java.awt.Dimension(563, 120));
        setPreferredSize(new java.awt.Dimension(563, 120));
        contentJPanel.setLayout(new java.awt.GridBagLayout());

        contentJPanel.setBorder(new javax.swing.border.EtchedBorder());
        ftpButtonGroup.add(urlEnabledRadioButton);
        urlEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        urlEnabledRadioButton.setText("<html><b>Enable</b> spyware URL blocking</html>");
        urlEnabledRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 10);
        contentJPanel.add(urlEnabledRadioButton, gridBagConstraints);

        ftpButtonGroup.add(urlDisabledRadioButton);
        urlDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        urlDisabledRadioButton.setText("<html><b>Disable</b> spyware URL blocking</html>");
        urlDisabledRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        contentJPanel.add(urlDisabledRadioButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(contentJPanel, gridBagConstraints);

    }//GEN-END:initComponents
    

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel contentJPanel;
    private javax.swing.ButtonGroup ftpButtonGroup;
    public javax.swing.JRadioButton urlDisabledRadioButton;
    public javax.swing.JRadioButton urlEnabledRadioButton;
    // End of variables declaration//GEN-END:variables
    

}

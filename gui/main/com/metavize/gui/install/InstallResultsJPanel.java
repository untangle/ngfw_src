/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.install;

import com.metavize.gui.widgets.wizard.*;
import com.metavize.gui.util.*;
import javax.swing.SwingUtilities;

public class InstallResultsJPanel extends MWizardPageJPanel {

    
    public InstallResultsJPanel() {
        initComponents();
    }

    public boolean enteringForwards(){
	try{
	    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
		if(InstallWizard.testsPassed()){
		    headerJLabel.setText("<html>Congratulations!<br>Your hardware meets requirements.</html>");
		    subheaderJLabel.setText("<html>You may continue installation...</html>");
		    messageJLabel.setText("<html>Press the Finish button to continue.</html>");
		}
		else{
		    headerJLabel.setText("<html><font color=\"#FF0000\">Warning!<br>Your hardware does not meet requirements.</font></html>");
		    subheaderJLabel.setText("<html>You may not continue installation...</html>");
		    messageJLabel.setText("<html>Press the Finish button to exit.</html>");				
		}
	    }});
	}
	catch(Exception e){
	    Util.handleExceptionNoRestart("Error waiting for update", e);
	}
	return true;
    }

        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                contentJPanel = new javax.swing.JPanel();
                headerJLabel = new javax.swing.JLabel();
                subheaderJLabel = new javax.swing.JLabel();
                messageJLabel = new javax.swing.JLabel();
                backgroundJPabel = new javax.swing.JLabel();

                setLayout(new java.awt.GridBagLayout());

                setOpaque(false);
                contentJPanel.setLayout(new java.awt.GridBagLayout());

                contentJPanel.setOpaque(false);
                headerJLabel.setFont(new java.awt.Font("Dialog", 1, 18));
                headerJLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                headerJLabel.setText("<html>Congratulations!<br>SOME RESULT MESSAGE GOES HERE</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                contentJPanel.add(headerJLabel, gridBagConstraints);

                subheaderJLabel.setFont(new java.awt.Font("Dialog", 1, 18));
                subheaderJLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                subheaderJLabel.setText("<html>some result submessage goes here</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                contentJPanel.add(subheaderJLabel, gridBagConstraints);

                messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                messageJLabel.setText("<html>some sub sub message goes here</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                contentJPanel.add(messageJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                add(contentJPanel, gridBagConstraints);

                backgroundJPabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/install/ProductShot.png")));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
                gridBagConstraints.weightx = 1.0;
                add(backgroundJPabel, gridBagConstraints);

        }//GEN-END:initComponents
    
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel backgroundJPabel;
        private javax.swing.JPanel contentJPanel;
        private javax.swing.JLabel headerJLabel;
        private javax.swing.JLabel messageJLabel;
        private javax.swing.JLabel subheaderJLabel;
        // End of variables declaration//GEN-END:variables
    
}

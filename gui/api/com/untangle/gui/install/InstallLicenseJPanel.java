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

package com.untangle.gui.install;

import java.awt.Window;
import java.awt.event.*;
import java.io.*;
import java.util.Properties;
import javax.swing.*;

import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.wizard.*;

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;

public class InstallLicenseJPanel extends MWizardPageJPanel {

    private static final String LOG4J_DEFAULT_PROPERTIES = "com/untangle/gui/log4j.properties";
    private static final String LOG4J_DEVEL_PROPERTIES   = "com/untangle/gui/log4j-devel.properties";

    private static final Logger logger = Logger.getLogger(InstallLicenseJPanel.class);

    public InstallLicenseJPanel() {
	    // CONFIGURE LOGGING
    	configureLogging();

        initComponents();


        try{
            InputStream licenseInputStream = getClass().getClassLoader().getResourceAsStream("LicenseStandard.txt");
            InputStreamReader licenseInputStreamReader = new InputStreamReader(licenseInputStream);
            BufferedReader licenseBufferedReader = new BufferedReader(licenseInputStreamReader);
            StringBuilder licenseStringBuilder = new StringBuilder();
            String licenseLine;
            while( true ){
                licenseLine=licenseBufferedReader.readLine();
                if(licenseLine==null)
                    break;
                else
                    licenseStringBuilder.append(licenseLine).append("\n");
            }

            contentJEditorPane.setContentType("text/plain");
            contentJEditorPane.setText(licenseStringBuilder.toString());
            contentJEditorPane.setFont(new java.awt.Font("Courier", 0, 11));
        }
        catch(Exception e){
            Util.handleExceptionNoRestart("error loading license", e);
        }
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {
    }


    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        acceptButtonGroup = new javax.swing.ButtonGroup();
        contentJPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        contentJScrollPane = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        contentJEditorPane = new javax.swing.JEditorPane();
        backgroundJPabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setOpaque(false);
        contentJPanel.setLayout(new java.awt.GridBagLayout());

        contentJPanel.setOpaque(false);
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>License Agreement</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
        contentJPanel.add(jLabel2, gridBagConstraints);

        contentJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        contentJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        contentJScrollPane.setFocusable(false);
        jPanel1.setLayout(new java.awt.BorderLayout());

        contentJEditorPane.setEditable(false);
        contentJEditorPane.setFocusable(false);
        jPanel1.add(contentJEditorPane, java.awt.BorderLayout.CENTER);

        contentJScrollPane.setViewportView(jPanel1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
        contentJPanel.add(contentJScrollPane, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(contentJPanel, gridBagConstraints);

        backgroundJPabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/login/ProductShot.png")));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 1.0;
        add(backgroundJPabel, gridBagConstraints);

    }// </editor-fold>//GEN-END:initComponents


    static void configureLogging()
    {
        Properties props = new Properties();

        /* defaults in case it can't parse the log4j.properties */
        props.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
        props.setProperty("log4j.appender.A1.layout","org.apache.log4j.PatternLayout");
        props.setProperty("log4j.appender.A1.layout.ConversionPattern",
                          "%d{HH:mm:ss,SSS} (%t) %-5p [%c] - %m%n");
        props.setProperty("log4j.rootLogger=WARN","A1");

        try {
            InputStream is = InstallLicenseJPanel.class.getClassLoader().
                getResourceAsStream(LOG4J_DEFAULT_PROPERTIES);

            if (is != null) {
                props.load(is);
            } else {
                System.err.println("Unable to load default log4j properties");
            }

        } catch ( IOException e ) {
            System.err.println("Unable to load default logging properties.");
            System.err.println("Using defaults." );
        }

        try {
            if ( Util.isDevel()) {
                System.out.println("enabling debug.");

                InputStream is = InstallLicenseJPanel.class.getClassLoader().
                    getResourceAsStream(LOG4J_DEVEL_PROPERTIES);
                if (is != null) {
                    props.load(is);
                } else {
                    System.err.println("Unable to load development log4j properties.");
                }
            }
        } catch ( IOException e ) {
            System.err.println("Unable to load development log4j properties.");
        }

        PropertyConfigurator.configure(props);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup acceptButtonGroup;
    private javax.swing.JLabel backgroundJPabel;
    private javax.swing.JEditorPane contentJEditorPane;
    private javax.swing.JPanel contentJPanel;
    private javax.swing.JScrollPane contentJScrollPane;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables

}

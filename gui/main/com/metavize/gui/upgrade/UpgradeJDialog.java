/*
 * UpgradeJDialog.java
 *
 * Created on July 16, 2004, 6:04 AM
 */

package com.metavize.gui.upgrade;

import java.awt.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;

import com.metavize.gui.main.MMainJFrame;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.coloredTable.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.widgets.restartWindow.*;

import com.metavize.mvvm.*;


/**
 *
 * @author  inieves
 */
public class UpgradeJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener, MTableChangeListener {

    ProceedJDialog proceedJDialog;
    private static final Color TABLE_BACKGROUND_COLOR = new Color(213, 213, 226);
    private Period period;
    private UpgradeSettings upgradeSettings;
    //    private Alerts alerts;
    private MEditTableJPanel mEditTableJPanel;
    private UpgradeTableModel upgradeTableModel;
    private UpdateThread updateThread;
    private MMainJFrame mMainJFrame;

    /** Creates new form UpgradeJDialog */
    public UpgradeJDialog(java.awt.Frame parent) {
        super(parent, true);
        mMainJFrame = (MMainJFrame) parent;




        // BUILD FIRST TAB (MANUAL UPGRADE)
        mEditTableJPanel = new MEditTableJPanel(false, true);
        mEditTableJPanel.setInsets(new Insets(0,0,0,0));
        mEditTableJPanel.setTableTitle("Available Upgrades");
        mEditTableJPanel.setDetailsTitle("Upgrade Details");
        mEditTableJPanel.setAddRemoveEnabled(false);
        upgradeTableModel = new UpgradeTableModel();
        upgradeTableModel.setMTableChangeListener(this);
        mEditTableJPanel.setTableModel( upgradeTableModel );
        mEditTableJPanel.getJTable().setRowHeight(49);


        // BUILD GENERAL GUI
        initComponents();
        this.addWindowListener(this);

        // attempt to download settings
        upgradeSettings = Util.getToolboxManager().getUpgradeSettings();


        // BUILD SECOND TAB (SCHEDULED AUTOMATIC UPGRADE)
        int hour, minute;
    yesAutoJRadioButton.setSelected(upgradeSettings.getAutoUpgrade());
    period = upgradeSettings.getPeriod();

    hour = period.getHour();
    minute = period.getMinute();

        // set time
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        //set days
        sundayJCheckBox.setSelected( period.getSunday() );
        mondayJCheckBox.setSelected( period.getMonday() );
        tuesdayJCheckBox.setSelected( period.getTuesday() );
        wednesdayJCheckBox.setSelected( period.getWednesday() );
        thursdayJCheckBox.setSelected( period.getThursday() );
        fridayJCheckBox.setSelected( period.getFriday() );
        saturdayJCheckBox.setSelected( period.getSaturday() );
        // set time value
        timeJSpinner.setValue(calendar.getTime());


        // BUILD THIRD TAB (EVENT LOG)
    /*
        alerts = upgradeSettings.alerts();
        if(alerts == null){
            alerts = (Alerts)NodeType.type(Alerts.class).instantiate();
            criticalAlertsJCheckBox.setSelected(alerts.generateCriticalAlertsDefault());
            summaryAlertsJCheckBox.setSelected(alerts.generateSummaryAlertsDefault());
        }
        else{
            criticalAlertsJCheckBox.setSelected(alerts.generateCriticalAlerts());
            summaryAlertsJCheckBox.setSelected(alerts.generateSummaryAlerts());
        }
        */


    }

    public void setVisible(boolean isVisible){
        if(isVisible){
            update();
            super.setVisible(true);
        }
        else
            super.setVisible(false);
    }
    public void update(){
        UpdateThread updateThread = new UpdateThread();
        updateThread.start();
    }

    public void dataChangedInvalid(Object reference){}
    public void dataChangedValid(Object reference){}
    public void dataRefreshed(Object reference){}

    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        closeJButton = new javax.swing.JButton();
        jTabbedPane = new javax.swing.JTabbedPane();
        upgradeJPanel = new javax.swing.JPanel();
        contentJPanel = mEditTableJPanel;
        actionJPanel = new javax.swing.JPanel();
        upgradeJButton = new javax.swing.JButton();
        actionJProgressBar = new javax.swing.JProgressBar();
        advancedJPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        detailNameJTextField2 = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        jPanel5 = new javax.swing.JPanel();
        mondayJCheckBox = new javax.swing.JCheckBox();
        tuesdayJCheckBox = new javax.swing.JCheckBox();
        wednesdayJCheckBox = new javax.swing.JCheckBox();
        thursdayJCheckBox = new javax.swing.JCheckBox();
        fridayJCheckBox = new javax.swing.JCheckBox();
        saturdayJCheckBox = new javax.swing.JCheckBox();
        sundayJCheckBox = new javax.swing.JCheckBox();
        // Create a calendar object and initialize to a particular hour if desired
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);

        // Create a date spinner that controls the hours
        SpinnerDateModel dateModel = new SpinnerDateModel(calendar.getTime(), null, null, Calendar.MINUTE );
        timeJSpinner = new JSpinner(dateModel);
        // Get the date formatter
        JFormattedTextField tf = ((JSpinner.DefaultEditor)timeJSpinner.getEditor()).getTextField();
        DefaultFormatterFactory factory = (DefaultFormatterFactory)tf.getFormatterFactory();
        DateFormatter formatter = (DateFormatter)factory.getDefaultFormatter();

        // Or use 24 hour mode
        formatter.setFormat(new SimpleDateFormat("HH:mm " + "(" + "a" + ")"));
        tf.setEditable(false);
        tf.setOpaque(false);
        timeJSpinner.setValue( calendar.getTime() );
        contentJPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel6 = new javax.swing.JPanel();
        yesAutoJRadioButton = new javax.swing.JRadioButton();
        noAutoJRadioButton = new javax.swing.JRadioButton();
        detailJTextArea1 = new javax.swing.JTextArea();
        detailNameJTextField = new javax.swing.JTextField();
        commitJButton = new javax.swing.JButton();
        backgroundJLabel = new com.metavize.gui.widgets.MTiledIconLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Upgrade Controls");
        setModal(true);
        setResizable(false);
        closeJButton.setFont(new java.awt.Font("Default", 0, 12));
        closeJButton.setText("<html><b>Close</b> Window</html>");
        closeJButton.setDoubleBuffered(true);
        closeJButton.setFocusPainted(false);
        closeJButton.setFocusable(false);
        closeJButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        closeJButton.setMinimumSize(new java.awt.Dimension(117, 25));
        closeJButton.setPreferredSize(new java.awt.Dimension(117, 25));
        closeJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 15, 0);
        getContentPane().add(closeJButton, gridBagConstraints);

        jTabbedPane.setDoubleBuffered(true);
        jTabbedPane.setFocusable(false);
        jTabbedPane.setFont(new java.awt.Font("Default", 0, 12));
        upgradeJPanel.setLayout(new java.awt.GridBagLayout());

        upgradeJPanel.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
        upgradeJPanel.add(contentJPanel, gridBagConstraints);

        actionJPanel.setLayout(new java.awt.GridBagLayout());

        upgradeJButton.setText("Upgrade System Now");
        upgradeJButton.setDoubleBuffered(true);
        upgradeJButton.setFocusPainted(false);
        upgradeJButton.setFocusable(false);
        upgradeJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        upgradeJButton.setMargin(new java.awt.Insets(4, 15, 4, 15));
        upgradeJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upgradeJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
        actionJPanel.add(upgradeJButton, gridBagConstraints);

        actionJProgressBar.setFont(new java.awt.Font("Default", 0, 10));
        actionJProgressBar.setForeground(new java.awt.Color(68, 91, 255));
        actionJProgressBar.setDoubleBuffered(true);
        actionJProgressBar.setFocusable(false);
        actionJProgressBar.setMinimumSize(new java.awt.Dimension(10, 15));
        actionJProgressBar.setPreferredSize(new java.awt.Dimension(148, 15));
        actionJProgressBar.setString("");
        actionJProgressBar.setStringPainted(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        actionJPanel.add(actionJProgressBar, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 15, 0);
        upgradeJPanel.add(actionJPanel, gridBagConstraints);

        jTabbedPane.addTab("Manual Upgrade", upgradeJPanel);

        advancedJPanel.setLayout(new java.awt.GridBagLayout());

        advancedJPanel.setFocusable(false);
        jPanel3.setLayout(new java.awt.GridBagLayout());

        jPanel3.setOpaque(false);
        detailNameJTextField2.setEditable(false);
        detailNameJTextField2.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        detailNameJTextField2.setText("Check For Upgrades");
        detailNameJTextField2.setDoubleBuffered(true);
        detailNameJTextField2.setFocusable(false);
        detailNameJTextField2.setMinimumSize(new java.awt.Dimension(4, 17));
        detailNameJTextField2.setPreferredSize(new java.awt.Dimension(119, 17));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel3.add(detailNameJTextField2, gridBagConstraints);

        jScrollPane2.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane2.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        jScrollPane2.setDoubleBuffered(true);
        jPanel5.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel5.setBackground(new java.awt.Color(213, 213, 226));
        jPanel5.setMinimumSize(new java.awt.Dimension(21, 185));
        jPanel5.setPreferredSize(new java.awt.Dimension(110, 250));
        mondayJCheckBox.setFont(new java.awt.Font("Default", 0, 12));
        mondayJCheckBox.setText("Monday");
        mondayJCheckBox.setDoubleBuffered(true);
        mondayJCheckBox.setFocusPainted(false);
        mondayJCheckBox.setFocusable(false);
        mondayJCheckBox.setOpaque(false);
        jPanel5.add(mondayJCheckBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 40, 91, -1));

        tuesdayJCheckBox.setFont(new java.awt.Font("Default", 0, 12));
        tuesdayJCheckBox.setText("Tuesday");
        tuesdayJCheckBox.setDoubleBuffered(true);
        tuesdayJCheckBox.setFocusPainted(false);
        tuesdayJCheckBox.setFocusable(false);
        tuesdayJCheckBox.setOpaque(false);
        jPanel5.add(tuesdayJCheckBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 60, 91, -1));

        wednesdayJCheckBox.setFont(new java.awt.Font("Default", 0, 12));
        wednesdayJCheckBox.setText("Wednesday");
        wednesdayJCheckBox.setDoubleBuffered(true);
        wednesdayJCheckBox.setFocusPainted(false);
        wednesdayJCheckBox.setFocusable(false);
        wednesdayJCheckBox.setOpaque(false);
        jPanel5.add(wednesdayJCheckBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 80, -1, -1));

        thursdayJCheckBox.setFont(new java.awt.Font("Default", 0, 12));
        thursdayJCheckBox.setText("Thursday");
        thursdayJCheckBox.setDoubleBuffered(true);
        thursdayJCheckBox.setFocusPainted(false);
        thursdayJCheckBox.setFocusable(false);
        thursdayJCheckBox.setOpaque(false);
        jPanel5.add(thursdayJCheckBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 100, 91, -1));

        fridayJCheckBox.setFont(new java.awt.Font("Default", 0, 12));
        fridayJCheckBox.setText("Friday");
        fridayJCheckBox.setDoubleBuffered(true);
        fridayJCheckBox.setFocusPainted(false);
        fridayJCheckBox.setFocusable(false);
        fridayJCheckBox.setOpaque(false);
        jPanel5.add(fridayJCheckBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 120, 91, -1));

        saturdayJCheckBox.setFont(new java.awt.Font("Default", 0, 12));
        saturdayJCheckBox.setText("Saturday");
        saturdayJCheckBox.setDoubleBuffered(true);
        saturdayJCheckBox.setFocusPainted(false);
        saturdayJCheckBox.setFocusable(false);
        saturdayJCheckBox.setOpaque(false);
        jPanel5.add(saturdayJCheckBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 140, 91, -1));

        sundayJCheckBox.setFont(new java.awt.Font("Default", 0, 12));
        sundayJCheckBox.setText("Sunday");
        sundayJCheckBox.setDoubleBuffered(true);
        sundayJCheckBox.setFocusPainted(false);
        sundayJCheckBox.setFocusable(false);
        sundayJCheckBox.setOpaque(false);
        jPanel5.add(sundayJCheckBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 160, 91, -1));

        timeJSpinner.setFont(new java.awt.Font("Default", 0, 12));
        timeJSpinner.setDoubleBuffered(true);
        timeJSpinner.setFocusable(false);
        timeJSpinner.setPreferredSize(null);
        timeJSpinner.setOpaque(false);
        jPanel5.add(timeJSpinner, new org.netbeans.lib.awtextra.AbsoluteConstraints(45, 200, 85, 30));

        jScrollPane2.setViewportView(jPanel5);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel3.add(jScrollPane2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 65;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 0);
        advancedJPanel.add(jPanel3, gridBagConstraints);

        contentJPanel2.setLayout(new java.awt.GridBagLayout());

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        jScrollPane1.setDoubleBuffered(true);
        jPanel6.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel6.setBackground(new java.awt.Color(213, 213, 226));
        jPanel6.setMinimumSize(new java.awt.Dimension(21, 185));
        jPanel6.setPreferredSize(new java.awt.Dimension(110, 250));
        buttonGroup1.add(yesAutoJRadioButton);
        yesAutoJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        yesAutoJRadioButton.setText("<html> <b>Automatically install new upgrades</b><br>\nIf new upgrades are found after a \"Scheduled<br>\nAutomatic Upgrade\", those upgrades will be<br>\nautomatically downloaded and installed.<br>\nIn the case of certain critical system upgrades,<br>\nthe system may be automatically restarted, and<br>\nthe user interface may not connect for a short<br>\nperiod of time. </html>");
        yesAutoJRadioButton.setDoubleBuffered(true);
        yesAutoJRadioButton.setFocusPainted(false);
        yesAutoJRadioButton.setOpaque(false);
        yesAutoJRadioButton.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        jPanel6.add(yesAutoJRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 10, -1, -1));

        buttonGroup1.add(noAutoJRadioButton);
        noAutoJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        noAutoJRadioButton.setSelected(true);
        noAutoJRadioButton.setText("<html>\n<b>Do not automatically install new upgrades</b><br>\nIf new upgrades are found after a \"Scheduled<br>\nAutomatic Upgrade\",  those upgrades will NOT<br>\nbe automatically installed.  The system<br>\nadministrator must manually upgrade the system<br>\nthrough the \"Manual Upgrade\" tab of this window.\n</html>");
        noAutoJRadioButton.setDoubleBuffered(true);
        noAutoJRadioButton.setFocusPainted(false);
        noAutoJRadioButton.setOpaque(false);
        noAutoJRadioButton.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        jPanel6.add(noAutoJRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 140, -1, -1));

        jScrollPane1.setViewportView(jPanel6);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        contentJPanel2.add(jScrollPane1, gridBagConstraints);

        detailJTextArea1.setBackground(new java.awt.Color(213, 213, 226));
        detailJTextArea1.setDoubleBuffered(true);
        contentJPanel2.add(detailJTextArea1, new java.awt.GridBagConstraints());

        detailNameJTextField.setEditable(false);
        detailNameJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        detailNameJTextField.setText("Automatic Installation Of Upgrades");
        detailNameJTextField.setDoubleBuffered(true);
        detailNameJTextField.setFocusable(false);
        detailNameJTextField.setMinimumSize(new java.awt.Dimension(4, 17));
        detailNameJTextField.setPreferredSize(new java.awt.Dimension(119, 17));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 1);
        contentJPanel2.add(detailNameJTextField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
        advancedJPanel.add(contentJPanel2, gridBagConstraints);

        commitJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        commitJButton.setText("<html><B>Save Settings</B></html>");
        commitJButton.setDoubleBuffered(true);
        commitJButton.setFocusPainted(false);
        commitJButton.setFocusable(false);
        commitJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        commitJButton.setMargin(new java.awt.Insets(4, 15, 4, 15));
        commitJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                commitJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
        advancedJPanel.add(commitJButton, gridBagConstraints);

        jTabbedPane.addTab("Scheduled Automatic Upgrade", advancedJPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
        getContentPane().add(jTabbedPane, gridBagConstraints);

        backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/images/DarkGreyBackground1600x100.png")));
        backgroundJLabel.setDoubleBuffered(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(backgroundJLabel, gridBagConstraints);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-640)/2, (screenSize.height-480)/2, 640, 480);
    }//GEN-END:initComponents





    private void commitJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_commitJButtonActionPerformed
        if( Util.getIsDemo() )
            return;

        try{
            Calendar tempCalendar = new GregorianCalendar();
            tempCalendar.setTime((Date)timeJSpinner.getValue());
            int hour = tempCalendar.get(Calendar.HOUR_OF_DAY);
            int minute = tempCalendar.get(Calendar.MINUTE);

            int days = 0;

        //alerts.generateCriticalAlerts(criticalAlertsJCheckBox.isSelected());
        //alerts.generateSummaryAlerts(summaryAlertsJCheckBox.isSelected());
        //upgradeSettings.alerts(alerts);

            // commit
            period.setHour(hour);
            period.setMinute(minute);
            period.setSunday( sundayJCheckBox.isSelected() );
        period.setMonday( mondayJCheckBox.isSelected() );
        period.setTuesday( tuesdayJCheckBox.isSelected() );
        period.setWednesday( wednesdayJCheckBox.isSelected() );
        period.setThursday( thursdayJCheckBox.isSelected() );
        period.setFriday( fridayJCheckBox.isSelected() );
        period.setSaturday( saturdayJCheckBox.isSelected() );
            upgradeSettings.setPeriod(period);
            upgradeSettings.setAutoUpgrade(yesAutoJRadioButton.isSelected());
            Util.getToolboxManager().setUpgradeSettings(upgradeSettings);
        }
        catch(Exception e){
            try{
                Util.handleExceptionWithRestart("Error committing upgrade data", e);
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("Error committing upgrade data", f);
            }
        }
    }//GEN-LAST:event_commitJButtonActionPerformed

    private void upgradeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upgradeJButtonActionPerformed
        if( Util.getIsDemo() )
            return;
    (new UpgradeThread()).start();
    }//GEN-LAST:event_upgradeJButtonActionPerformed


    private class UpgradeThread extends Thread {

        public void run() {

            // ASK THE USER IF HE REALLY WANTS TO UPGRADE
            proceedJDialog = new ProceedJDialog();
            boolean isUpgrading = proceedJDialog.isUpgrading();
            proceedJDialog.dispose();
            proceedJDialog = null;
            if(!isUpgrading)
                return;

            // UPGRADE
            try{
                if(Util.getToolboxManager() == null)
                    return;

                // prevent GUI from interacting
                jTabbedPane.setEnabled(false);
                upgradeJButton.setEnabled(false);
                closeJButton.setEnabled(false);

                // tell user whats going on
                mEditTableJPanel.setTableTitle("Available Upgrades (upgrading...)");
                actionJProgressBar.setValue(0);
                actionJProgressBar.setString("upgrading...");
                actionJProgressBar.setIndeterminate(true);
                Thread.sleep(2000);

                // start upgrade... should either throw a timeout exception or return
                Util.getToolboxManager().upgrade();

                // to trigger an automatic restart if upgrade() actually returns
                actionJProgressBar.setIndeterminate(true);
                actionJProgressBar.setValue(100);
                actionJProgressBar.setString("shutting down...");
                new RestartJDialog();
                

            }
            catch(Exception e){
                new RestartJDialog();
            }
        }
    }


    private class UpdateThread extends Thread {
        public void run() {
            try{
                MackageDesc[] upgradable = null;
                // make sure the window is visible
                while(true){
                    if(UpgradeJDialog.this.isShowing())
                        break;
                    else
                        Thread.sleep(100);
                }

                // CHECK FOR UPGRADES, PREVENT GUI FROM INTERACTING
                upgradeJButton.setEnabled(false);
                jTabbedPane.setEnabled(false);
                closeJButton.setEnabled(false);
                mEditTableJPanel.setTableTitle("Available Upgrades (checking...)");
                actionJProgressBar.setValue(0);
                actionJProgressBar.setString("downloading upgrade list from server...");
                actionJProgressBar.setIndeterminate(true);
                if(Util.getToolboxManager() != null){
                    Util.getToolboxManager().update();
                    upgradable = Util.getToolboxManager().upgradable();
                }
                Thread.sleep(1000);

                // REFRESH TABLE
                actionJProgressBar.setIndeterminate(false);
                actionJProgressBar.setValue(50);
                actionJProgressBar.setString("refreshing upgrade table...");
                Thread.sleep(1000);

                // TELL USER ITS ALL OVER
                upgradeTableModel.refresh();
                actionJProgressBar.setValue(100);
                actionJProgressBar.setString("finished refreshing upgrade table.");
                Thread.sleep(1000);

                if( Util.isArrayEmpty(upgradable) ){
                    upgradeJButton.setEnabled(false);
                    mEditTableJPanel.setTableTitle("Available Upgrades (none found)");
                    Util.getMMainJFrame().updateJButton(0);
                    actionJProgressBar.setString("No upgrades found.");
                }
                else{
                    upgradeJButton.setEnabled(true);
                    mEditTableJPanel.setTableTitle("Available Upgrades (" + upgradable.length + " found)");
                    Util.getMMainJFrame().updateJButton(upgradable.length);
                }


                actionJProgressBar.setValue(0);
                actionJProgressBar.setString("");

                mMainJFrame.updateJButton( upgradable.length );
                jTabbedPane.setEnabled(true);
                closeJButton.setEnabled(true);
                actionJProgressBar.setIndeterminate(false);
            }
            catch(InterruptedException e){
                upgradeJButton.setEnabled(true);
                mMainJFrame.updateJButton( 0 );
                jTabbedPane.setEnabled(true);
                closeJButton.setEnabled(true);
            }
            catch(Exception e){
                try{
                    Util.handleExceptionWithRestart("Error refreshing upgrade list", e);
                }
                catch(Exception f){
                    upgradeJButton.setEnabled(false);
                    mMainJFrame.updateJButton( 0 );
                    jTabbedPane.setEnabled(true);
                    closeJButton.setEnabled(true);
                    actionJProgressBar.setIndeterminate(false);
                    actionJProgressBar.setValue(0);
                }
            }
        }
    }

    private void closeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeJButtonActionPerformed
        windowClosing(null);
    }//GEN-LAST:event_closeJButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        // SETUP L&F
        try {
          com.incors.plaf.kunststoff.KunststoffLookAndFeel kunststoffLnF = new com.incors.plaf.kunststoff.KunststoffLookAndFeel();
          kunststoffLnF.setCurrentTheme(new com.incors.plaf.kunststoff.KunststoffTheme());
          UIManager.setLookAndFeel(kunststoffLnF);
          System.out.println(UIManager.getLookAndFeel().getDescription());


        UpgradeJDialog upgradeJDialog = new UpgradeJDialog(new javax.swing.JFrame());
        upgradeJDialog.setVisible(true);
        }
        catch (Exception e) {
           // handle exception or not, whatever you prefer
            e.printStackTrace();
        }
    }



    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        if( (updateThread != null) )
            updateThread.interrupt();
        this.dispose();
    }

    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {}
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowClosed(java.awt.event.WindowEvent windowEvent) {}








    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel actionJPanel;
    protected javax.swing.JProgressBar actionJProgressBar;
    private javax.swing.JPanel advancedJPanel;
    private javax.swing.JLabel backgroundJLabel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton closeJButton;
    protected javax.swing.JButton commitJButton;
    private javax.swing.JPanel contentJPanel;
    private javax.swing.JPanel contentJPanel2;
    protected javax.swing.JTextArea detailJTextArea1;
    protected javax.swing.JTextField detailNameJTextField;
    protected javax.swing.JTextField detailNameJTextField2;
    private javax.swing.JCheckBox fridayJCheckBox;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane;
    private javax.swing.JCheckBox mondayJCheckBox;
    private javax.swing.JRadioButton noAutoJRadioButton;
    private javax.swing.JCheckBox saturdayJCheckBox;
    private javax.swing.JCheckBox sundayJCheckBox;
    private javax.swing.JCheckBox thursdayJCheckBox;
    private javax.swing.JSpinner timeJSpinner;
    private javax.swing.JCheckBox tuesdayJCheckBox;
    protected javax.swing.JButton upgradeJButton;
    private javax.swing.JPanel upgradeJPanel;
    private javax.swing.JCheckBox wednesdayJCheckBox;
    private javax.swing.JRadioButton yesAutoJRadioButton;
    // End of variables declaration//GEN-END:variables

}


class UpgradeTableModel extends MSortedTableModel {

    private ToolboxManager toolboxManager;

    UpgradeTableModel(){
        super(null);
        this.toolboxManager = Util.getToolboxManager();
        refresh();
    }

    public void refresh(){

        try{
            Vector dataVector;
            if(toolboxManager != null)
                dataVector = generateRows( toolboxManager.upgradable() );
            else
                dataVector = generateRows( null );
            super.getDataVector().removeAllElements();
            super.getDataVector().addAll(dataVector);
            super.fireTableDataChanged();
        }
        catch(Exception e){
            try{
                Util.handleExceptionWithRestart("Error refreshing table", e);
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("Error refreshing table", f);
            }
        }
    }

    public TableColumnModel getTableColumnModel(){

    DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min  rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0,  30, false, false, false, false, Integer.class, null, "#");
        addTableColumn( tableColumnModel,  1,  49, false, false, false, false, ImageIcon.class, null, "");
        addTableColumn( tableColumnModel,  2,  49, false, false, false, false, ImageIcon.class, null, "");
        addTableColumn( tableColumnModel,  3, 125, true,  false, false, false, String.class, null, "name");
        addTableColumn( tableColumnModel,  4, 100, false, false, false, false, String.class, null, "new version");
        addTableColumn( tableColumnModel,  5, 200, false, false, false, false, String.class, null, "type");
    addTableColumn( tableColumnModel,  6, 100, false, false, false, false, String.class, null, "size");
        addTableColumn( tableColumnModel,  7, 125, false, false, true,  true,  String.class, null, "description");

        return tableColumnModel;
    }

    public Set generateSettings(Vector dataVector){
        return null;
    }

    public Vector generateRows(Object mackageDescArray){

        MackageDesc[] mackageDesc = (MackageDesc[]) mackageDescArray;
        Vector dataVector = new Vector();

        Vector rowVector;
        for(int i=0; i<mackageDesc.length; i++){
            if( mackageDesc[i].getType() == MackageDesc.CASING_TYPE )
                continue;
        try{
            System.err.println("TRYING TO GENERATE: " + mackageDesc[i].getName() );
        rowVector = new Vector();
        rowVector.add( new Integer(i+1) );

        byte[] orgIcon = mackageDesc[i].getOrgIcon();
        byte[] descIcon = mackageDesc[i].getDescIcon();
        if( orgIcon != null)
            rowVector.add( new ImageIcon(orgIcon) );
        else
            rowVector.add( new ImageIcon(getClass().getResource("/com/metavize/gui/transform/IconOrgUnknown42x42.png"))) ;

        if( descIcon != null)
            rowVector.add( new ImageIcon(descIcon) );
        else
            rowVector.add( new ImageIcon(getClass().getResource("/com/metavize/gui/transform/IconDescUnknown42x42.png"))) ;

        rowVector.add( new String(mackageDesc[i].getDisplayName()) );
        rowVector.add( new String(mackageDesc[i].getAvailableVersion()) );
        if( mackageDesc[i].getType() == 0 )
            rowVector.add( "System Component" );
        else
            rowVector.add( "Software Appliance" );
        rowVector.add( new String(Integer.toString(mackageDesc[i].getSize()/1000) + " kB") );
        rowVector.add( new String(mackageDesc[i].getLongDescription()) );
        dataVector.add(rowVector);
        }
        catch(Exception e){
        e.printStackTrace();
        }
        }
        return dataVector;
    }



    public class TimedSpinnerDateModel extends SpinnerDateModel {
        public final static long DEFAULT_SCROLL_INTERVAL = 25;

        Long lastIncrement, lastDecrement;

        public TimedSpinnerDateModel(Date value, Comparable start, Comparable end, int calendarIncrement) {
            super(value, start, end, calendarIncrement);
            lastIncrement = null;
            lastDecrement = null;
        }


        public Object getNextValue() {
            Object value;
            if((lastIncrement == null) || System.currentTimeMillis() >= lastIncrement.longValue() + DEFAULT_SCROLL_INTERVAL) {
                value = super.getNextValue();
                lastIncrement = new Long(System.currentTimeMillis());
            }
            else {
                value = getDate();
            }
            return value;
        }

        public Object getPreviousValue() {
            Object value;
            if((lastDecrement == null) || System.currentTimeMillis() >= lastDecrement.longValue() + DEFAULT_SCROLL_INTERVAL) {
                value = super.getPreviousValue();
                lastDecrement = new Long(System.currentTimeMillis());
            }
            else {
                value = getDate();
            }
            return value;
        }

    }

}

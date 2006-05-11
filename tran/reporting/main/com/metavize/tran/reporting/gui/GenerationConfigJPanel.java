/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.reporting.gui;

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.*;

import com.metavize.tran.reporting.*;

import java.util.List;
import java.awt.*;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class GenerationConfigJPanel extends javax.swing.JPanel implements Savable<Object>, Refreshable<Object> {

    private static final String DAY_SUNDAY    = "Sunday";
    private static final String DAY_MONDAY    = "Monday";
    private static final String DAY_TUESDAY   = "Tuesday";
    private static final String DAY_WEDNESDAY = "Wednesday";
    private static final String DAY_THURSDAY  = "Thursday";
    private static final String DAY_FRIDAY    = "Friday";
    private static final String DAY_SATURDAY  = "Saturday";

    public GenerationConfigJPanel() {
        initComponents();
	monthlyOnceJComboBox.addItem( DAY_SUNDAY );
	monthlyOnceJComboBox.addItem( DAY_MONDAY );
	monthlyOnceJComboBox.addItem( DAY_TUESDAY );
	monthlyOnceJComboBox.addItem( DAY_WEDNESDAY );
	monthlyOnceJComboBox.addItem( DAY_THURSDAY );
	monthlyOnceJComboBox.addItem( DAY_FRIDAY );
	monthlyOnceJComboBox.addItem( DAY_SATURDAY );
    }

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
	this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////
    
    public void doSave(Object settings, boolean validateOnly) throws Exception {

	// DAILY //
	boolean daily = dailyEverydayJCheckBox.isSelected();
        
        // WEEKLY //
	boolean weeklySunday    = weeklySundayJCheckBox.isSelected();
	boolean weeklyMonday    = weeklyMondayJCheckBox.isSelected();
	boolean weeklyTuesday   = weeklyTuesdayJCheckBox.isSelected();
	boolean weeklyWednesday = weeklyWednesdayJCheckBox.isSelected();
	boolean weeklyThursday  = weeklyThursdayJCheckBox.isSelected();
	boolean weeklyFriday    = weeklyFridayJCheckBox.isSelected();
	boolean weeklySaturday  = weeklySaturdayJCheckBox.isSelected();

	// MONTHLY //
	boolean monthlyFirst    = monthlyFirstJRadioButton.isSelected();
	boolean monthlyEveryday = monthlyEverydayJRadioButton.isSelected();
	boolean monthlyOnce     = monthlyOnceJRadioButton.isSelected();
	String monthlyOnceDay   = (String) monthlyOnceJComboBox.getSelectedItem();

	// OPTIONS //
	boolean includeIncidents = includeIncidentsJCheckBox.isSelected();

        // SAVE THE VALUES ////////////////////////////////////
	if( !validateOnly ){
	    ReportingSettings reportingSettings = (ReportingSettings) settings;
	    Schedule schedule = reportingSettings.getSchedule();

	    schedule.setDaily( daily );
	    List<Integer> dayList = (List<Integer>) schedule.getWeeklySched();
	    dayList.clear();
	    if( weeklySunday )
		dayList.add( Schedule.SUNDAY );
	    if( weeklyMonday )
		dayList.add( Schedule.MONDAY );
	    if( weeklyTuesday )
		dayList.add( Schedule.TUESDAY );
	    if( weeklyWednesday )
		dayList.add( Schedule.WEDNESDAY );
	    if( weeklyThursday )
		dayList.add( Schedule.THURSDAY );
	    if( weeklyFriday )
		dayList.add( Schedule.FRIDAY );
	    if( weeklySaturday )
		dayList.add( Schedule.SATURDAY );
	    schedule.setWeeklySched( dayList );

	    schedule.setMonthlyNFirst( monthlyFirst );
	    schedule.setMonthlyNDaily( monthlyEveryday );
	    if( !monthlyOnce )
		schedule.setMonthlyNDayOfWk( Schedule.NONE );
	    else if( monthlyOnceDay.equals(DAY_SUNDAY) )
		schedule.setMonthlyNDayOfWk( Schedule.SUNDAY );
	    else if( monthlyOnceDay.equals(DAY_MONDAY) )
		schedule.setMonthlyNDayOfWk( Schedule.MONDAY );
	    else if( monthlyOnceDay.equals(DAY_TUESDAY) )
		schedule.setMonthlyNDayOfWk( Schedule.TUESDAY );
	    else if( monthlyOnceDay.equals(DAY_WEDNESDAY) )
		schedule.setMonthlyNDayOfWk( Schedule.WEDNESDAY );
	    else if( monthlyOnceDay.equals(DAY_THURSDAY) )
		schedule.setMonthlyNDayOfWk( Schedule.THURSDAY );
	    else if( monthlyOnceDay.equals(DAY_FRIDAY) )
		schedule.setMonthlyNDayOfWk( Schedule.FRIDAY );
	    else if( monthlyOnceDay.equals(DAY_SATURDAY) )
		schedule.setMonthlyNDayOfWk( Schedule.SATURDAY );
	    
	    reportingSettings.setSchedule( schedule );
	    reportingSettings.setEmailDetail( includeIncidents );
	}        
    }

    boolean dailyCurrent;
    boolean weeklySundayCurrent;
    boolean weeklyMondayCurrent;
    boolean weeklyTuesdayCurrent;
    boolean weeklyWednesdayCurrent;
    boolean weeklyThursdayCurrent;
    boolean weeklyFridayCurrent;
    boolean weeklySaturdayCurrent;
    boolean monthlyNoneCurrent;
    boolean monthlyFirstCurrent;
    boolean monthlyEverydayCurrent;
    boolean monthlyOnceCurrent;
    int monthlyOnceDayCurrent;
    boolean includeIncidentsCurrent;
    
    public void doRefresh(Object settings) {
	ReportingSettings reportingSettings = (ReportingSettings) settings;
	Schedule schedule = reportingSettings.getSchedule();

        // DAILY //
	dailyCurrent = schedule.getDaily();
	dailyEverydayJCheckBox.setSelected( dailyCurrent );
        
        // WEEKLY //
	List<Integer> dayList = (List<Integer>) schedule.getWeeklySched();
	weeklySundayCurrent = dayList.contains( Schedule.SUNDAY );
	weeklyMondayCurrent = dayList.contains( Schedule.MONDAY );
	weeklyTuesdayCurrent = dayList.contains( Schedule.TUESDAY );
	weeklyWednesdayCurrent = dayList.contains( Schedule.WEDNESDAY );
	weeklyThursdayCurrent = dayList.contains( Schedule.THURSDAY );
	weeklyFridayCurrent = dayList.contains( Schedule.FRIDAY );
	weeklySaturdayCurrent = dayList.contains( Schedule.SATURDAY );
	weeklySundayJCheckBox.setSelected( weeklySundayCurrent );
	weeklyMondayJCheckBox.setSelected( weeklyMondayCurrent );
	weeklyTuesdayJCheckBox.setSelected( weeklyTuesdayCurrent );
	weeklyWednesdayJCheckBox.setSelected( weeklyWednesdayCurrent );
	weeklyThursdayJCheckBox.setSelected( weeklyThursdayCurrent );
	weeklyFridayJCheckBox.setSelected( weeklyFridayCurrent );
	weeklySaturdayJCheckBox.setSelected( weeklySaturdayCurrent );

	// MONTHLY //
	monthlyFirstCurrent = schedule.getMonthlyNFirst();
	monthlyEverydayCurrent = schedule.getMonthlyNDaily();
	monthlyOnceCurrent = ( schedule.getMonthlyNDayOfWk() != Schedule.NONE );
	monthlyOnceDayCurrent = schedule.getMonthlyNDayOfWk();
	monthlyNoneCurrent = !( monthlyFirstCurrent || monthlyEverydayCurrent || monthlyOnceCurrent );
	if( monthlyFirstCurrent )
	    monthlyFirstJRadioButton.setSelected(true);
	else if( monthlyEverydayCurrent )
	    monthlyEverydayJRadioButton.setSelected(true);
	else if( monthlyOnceCurrent )
	    monthlyOnceJRadioButton.setSelected(true);
	else
	    monthlyNoneJRadioButton.setSelected(true);
	if( monthlyOnceCurrent ){
	    if( monthlyOnceDayCurrent == Schedule.SUNDAY )
		monthlyOnceJComboBox.setSelectedItem( DAY_SUNDAY );
	    else if( monthlyOnceDayCurrent == Schedule.MONDAY )
		monthlyOnceJComboBox.setSelectedItem( DAY_MONDAY );
	    else if( monthlyOnceDayCurrent == Schedule.TUESDAY )
		monthlyOnceJComboBox.setSelectedItem( DAY_TUESDAY );
	    else if( monthlyOnceDayCurrent == Schedule.WEDNESDAY )
		monthlyOnceJComboBox.setSelectedItem( DAY_WEDNESDAY );
	    else if( monthlyOnceDayCurrent == Schedule.THURSDAY )
		monthlyOnceJComboBox.setSelectedItem( DAY_THURSDAY );
	    else if( monthlyOnceDayCurrent == Schedule.FRIDAY )
		monthlyOnceJComboBox.setSelectedItem( DAY_FRIDAY );
	    else if( monthlyOnceDayCurrent == Schedule.SATURDAY )
		monthlyOnceJComboBox.setSelectedItem( DAY_SATURDAY );
	}
	else{
	    monthlyOnceJComboBox.setSelectedItem( DAY_SUNDAY );
	}

	setMonthlyDependency( monthlyOnceCurrent );

	// OPTIONS //
	includeIncidentsCurrent = reportingSettings.getEmailDetail();
        
    }

        
    

    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                monthlyButtonGroup = new javax.swing.ButtonGroup();
                explanationJPanel1 = new javax.swing.JPanel();
                jPanel1 = new javax.swing.JPanel();
                explorerJLabel1 = new javax.swing.JLabel();
                dailyEverydayJCheckBox = new javax.swing.JCheckBox();
                explanationJPanel = new javax.swing.JPanel();
                jPanel2 = new javax.swing.JPanel();
                weeklySundayJLabel = new javax.swing.JLabel();
                weeklySundayJCheckBox = new javax.swing.JCheckBox();
                weeklyMondayJLabel = new javax.swing.JLabel();
                weeklyMondayJCheckBox = new javax.swing.JCheckBox();
                weeklyTuesdayJLabel = new javax.swing.JLabel();
                weeklyTuesdayJCheckBox = new javax.swing.JCheckBox();
                weeklyWednesdayJLabel = new javax.swing.JLabel();
                weeklyWednesdayJCheckBox = new javax.swing.JCheckBox();
                weeklyThursdayJLabel = new javax.swing.JLabel();
                weeklyThursdayJCheckBox = new javax.swing.JCheckBox();
                weeklyFridayJLabel = new javax.swing.JLabel();
                weeklyFridayJCheckBox = new javax.swing.JCheckBox();
                weeklySaturdayJLabel = new javax.swing.JLabel();
                weeklySaturdayJCheckBox = new javax.swing.JCheckBox();
                externalRemoteJPanel = new javax.swing.JPanel();
                jPanel3 = new javax.swing.JPanel();
                monthlyNoneJRadioButton = new javax.swing.JRadioButton();
                monthlyFirstJRadioButton = new javax.swing.JRadioButton();
                monthlyEverydayJRadioButton = new javax.swing.JRadioButton();
                monthlyOnceJRadioButton = new javax.swing.JRadioButton();
                monthlyOnceJComboBox = new javax.swing.JComboBox();
                externalRemoteJPanel1 = new javax.swing.JPanel();
                jPanel4 = new javax.swing.JPanel();
                explorerJLabel2 = new javax.swing.JLabel();
                includeIncidentsJCheckBox = new javax.swing.JCheckBox();

                setLayout(new java.awt.GridBagLayout());

                setMinimumSize(new java.awt.Dimension(530, 485));
                setPreferredSize(new java.awt.Dimension(530, 485));
                explanationJPanel1.setLayout(new java.awt.GridBagLayout());

                explanationJPanel1.setBorder(new javax.swing.border.TitledBorder(null, "Daily Schedule", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jPanel1.setLayout(new java.awt.GridBagLayout());

                explorerJLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                explorerJLabel1.setText("Every Day");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel1.add(explorerJLabel1, gridBagConstraints);

                dailyEverydayJCheckBox.setFocusable(false);
                dailyEverydayJCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                dailyEverydayJCheckBoxActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel1.add(dailyEverydayJCheckBox, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
                explanationJPanel1.add(jPanel1, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(explanationJPanel1, gridBagConstraints);

                explanationJPanel.setLayout(new java.awt.GridBagLayout());

                explanationJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Weekly Schedule", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jPanel2.setLayout(new java.awt.GridBagLayout());

                weeklySundayJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                weeklySundayJLabel.setText("Sunday");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel2.add(weeklySundayJLabel, gridBagConstraints);

                weeklySundayJCheckBox.setFocusable(false);
                weeklySundayJCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                weeklySundayJCheckBoxActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel2.add(weeklySundayJCheckBox, gridBagConstraints);

                weeklyMondayJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                weeklyMondayJLabel.setText("Monday");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel2.add(weeklyMondayJLabel, gridBagConstraints);

                weeklyMondayJCheckBox.setFocusable(false);
                weeklyMondayJCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                weeklyMondayJCheckBoxActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel2.add(weeklyMondayJCheckBox, gridBagConstraints);

                weeklyTuesdayJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                weeklyTuesdayJLabel.setText("Tuesday");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel2.add(weeklyTuesdayJLabel, gridBagConstraints);

                weeklyTuesdayJCheckBox.setFocusable(false);
                weeklyTuesdayJCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                weeklyTuesdayJCheckBoxActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel2.add(weeklyTuesdayJCheckBox, gridBagConstraints);

                weeklyWednesdayJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                weeklyWednesdayJLabel.setText("Wednesday");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel2.add(weeklyWednesdayJLabel, gridBagConstraints);

                weeklyWednesdayJCheckBox.setFocusable(false);
                weeklyWednesdayJCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                weeklyWednesdayJCheckBoxActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel2.add(weeklyWednesdayJCheckBox, gridBagConstraints);

                weeklyThursdayJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                weeklyThursdayJLabel.setText("Thursday");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 4;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel2.add(weeklyThursdayJLabel, gridBagConstraints);

                weeklyThursdayJCheckBox.setFocusable(false);
                weeklyThursdayJCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                weeklyThursdayJCheckBoxActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 4;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel2.add(weeklyThursdayJCheckBox, gridBagConstraints);

                weeklyFridayJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                weeklyFridayJLabel.setText("Friday");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 5;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel2.add(weeklyFridayJLabel, gridBagConstraints);

                weeklyFridayJCheckBox.setFocusable(false);
                weeklyFridayJCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                weeklyFridayJCheckBoxActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 5;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel2.add(weeklyFridayJCheckBox, gridBagConstraints);

                weeklySaturdayJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                weeklySaturdayJLabel.setText("Saturday");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 6;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel2.add(weeklySaturdayJLabel, gridBagConstraints);

                weeklySaturdayJCheckBox.setFocusable(false);
                weeklySaturdayJCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                weeklySaturdayJCheckBoxActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 6;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel2.add(weeklySaturdayJCheckBox, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
                explanationJPanel.add(jPanel2, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(explanationJPanel, gridBagConstraints);

                externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

                externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Monthly Schedule", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jPanel3.setLayout(new java.awt.GridBagLayout());

                monthlyButtonGroup.add(monthlyNoneJRadioButton);
                monthlyNoneJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                monthlyNoneJRadioButton.setText("Never");
                monthlyNoneJRadioButton.setFocusPainted(false);
                monthlyNoneJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                monthlyNoneJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel3.add(monthlyNoneJRadioButton, gridBagConstraints);

                monthlyButtonGroup.add(monthlyFirstJRadioButton);
                monthlyFirstJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                monthlyFirstJRadioButton.setText("First Day of the Month");
                monthlyFirstJRadioButton.setFocusPainted(false);
                monthlyFirstJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                monthlyFirstJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel3.add(monthlyFirstJRadioButton, gridBagConstraints);

                monthlyButtonGroup.add(monthlyEverydayJRadioButton);
                monthlyEverydayJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                monthlyEverydayJRadioButton.setText("Everyday");
                monthlyEverydayJRadioButton.setFocusPainted(false);
                monthlyEverydayJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                monthlyEverydayJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel3.add(monthlyEverydayJRadioButton, gridBagConstraints);

                monthlyButtonGroup.add(monthlyOnceJRadioButton);
                monthlyOnceJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                monthlyOnceJRadioButton.setText("Once Per Week");
                monthlyOnceJRadioButton.setFocusPainted(false);
                monthlyOnceJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                monthlyOnceJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel3.add(monthlyOnceJRadioButton, gridBagConstraints);

                monthlyOnceJComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
                monthlyOnceJComboBox.setFocusable(false);
                monthlyOnceJComboBox.setMaximumSize(new java.awt.Dimension(150, 24));
                monthlyOnceJComboBox.setMinimumSize(new java.awt.Dimension(150, 24));
                monthlyOnceJComboBox.setPreferredSize(new java.awt.Dimension(150, 24));
                monthlyOnceJComboBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                monthlyOnceJComboBoxActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 4;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 25, 5, 0);
                jPanel3.add(monthlyOnceJComboBox, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
                externalRemoteJPanel.add(jPanel3, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(externalRemoteJPanel, gridBagConstraints);

                externalRemoteJPanel1.setLayout(new java.awt.GridBagLayout());

                externalRemoteJPanel1.setBorder(new javax.swing.border.TitledBorder(null, "Contents", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jPanel4.setLayout(new java.awt.GridBagLayout());

                explorerJLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                explorerJLabel2.setText("Include Incident Lists In Emails");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel4.add(explorerJLabel2, gridBagConstraints);

                includeIncidentsJCheckBox.setFocusable(false);
                includeIncidentsJCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                includeIncidentsJCheckBoxActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                jPanel4.add(includeIncidentsJCheckBox, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
                externalRemoteJPanel1.add(jPanel4, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
                add(externalRemoteJPanel1, gridBagConstraints);

        }//GEN-END:initComponents

    private void setMonthlyDependency(boolean enabled){
	monthlyOnceJComboBox.setEnabled( enabled );
    }

    private void weeklySaturdayJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_weeklySaturdayJCheckBoxActionPerformed
	if( !((Boolean)weeklySaturdayCurrent).equals(weeklySaturdayJCheckBox.isSelected()) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_weeklySaturdayJCheckBoxActionPerformed
        
    private void includeIncidentsJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_includeIncidentsJCheckBoxActionPerformed
	if( !((Boolean)includeIncidentsCurrent).equals(includeIncidentsJCheckBox.isSelected()) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_includeIncidentsJCheckBoxActionPerformed
    
    private void monthlyOnceJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_monthlyOnceJComboBoxActionPerformed
	if( settingsChangedListener != null )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_monthlyOnceJComboBoxActionPerformed
        
    private void weeklyFridayJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_weeklyFridayJCheckBoxActionPerformed
	if( !((Boolean)weeklyFridayCurrent).equals(weeklyFridayJCheckBox.isSelected()) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_weeklyFridayJCheckBoxActionPerformed
    
    private void weeklyThursdayJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_weeklyThursdayJCheckBoxActionPerformed
	if( !((Boolean)weeklyThursdayCurrent).equals(weeklyThursdayJCheckBox.isSelected()) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_weeklyThursdayJCheckBoxActionPerformed
    
    private void weeklyWednesdayJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_weeklyWednesdayJCheckBoxActionPerformed
	if( !((Boolean)weeklyWednesdayCurrent).equals(weeklyWednesdayJCheckBox.isSelected()) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_weeklyWednesdayJCheckBoxActionPerformed
    
    private void weeklyTuesdayJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_weeklyTuesdayJCheckBoxActionPerformed
	if( !((Boolean)weeklyTuesdayCurrent).equals(weeklyTuesdayJCheckBox.isSelected()) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_weeklyTuesdayJCheckBoxActionPerformed
    
    private void weeklyMondayJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_weeklyMondayJCheckBoxActionPerformed
	if( !((Boolean)weeklyMondayCurrent).equals(weeklyMondayJCheckBox.isSelected()) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_weeklyMondayJCheckBoxActionPerformed
    
    private void weeklySundayJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_weeklySundayJCheckBoxActionPerformed
	if( !((Boolean)weeklySundayCurrent).equals(weeklySundayJCheckBox.isSelected()) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_weeklySundayJCheckBoxActionPerformed
    
    private void dailyEverydayJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dailyEverydayJCheckBoxActionPerformed
	if( !((Boolean)dailyCurrent).equals(dailyEverydayJCheckBox.isSelected()) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_dailyEverydayJCheckBoxActionPerformed
    
    private void monthlyOnceJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_monthlyOnceJRadioButtonActionPerformed
	setMonthlyDependency(true);
	if( !((Boolean)monthlyOnceCurrent).equals(monthlyOnceJRadioButton.isSelected()) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_monthlyOnceJRadioButtonActionPerformed
    
    private void monthlyEverydayJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_monthlyEverydayJRadioButtonActionPerformed
	setMonthlyDependency(false);
	if( !((Boolean)monthlyEverydayCurrent).equals(monthlyEverydayJRadioButton.isSelected()) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_monthlyEverydayJRadioButtonActionPerformed
    
    private void monthlyFirstJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_monthlyFirstJRadioButtonActionPerformed
	setMonthlyDependency(false);
	if( !((Boolean)monthlyFirstCurrent).equals(monthlyFirstJRadioButton.isSelected()) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_monthlyFirstJRadioButtonActionPerformed
    
    private void monthlyNoneJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_monthlyNoneJRadioButtonActionPerformed
	setMonthlyDependency(false);
	if( !((Boolean)monthlyNoneCurrent).equals(monthlyNoneJRadioButton.isSelected()) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_monthlyNoneJRadioButtonActionPerformed
    
    
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JCheckBox dailyEverydayJCheckBox;
        private javax.swing.JPanel explanationJPanel;
        private javax.swing.JPanel explanationJPanel1;
        private javax.swing.JLabel explorerJLabel1;
        private javax.swing.JLabel explorerJLabel2;
        private javax.swing.JPanel externalRemoteJPanel;
        private javax.swing.JPanel externalRemoteJPanel1;
        private javax.swing.JCheckBox includeIncidentsJCheckBox;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel2;
        private javax.swing.JPanel jPanel3;
        private javax.swing.JPanel jPanel4;
        private javax.swing.ButtonGroup monthlyButtonGroup;
        private javax.swing.JRadioButton monthlyEverydayJRadioButton;
        private javax.swing.JRadioButton monthlyFirstJRadioButton;
        private javax.swing.JRadioButton monthlyNoneJRadioButton;
        private javax.swing.JComboBox monthlyOnceJComboBox;
        private javax.swing.JRadioButton monthlyOnceJRadioButton;
        private javax.swing.JCheckBox weeklyFridayJCheckBox;
        private javax.swing.JLabel weeklyFridayJLabel;
        private javax.swing.JCheckBox weeklyMondayJCheckBox;
        private javax.swing.JLabel weeklyMondayJLabel;
        private javax.swing.JCheckBox weeklySaturdayJCheckBox;
        private javax.swing.JLabel weeklySaturdayJLabel;
        private javax.swing.JCheckBox weeklySundayJCheckBox;
        private javax.swing.JLabel weeklySundayJLabel;
        private javax.swing.JCheckBox weeklyThursdayJCheckBox;
        private javax.swing.JLabel weeklyThursdayJLabel;
        private javax.swing.JCheckBox weeklyTuesdayJCheckBox;
        private javax.swing.JLabel weeklyTuesdayJLabel;
        private javax.swing.JCheckBox weeklyWednesdayJCheckBox;
        private javax.swing.JLabel weeklyWednesdayJLabel;
        // End of variables declaration//GEN-END:variables
    
}

/*
 * $HeadURL$
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

import java.awt.*;
import java.util.List;

import com.untangle.gui.node.*;
import com.untangle.gui.util.Util;
import com.untangle.node.reporting.*;

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
        Util.addPanelFocus(this, dailyEverydayJCheckBox);
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
            List<WeeklyScheduleRule> dayList = (List<WeeklyScheduleRule>) schedule.getWeeklySched();
            dayList.clear();
            if( weeklySunday )
                dayList.add(new WeeklyScheduleRule( Schedule.SUNDAY ));
            if( weeklyMonday )
                dayList.add(new WeeklyScheduleRule( Schedule.MONDAY ));
            if( weeklyTuesday )
                dayList.add(new WeeklyScheduleRule( Schedule.TUESDAY ));
            if( weeklyWednesday )
                dayList.add(new WeeklyScheduleRule( Schedule.WEDNESDAY ));
            if( weeklyThursday )
                dayList.add(new WeeklyScheduleRule( Schedule.THURSDAY ));
            if( weeklyFriday )
                dayList.add(new WeeklyScheduleRule( Schedule.FRIDAY ));
            if( weeklySaturday )
                dayList.add(new WeeklyScheduleRule( Schedule.SATURDAY ));
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
        weeklySundayCurrent = false;
        weeklyMondayCurrent = false;
        weeklyTuesdayCurrent = false;
        weeklyWednesdayCurrent = false;
        weeklyThursdayCurrent = false;
        weeklyFridayCurrent = false;
        weeklySaturdayCurrent = false;

        List<WeeklyScheduleRule> dayList = (List<WeeklyScheduleRule>) schedule.getWeeklySched();
        for (WeeklyScheduleRule weeklySR : dayList) {
            switch (weeklySR.getDay())
                {
                case Schedule.SUNDAY:
                    weeklySundayCurrent = true;
                    break;
                case Schedule.MONDAY:
                    weeklyMondayCurrent = true;
                    break;
                case Schedule.TUESDAY:
                    weeklyTuesdayCurrent = true;
                    break;
                case Schedule.WEDNESDAY:
                    weeklyWednesdayCurrent = true;
                    break;
                case Schedule.THURSDAY:
                    weeklyThursdayCurrent = true;
                    break;
                case Schedule.FRIDAY:
                    weeklyFridayCurrent = true;
                    break;
                case Schedule.SATURDAY:
                    weeklySaturdayCurrent = true;
                    break;
                }
        }

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
        includeIncidentsJCheckBox.setSelected( includeIncidentsCurrent );

    }





    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        monthlyButtonGroup = new javax.swing.ButtonGroup();
        explanationJPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        dailyEverydayJCheckBox = new javax.swing.JCheckBox();
        explanationJPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        weeklySundayJCheckBox = new javax.swing.JCheckBox();
        weeklyMondayJCheckBox = new javax.swing.JCheckBox();
        weeklyTuesdayJCheckBox = new javax.swing.JCheckBox();
        weeklyWednesdayJCheckBox = new javax.swing.JCheckBox();
        weeklyThursdayJCheckBox = new javax.swing.JCheckBox();
        weeklyFridayJCheckBox = new javax.swing.JCheckBox();
        weeklySaturdayJCheckBox = new javax.swing.JCheckBox();
        externalRemoteJPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        monthlyNoneJRadioButton = new javax.swing.JRadioButton();
        monthlyFirstJRadioButton = new javax.swing.JRadioButton();
        monthlyEverydayJRadioButton = new javax.swing.JRadioButton();
        monthlyOnceJRadioButton = new javax.swing.JRadioButton();
        monthlyOnceJComboBox = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        externalRemoteJPanel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        includeIncidentsJCheckBox = new javax.swing.JCheckBox();
        externalRemoteJPanel2 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        keepAWeekJCheckBox = new javax.swing.JCheckBox();

        setLayout(new java.awt.GridBagLayout());

        setMinimumSize(new java.awt.Dimension(530, 638));
        setPreferredSize(new java.awt.Dimension(530, 638));
        explanationJPanel1.setLayout(new java.awt.GridBagLayout());

        explanationJPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Daily Report Delivery", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel1.setText("<html>This report is delivered at midnight and covers events from the previous 24 hours, up to, but not including the day of delivery.</html>");
        jLabel1.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 5);
        explanationJPanel1.add(jLabel1, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        dailyEverydayJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        dailyEverydayJCheckBox.setText("Every Day");
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
        gridBagConstraints.gridy = 1;
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

        explanationJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Weekly Delivery", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>This report is delivered at midnight and covers events from the previous 7 days, up to, but not including the day of delivery.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 5);
        explanationJPanel.add(jLabel2, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        weeklySundayJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        weeklySundayJCheckBox.setText("Sunday");
        weeklySundayJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                weeklySundayJCheckBoxActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(weeklySundayJCheckBox, gridBagConstraints);

        weeklyMondayJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        weeklyMondayJCheckBox.setText("Monday");
        weeklyMondayJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                weeklyMondayJCheckBoxActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(weeklyMondayJCheckBox, gridBagConstraints);

        weeklyTuesdayJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        weeklyTuesdayJCheckBox.setText("Tuesday");
        weeklyTuesdayJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                weeklyTuesdayJCheckBoxActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(weeklyTuesdayJCheckBox, gridBagConstraints);

        weeklyWednesdayJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        weeklyWednesdayJCheckBox.setText("Wednesday");
        weeklyWednesdayJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                weeklyWednesdayJCheckBoxActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(weeklyWednesdayJCheckBox, gridBagConstraints);

        weeklyThursdayJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        weeklyThursdayJCheckBox.setText("Thursday");
        weeklyThursdayJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                weeklyThursdayJCheckBoxActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(weeklyThursdayJCheckBox, gridBagConstraints);

        weeklyFridayJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        weeklyFridayJCheckBox.setText("Friday");
        weeklyFridayJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                weeklyFridayJCheckBoxActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(weeklyFridayJCheckBox, gridBagConstraints);

        weeklySaturdayJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        weeklySaturdayJCheckBox.setText("Saturday");
        weeklySaturdayJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                weeklySaturdayJCheckBoxActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(weeklySaturdayJCheckBox, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
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

        externalRemoteJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Monthly Delivery", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jPanel3.setLayout(new java.awt.GridBagLayout());

        monthlyButtonGroup.add(monthlyNoneJRadioButton);
        monthlyNoneJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        monthlyNoneJRadioButton.setText("Never");
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
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
        externalRemoteJPanel.add(jPanel3, gridBagConstraints);

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel3.setText("<html>This report is delivered at midnight and covers events from the previous 30 days, up to, but not including the day of delivery.</html>");
        jLabel3.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 5);
        externalRemoteJPanel.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(externalRemoteJPanel, gridBagConstraints);

        externalRemoteJPanel1.setLayout(new java.awt.GridBagLayout());

        externalRemoteJPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Contents", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jPanel4.setLayout(new java.awt.GridBagLayout());

        includeIncidentsJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        includeIncidentsJCheckBox.setText("<html><b>Include Incident Lists in Emails</b> - this makes emailed reports larger, but includes information about each policy incident.</html>");
        includeIncidentsJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                includeIncidentsJCheckBoxActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel4.add(includeIncidentsJCheckBox, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
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

        externalRemoteJPanel2.setLayout(new java.awt.GridBagLayout());

        externalRemoteJPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Data Retention", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jPanel5.setLayout(new java.awt.GridBagLayout());

        keepAWeekJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        keepAWeekJCheckBox.setText("<html><b>Keep One Week's Data</b> - limits data retention to one week, this allows reports to run faster on high traffic sites.</html>");
        keepAWeekJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keepAWeekJCheckBoxActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel5.add(keepAWeekJCheckBox, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
        externalRemoteJPanel2.add(jPanel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        add(externalRemoteJPanel2, gridBagConstraints);

    }// </editor-fold>//GEN-END:initComponents

    private void keepAWeekJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keepAWeekJCheckBoxActionPerformed
// TODO add your handling code here:
    }//GEN-LAST:event_keepAWeekJCheckBoxActionPerformed

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
    private javax.swing.JPanel externalRemoteJPanel;
    private javax.swing.JPanel externalRemoteJPanel1;
    private javax.swing.JPanel externalRemoteJPanel2;
    private javax.swing.JCheckBox includeIncidentsJCheckBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JCheckBox keepAWeekJCheckBox;
    private javax.swing.ButtonGroup monthlyButtonGroup;
    private javax.swing.JRadioButton monthlyEverydayJRadioButton;
    private javax.swing.JRadioButton monthlyFirstJRadioButton;
    private javax.swing.JRadioButton monthlyNoneJRadioButton;
    private javax.swing.JComboBox monthlyOnceJComboBox;
    private javax.swing.JRadioButton monthlyOnceJRadioButton;
    private javax.swing.JCheckBox weeklyFridayJCheckBox;
    private javax.swing.JCheckBox weeklyMondayJCheckBox;
    private javax.swing.JCheckBox weeklySaturdayJCheckBox;
    private javax.swing.JCheckBox weeklySundayJCheckBox;
    private javax.swing.JCheckBox weeklyThursdayJCheckBox;
    private javax.swing.JCheckBox weeklyTuesdayJCheckBox;
    private javax.swing.JCheckBox weeklyWednesdayJCheckBox;
    // End of variables declaration//GEN-END:variables
}

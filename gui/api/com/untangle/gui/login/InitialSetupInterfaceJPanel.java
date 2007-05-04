/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.gui.login;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Vector;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.table.*;

import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.editTable.MEditTableJPanel;
import com.untangle.gui.widgets.editTable.MSortedTableModel;
import com.untangle.gui.widgets.wizard.*;
import com.untangle.mvvm.networking.Interface;

public class InitialSetupInterfaceJPanel extends MWizardPageJPanel {

    private static long SLEEP_MILLIS = 3000l;
    private InterfaceJPanel interfaceJPanel;
    private InterfaceDetectThread interfaceDetectThread;
    private InterfaceListCompoundSettings interfaceListCompoundSettings;

    public InitialSetupInterfaceJPanel() {
        interfaceJPanel = new InterfaceJPanel();
        interfaceListCompoundSettings = new InterfaceListCompoundSettings();
        setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints(0,0,1,1,1d,1d,GridBagConstraints.NORTH,GridBagConstraints.BOTH,new Insets(15,15,100,15),0,0);
        add(interfaceJPanel, gridBagConstraints);

        JLabel backgroundJLabel = new JLabel();
        backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/login/ProductShot.png")));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 1.0;
        add(backgroundJLabel, gridBagConstraints);
    }

    public boolean enteringForwards(){
        interfaceDetectThread = new InterfaceDetectThread();
        return true;
    }
    public boolean enteringBackwards(){
        interfaceDetectThread = new InterfaceDetectThread();
        return true;
    }
    public boolean leavingForwards(){
        interfaceDetectThread.stopDetection();
        return true;
    }
    public boolean leavingBackwards(){
        interfaceDetectThread.stopDetection();
        return true;
    }
    public void finishedAbnormal(){
        if(interfaceDetectThread != null)
            interfaceDetectThread.stopDetection();
    }

    private class InterfaceDetectThread extends Thread {
        private volatile boolean doDetection = true;
        public InterfaceDetectThread(){
            setDaemon(true);
            setName("MV-CLIENT: InterfaceDetectThread");
            start();
        }
        public synchronized void stopDetection(){
            doDetection = false;
        }
        public void run(){
            while(true){
                try{
                    synchronized(this){
                        if(!doDetection)
                            return;
                    }
                    doDetectionUpdates();
                    sleep(SLEEP_MILLIS);
                }
                catch(Exception e){
                    Util.handleExceptionNoRestart("Error sleeping", e);
                }
            }
        }
        private void doDetectionUpdates(){
            try{ interfaceListCompoundSettings.refresh(); }
            catch(Exception e){
                try{ Util.handleExceptionWithRestart("Error refreshing interface list", e); }
                catch(Exception f){ Util.handleExceptionNoRestart("Error refreshing interface list", f); }
                return;
            }
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                interfaceJPanel.doRefresh(interfaceListCompoundSettings);
            }});
        }
    }


    class InterfaceListCompoundSettings{
        private List<Interface> interfaceList;
        public InterfaceListCompoundSettings(){}
        public void save(){}
        public void refresh() throws Exception{
            if(Util.getNetworkManager() == null)
                return; // net connection died
            Util.getNetworkManager().updateLinkStatus();
            interfaceList = Util.getNetworkManager().getNetworkSettings().getInterfaceList();
        }
        public void validate(){}
        public List<Interface> getInterfaceList(){ return interfaceList; }
    }


    class InterfaceJPanel extends MEditTableJPanel{
        private InterfaceModel interfaceModel;
        public InterfaceJPanel() {
            super(false, false);
            super.setFillJButtonEnabled( false );
            super.setInsets(new Insets(4, 4, 2, 2));
            super.setTableTitle("");
            super.setDetailsTitle("");
            super.setAddRemoveEnabled(false);
            super.setAuxJPanelEnabled(true);

            // add a basic description
            JLabel descriptionJLabel = new JLabel("<html>This Interface Test helps you identify your external, internal, and other network interface cards.  It shows you when a network interface is connected to an ethernet cable, or disconnected so you can figure out which card is which.</html>");
            descriptionJLabel.setFont(new Font("Default", 0, 12));
            auxJPanel.setLayout(new BorderLayout());
            auxJPanel.add(descriptionJLabel);

            // create actual table model
            interfaceModel = new InterfaceModel();
            this.setTableModel( interfaceModel );
        }
        public void doRefresh(InterfaceListCompoundSettings interfaceListCompoundSettings){
            interfaceModel.doRefresh(interfaceListCompoundSettings);
        }
    }

    class InterfaceModel extends MSortedTableModel<InterfaceListCompoundSettings>{

        private static final int  T_TW  = Util.TABLE_TOTAL_WIDTH;
        private static final int  C0_MW = Util.STATUS_MIN_WIDTH; /* status */
        private static final int  C1_MW = Util.LINENO_MIN_WIDTH; /* # */
        private static final int  C2_MW = 120;  /* network interface */
        private static final int  C3_MW = 343;  /* connection */



        public TableColumnModel getTableColumnModel(){

            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min    rsz    edit   remv   desc   typ            def
            addTableColumn( tableColumnModel,  0,  C0_MW, false, false, true,  false, String.class,  null, sc.TITLE_STATUS );
            addTableColumn( tableColumnModel,  1,  C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
            addTableColumn( tableColumnModel,  2,  C2_MW, false, false, false, false, String.class, null, sc.html("network<br>interface") );
            addTableColumn( tableColumnModel,  3,  C3_MW, false, false, false, false, String.class, null, sc.html("connection") );
            return tableColumnModel;
        }


        public void generateSettings(InterfaceListCompoundSettings interfaceListCompoundSettings, Vector<Vector> tableVector,
                                     boolean validateOnly) throws Exception {}


        public Vector<Vector> generateRows(InterfaceListCompoundSettings interfaceListCompoundSettings) {
            List<Interface> interfaceList = interfaceListCompoundSettings.getInterfaceList();
            if(interfaceList == null)
                return new Vector<Vector>(); // to deal with disconnection
            Vector<Vector> allRows = new Vector<Vector>(interfaceList.size());
            Vector tempRow = null;
            int rowIndex = 0;

            for( Interface intf : interfaceList ){
                rowIndex++;
                tempRow = new Vector(5);
                tempRow.add( super.ROW_SAVED );
                tempRow.add( rowIndex );
                tempRow.add( intf.getName() );
                tempRow.add( intf.getConnectionState() + (intf.getConnectionState().equals("connected")?" @ "+intf.getCurrentMedia():"") );
                tempRow.add( intf.getCurrentMedia() );
                allRows.add( tempRow );
            }
            return allRows;
        }


    }


}

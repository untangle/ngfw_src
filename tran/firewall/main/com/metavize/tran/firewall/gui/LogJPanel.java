/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.firewall.gui;

import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.logging.EventRepository;
import com.metavize.mvvm.logging.RepositoryDesc;
import com.metavize.mvvm.tran.PipelineEndpoints;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.firewall.*;

public class LogJPanel extends MLogTableJPanel {


    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);

        final Firewall firewall = (Firewall)transform;

        depthJSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent ce) {
                    int v = depthJSlider.getValue();
                    EventManager<FirewallEvent> em = firewall.getEventManager();
                    em.setLimit(v);
                }
            });

        setTableModel(new LogTableModel());

        EventManager<FirewallEvent> eventManager = firewall.getEventManager();
        for (RepositoryDesc fd : eventManager.getRepositoryDescs()) {
            queryJComboBox.addItem(fd.getName());
        }
    }

    protected void refreshSettings(){
        Firewall fw = (Firewall)logTransform;
        EventManager<FirewallEvent> em = fw.getEventManager();
        EventRepository<FirewallEvent> ef = em.getRepository((String)queryJComboBox.getSelectedItem());
        settings = ef.getEvents();
    }

    class LogTableModel extends MSortedTableModel<Object>{

        public TableColumnModel getTableColumnModel(){
            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min  rsz    edit   remv   desc   typ               def
            addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null, "timestamp" );
            addTableColumn( tableColumnModel,  1,  55,  true,  false, false, false, String.class, null, "action" );
            addTableColumn( tableColumnModel,  2,  165, true,  false, false, false, IPPortString.class, null, "client" );
            addTableColumn( tableColumnModel,  3,  100, true,  false, false, false, String.class, null, sc.html("reason for<br>action") );
            addTableColumn( tableColumnModel,  4,  100, true,  false, false, false, String.class, null, sc.html("direction") );
            addTableColumn( tableColumnModel,  5,  165, true,  false, false, false, IPPortString.class, null, "server" );
            return tableColumnModel;
        }

        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}

        public Vector<Vector> generateRows(Object settings){
            List<FirewallEvent> requestLogList = (List<FirewallEvent>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(requestLogList.size());
            Vector event;

            for( FirewallEvent requestLog : requestLogList ){
                event = new Vector(6);
                PipelineEndpoints pe = requestLog.getPipelineEndpoints();
                event.add( null == pe ? "" : requestLog.getTimeStamp() );
                event.add( requestLog.getWasBlocked() ? "blocked" : "passed" );
                event.add( null == pe ? new IPPortString() : new IPPortString(pe.getCClientAddr(), pe.getCClientPort()) );
                event.add( "rule #" + requestLog.getRuleIndex() );
                event.add( null == pe ? "" : pe.getDirectionName() );
                event.add( null == pe ? new IPPortString() : new IPPortString(pe.getSServerAddr(), pe.getSServerPort()) );
                allEvents.add( event );
            }

            return allEvents;
        }

    }

}

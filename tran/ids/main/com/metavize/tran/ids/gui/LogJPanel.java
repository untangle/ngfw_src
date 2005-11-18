/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.ids.gui;

import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.Util;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.logging.EventRepository;
import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.logging.RepositoryDesc;
import com.metavize.mvvm.tran.PipelineEndpoints;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.ids.*;

public class LogJPanel extends MLogTableJPanel {

    private static final String BLOCKED_EVENTS_STRING = "Packet blocked events";

    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);

        final IDSTransform ids = (IDSTransform)transform;

        depthJSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent ce) {
                    int v = depthJSlider.getValue();
                    EventManager<IDSLogEvent> em = ids.getEventManager();
                    em.setLimit(v);
                }
            });

        setTableModel(new LogTableModel());

        EventManager<IDSLogEvent> eventManager = ids.getEventManager();
        for (RepositoryDesc fd : eventManager.getRepositoryDescs()) {
            queryJComboBox.addItem(fd.getName());
        }
    }

    protected void refreshSettings(){
        IDSTransform ids = (IDSTransform)logTransform;
        EventManager<IDSLogEvent> em = ids.getEventManager();
        EventRepository<IDSLogEvent> ef = em.getRepository((String)queryJComboBox.getSelectedItem());
        settings = ef.getEvents();
    }

    class LogTableModel extends MSortedTableModel{

        public TableColumnModel getTableColumnModel(){
            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min  rsz    edit   remv   desc   typ               def
            addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, String.class, null, "timestamp" );
            addTableColumn( tableColumnModel,  1,  55,  true,  false, false, false, String.class, null, "action" );
            addTableColumn( tableColumnModel,  2,  165, true,  false, false, false, String.class, null, sc.html("client") );
            addTableColumn( tableColumnModel,  3,  150, true,  false, false, false, String.class, null, sc.html("reason for<br>action") );
            addTableColumn( tableColumnModel,  4,  100, true,  false, false, false, String.class, null, sc.html("direction") );
            addTableColumn( tableColumnModel,  5,  165, true,  false, false, false, String.class, null, "server" );
            return tableColumnModel;
        }

        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}


        public Vector<Vector> generateRows(Object settings){
            List<IDSLogEvent> logList = (List<IDSLogEvent>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(logList.size());
            Vector event;

            for( IDSLogEvent log : logList ){
                PipelineEndpoints pe = log.getPipelineEndpoints();

                event = new Vector(6);
                event.add( Util.getLogDateFormat().format( log.getTimeStamp() ));
                event.add( log.isBlocked() ? "block" : "pass" );
                event.add( null == pe ? "" : (pe.getCClientAddr().getHostAddress() + ":" + Integer.toString(pe.getCClientPort())));
                event.add( log.getMessage() );
                event.add( null == pe ? "" : pe.getDirectionName() );
                event.add( null == pe ? "" : (pe.getSServerAddr().getHostAddress() + ":" + Integer.toString(pe.getSServerPort())));
                allEvents.add( event );
            }

            return allEvents;
        }
    }
}

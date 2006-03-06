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

package com.metavize.tran.spyware.gui;

import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.Util;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.logging.EventRepository;
import com.metavize.mvvm.logging.RepositoryDesc;
import com.metavize.mvvm.tran.PipelineEndpoints;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.spyware.*;

public class LogJPanel extends MLogTableJPanel {

    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);

        final Spyware spyware = (Spyware)logTransform;

        depthJSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent ce) {
                    int v = depthJSlider.getValue();
                    EventManager<SpywareEvent> em = spyware.getEventManager();
                    em.setLimit(v);
                }
            });

        setTableModel(new LogTableModel());

        EventManager<SpywareEvent> eventManager = spyware.getEventManager();
        for (RepositoryDesc fd : eventManager.getRepositoryDescs()) {
            queryJComboBox.addItem(fd.getName());
        }
    }

    protected void refreshSettings(){
        Spyware spyware = (Spyware)logTransform;
        EventManager<SpywareEvent> em = spyware.getEventManager();
        EventRepository<SpywareEvent> ef = em.getRepository((String)queryJComboBox.getSelectedItem());
        settings = ef.getEvents();
    }

    class LogTableModel extends MSortedTableModel<Object>{

        public TableColumnModel getTableColumnModel(){
            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min  rsz    edit   remv   desc   typ           def
            addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null, "timestamp" );
            addTableColumn( tableColumnModel,  1,  55,  true,  false, false, false, String.class, null, "action" );
            addTableColumn( tableColumnModel,  2,  165, true,  false, false, false, String.class, null, "client" );
            addTableColumn( tableColumnModel,  3,  200, true,  false, false, true,  String.class, null, "request" );
            addTableColumn( tableColumnModel,  4,  100, true,  false, false, false, String.class, null, sc.html("reason for<br>action") );
            addTableColumn( tableColumnModel,  5,  100, true,  false, false, false, String.class, null, sc.html("direction") );
            addTableColumn( tableColumnModel,  6,  165, true,  false, false, false, String.class, null, "server" );
            return tableColumnModel;
        }

        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}

        public Vector<Vector> generateRows(Object settings){
            List<SpywareEvent> requestLogList = (List<SpywareEvent>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(requestLogList.size());
            Vector event;

            for( SpywareEvent requestLog : requestLogList ){
                PipelineEndpoints pe = requestLog.getPipelineEndpoints();
                event = new Vector(7);
                event.add( requestLog.getTimeStamp() );
                event.add(requestLog.isBlocked() ? "block" : "pass");
                event.add(null == pe ? "" : (pe.getCClientAddr().getHostAddress() + ":" + ((Integer)pe.getCClientPort()).toString()));
                event.add(requestLog.getLocation() + " : " + requestLog.getIdentification());
                event.add(requestLog.getReason());
                event.add(pe.getDirectionName());
                event.add(null == pe ? "" : (pe.getSServerAddr().getHostAddress() + ":" + ((Integer)pe.getSServerPort()).toString()));
                allEvents.add(event);
            }

            return allEvents;
        }


    }

}

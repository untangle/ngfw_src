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

package com.untangle.tran.clamphish.gui;

import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;
import com.untangle.mvvm.logging.EventRepository;
import com.untangle.mvvm.logging.EventManager;
import com.untangle.mvvm.logging.RepositoryDesc;
import com.untangle.mvvm.tran.Transform;
import com.untangle.tran.spam.*;
import com.untangle.tran.clamphish.*;

import com.untangle.mvvm.tran.PipelineEndpoints;
import com.untangle.tran.http.HttpRequestEvent;
import com.untangle.tran.http.RequestLine;

public class WebLogJPanel extends MLogTableJPanel {

    private static final String BLOCKED_EVENTS_STRING = "Phish detected events";

    public WebLogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);

        final ClamPhish spam = (ClamPhish)logTransform;

        setTableModel(new LogTableModel());

        EventManager<PhishHttpEvent> eventManager = spam.getPhishHttpEventManager();
        for (RepositoryDesc fd : eventManager.getRepositoryDescs()) {
            queryJComboBox.addItem(fd.getName());
        }
    }

    protected void refreshSettings(){
        ClamPhish spam = (ClamPhish)logTransform;
        EventManager<PhishHttpEvent> em = spam.getPhishHttpEventManager();
        EventRepository<PhishHttpEvent> ef = em.getRepository((String)queryJComboBox.getSelectedItem());
        settings = ef.getEvents();
    }

    class LogTableModel extends MSortedTableModel<Object>{

        public TableColumnModel getTableColumnModel(){
            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min  rsz    edit   remv   desc   typ               def
            addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null, "timestamp" );
            addTableColumn( tableColumnModel,  1,  55,  true,  false, false, false, String.class, null, "action" );
            addTableColumn( tableColumnModel,  2,  165, true,  false, false, false, IPPortString.class, null, "client" );
            addTableColumn( tableColumnModel,  3,  200, true,  false, false, true,  String.class, null, "request" );
            addTableColumn( tableColumnModel,  4,  100, true,  false, false, false, String.class, null, sc.html("direction") );
            addTableColumn( tableColumnModel,  5,  165, true,  false, false, false, IPPortString.class, null, "server" );

            return tableColumnModel;
        }

        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}


        public Vector<Vector> generateRows(Object settings){
            List<PhishHttpEvent> requestLogList = (List<PhishHttpEvent>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(requestLogList.size());
            Vector event;

            for( PhishHttpEvent requestLog : requestLogList ){
                RequestLine rl = requestLog.getRequestLine();
                HttpRequestEvent re = null == rl ? null : rl.getHttpRequestEvent();
                PipelineEndpoints pe = null == rl ? null : rl.getPipelineEndpoints();

                event = new Vector(6);
                event.add( requestLog.getEventTimeStamp() );
                Action a = requestLog.getAction();
                event.add( null == a ? "none" : a.toString() );
                event.add( null == pe ? new IPPortString() : new IPPortString(pe.getCClientAddr(), pe.getCClientPort()) );
                event.add( null == rl ? "" : rl.getUrl().toString() );
                event.add( null == pe ? "" : pe.getDirectionName() );
                event.add( null == pe ? new IPPortString() : new IPPortString(pe.getSServerAddr(), pe.getSServerPort()) );
                allEvents.add( event );
            }

            return allEvents;
        }




    }

}

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


package com.untangle.node.httpblocker.gui;

import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.EventRepository;
import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.node.PipelineEndpoints;
import com.untangle.uvm.node.Node;
import com.untangle.node.http.HttpRequestEvent;
import com.untangle.node.http.RequestLine;
import com.untangle.node.httpblocker.Action;
import com.untangle.node.httpblocker.HttpBlocker;
import com.untangle.node.httpblocker.HttpBlockerEvent;
import com.untangle.node.httpblocker.Reason;

public class LogJPanel extends MLogTableJPanel {

    public LogJPanel(Node node, MNodeControlsJPanel mNodeControlsJPanel){
        super(node, mNodeControlsJPanel);

        final HttpBlocker httpBlocker = (HttpBlocker)logNode;

        setTableModel(new LogTableModel());

        EventManager<HttpBlockerEvent> eventManager = httpBlocker.getEventManager();
        for (RepositoryDesc fd : eventManager.getRepositoryDescs()) {
            queryJComboBox.addItem(fd.getName());
        }
    }

    protected void refreshSettings(){
        HttpBlocker httpBlocker = (HttpBlocker)logNode;
        EventManager<HttpBlockerEvent> em = httpBlocker.getEventManager();
        EventRepository<HttpBlockerEvent> ef = em.getRepository((String)queryJComboBox.getSelectedItem());
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
            addTableColumn( tableColumnModel,  4,  140, true,  false, false, false, String.class, null, sc.html("reason for<br>action") );
            addTableColumn( tableColumnModel,  5,  100, true,  false, false, false, String.class, null, sc.html("direction") );
            addTableColumn( tableColumnModel,  6,  165, true,  false, false, false, IPPortString.class, null, "server" );

            return tableColumnModel;
        }

        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}

        public Vector<Vector> generateRows(Object settings){
            List<HttpBlockerEvent> requestLogList = (List<HttpBlockerEvent>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(requestLogList.size());
            Vector event;

            for( HttpBlockerEvent requestLog : requestLogList ){
                RequestLine rl = requestLog.getRequestLine();
                HttpRequestEvent re = null == rl ? null : rl.getHttpRequestEvent();
                PipelineEndpoints pe = null == rl ? null : rl.getPipelineEndpoints();

                event = new Vector(7);
                event.add( requestLog.getTimeStamp() );
                Action a = requestLog.getAction();
                event.add( null == a ? "none" : a.toString() );
                event.add( null == pe ? new IPPortString() : new IPPortString(pe.getCClientAddr(), pe.getCClientPort()) );
                event.add( null == rl ? "" : rl.getUrl().toString() );
                Reason r = requestLog.getReason();
                event.add( null == r ? "none" : r.toString() );
                event.add( null == pe ? "" : pe.getDirectionName() );
                event.add( null == pe ? new IPPortString() : new IPPortString(pe.getSServerAddr(), pe.getSServerPort()) );
                allEvents.add( event );
            }

            return allEvents;
        }

    }

}

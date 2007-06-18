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


package com.untangle.node.webfilter.gui;

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
import com.untangle.node.webfilter.Action;
import com.untangle.node.webfilter.WebFilter;
import com.untangle.node.webfilter.WebFilterEvent;
import com.untangle.node.webfilter.Reason;

public class LogJPanel extends MLogTableJPanel {

    public LogJPanel(Node node, MNodeControlsJPanel mNodeControlsJPanel){
        super(node, mNodeControlsJPanel);

        final WebFilter webFilter = (WebFilter)logNode;

        setTableModel(new LogTableModel());

        EventManager<WebFilterEvent> eventManager = webFilter.getEventManager();
        for (RepositoryDesc fd : eventManager.getRepositoryDescs()) {
            queryJComboBox.addItem(fd.getName());
        }
    }

    protected void refreshSettings(){
        WebFilter webFilter = (WebFilter)logNode;
        EventManager<WebFilterEvent> em = webFilter.getEventManager();
        EventRepository<WebFilterEvent> ef = em.getRepository((String)queryJComboBox.getSelectedItem());
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
            List<WebFilterEvent> requestLogList = (List<WebFilterEvent>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(requestLogList.size());
            Vector event;

            for( WebFilterEvent requestLog : requestLogList ){
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

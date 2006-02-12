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


package com.metavize.tran.openvpn.gui;

import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.Util;
import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.logging.EventRepository;
import com.metavize.mvvm.logging.RepositoryDesc;
import com.metavize.mvvm.tran.PipelineEndpoints;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.openvpn.*;

public class LogJPanel extends MLogTableJPanel {

    private VpnTransform vpnTransform;

    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);

	vpnTransform = (VpnTransform)logTransform;

        depthJSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent ce) {
                    int v = depthJSlider.getValue();
                    EventManager<ClientConnectEvent> em = vpnTransform.getClientConnectEventManager();
                    em.setLimit(v);
                }
            });

        setTableModel(new LogTableModel());

        EventManager<ClientConnectEvent> eventManager = vpnTransform.getClientConnectEventManager();
        for (RepositoryDesc fd : eventManager.getRepositoryDescs()) {
            queryJComboBox.addItem(fd.getName());
        }
    }

    protected void refreshSettings(){
	EventManager<ClientConnectEvent> em = vpnTransform.getClientConnectEventManager();
        EventRepository<ClientConnectEvent> ef = em.getRepository((String)queryJComboBox.getSelectedItem());
        settings = ef.getEvents();
    }

    class LogTableModel extends MSortedTableModel<Object>{

        public TableColumnModel getTableColumnModel(){
            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min  rsz    edit   remv   desc   typ               def
            addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null, "start time" );
            addTableColumn( tableColumnModel,  1,  150, true,  false, false, false, Date.class,   null, "end time" );
            addTableColumn( tableColumnModel,  2,  150, true,  false, false, false, String.class, null, "client name" );
            addTableColumn( tableColumnModel,  3,  150, true,  false, false, false, String.class, null, "client address" );
            addTableColumn( tableColumnModel,  4,  100, true,  false, false, false, Integer.class, null, sc.html("Kbytes<br>sent") );
            addTableColumn( tableColumnModel,  5,  100, true,  false, false, false, Integer.class, null, sc.html("Kbytes<br>received") );

            return tableColumnModel;
        }

        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}

        public Vector<Vector> generateRows(Object settings){
            List<ClientConnectEvent> requestLogList = (List<ClientConnectEvent>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(requestLogList.size());
            Vector event;

            for( ClientConnectEvent requestLog : requestLogList ){

                event = new Vector(6);
		event.add( requestLog.getStart() );
		event.add( requestLog.getEnd() );
		event.add( requestLog.getClientName() );
		event.add( requestLog.getAddress().toString() + ":" + requestLog.getPort() );
		event.add( requestLog.getBytesTx()/1024l );
		event.add( requestLog.getBytesRx()/1024l );
                allEvents.add( event );
            }

            return allEvents;
        }

    }

}

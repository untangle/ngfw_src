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

package com.metavize.tran.portal.gui;

import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.logging.EventRepository;
import com.metavize.mvvm.logging.RepositoryDesc;
import com.metavize.mvvm.tran.PipelineEndpoints;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.portal.*;
import com.metavize.mvvm.portal.*;

public class LogJPanel extends MLogTableJPanel {


    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);

        final PortalTransform portalTransform = (PortalTransform)transform;

        depthJSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent ce) {
                    int v = depthJSlider.getValue();
                    EventManager<LogEvent> em = portalTransform.getEventManager();
                    em.setLimit(v);
                }
            });

        setTableModel(new LogTableModel());

        EventManager<LogEvent> eventManager = portalTransform.getEventManager();
        for (RepositoryDesc fd : eventManager.getRepositoryDescs()) {
            queryJComboBox.addItem(fd.getName());
        }
    }

    protected void refreshSettings(){
        PortalTransform portalTransform = (PortalTransform)logTransform;
        EventManager<LogEvent> em = portalTransform.getEventManager();
        EventRepository<LogEvent> ef = em.getRepository((String)queryJComboBox.getSelectedItem());
        settings = ef.getEvents();
    }

    class LogTableModel extends MSortedTableModel<Object>{

        public TableColumnModel getTableColumnModel(){
            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min  rsz    edit   remv   desc   typ               def
            addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null, "timestamp" );
            addTableColumn( tableColumnModel,  1,  55,  true,  false, false, false, String.class, null, "action" );
            addTableColumn( tableColumnModel,  2,  165, true,  false, false, false, String.class, null, "user id/login" );
            addTableColumn( tableColumnModel,  3,  165, true,  false, false, false, IPaddrString.class, null, "client" );
            addTableColumn( tableColumnModel,  4,  100, true,  false, false, false, String.class, null, sc.html("success") );
            addTableColumn( tableColumnModel,  5,  100, true,  false, false, false, String.class, null, sc.html("reason") );
            return tableColumnModel;
        }

        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}

        public Vector<Vector> generateRows(Object settings){
            List<LogEvent> requestLogList = (List<LogEvent>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(requestLogList.size());
            Vector event;

            for( LogEvent requestLog : requestLogList ){
                event = new Vector(6);

                event.add( requestLog.getTimeStamp() );

		if( requestLog instanceof PortalLogoutEvent ){
		    PortalLogoutEvent newEvent = (PortalLogoutEvent) requestLog;
		    event.add( "Logout" );
		    event.add( newEvent.getUid() );
		    event.add( new IPaddrString(newEvent.getClientAddr()) );
		    event.add( "Success" );
		    event.add( "" );
		    allEvents.add( event );
		}
		else if( requestLog instanceof PortalLoginEvent ){
		    PortalLoginEvent newEvent = (PortalLoginEvent) requestLog;
		    event.add( "Login" );
		    event.add( newEvent.getUid() );
		    event.add( new IPaddrString(newEvent.getClientAddr()) );
		    event.add( newEvent.isSucceeded()==true?"Success":"Failure" );
		    event.add( newEvent.getReason().toString() );
		    allEvents.add( event );
		}
            }

            return allEvents;
        }

    }

}

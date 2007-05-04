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

package com.untangle.gui.pipeline;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JPanel;

import com.untangle.gui.main.*;
import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.separator.*;
import com.untangle.mvvm.security.Tid;


public class RackSection<T> {

    // FOR INIT PURPOSES ONLY //
    private Map<T,List<Tid>>          tidMap  = new TreeMap<T,List<Tid>>();
    private Map<T,Map<String,Object>> nameMap = new TreeMap<T,Map<String,Object>>();

    // GUI DATA MODEL //
    private Map<T,Map<ButtonKey,MTransformJButton>> toolboxDataMap = new TreeMap<T,Map<ButtonKey,MTransformJButton>>();
    private Map<T,Map<ButtonKey,MTransformJPanel>>  rackDataMap    = new TreeMap<T,Map<ButtonKey,MTransformJPanel>>();

    // GUI VIEW MODEL //
    private JPanel        toolboxViewJPanel = new JPanel();
    private JPanel        rackViewJPanel    = new JPanel();
    private Map<T,JPanel> toolboxViewMap    = new TreeMap<T,JPanel>();
    private Map<T,JPanel> rackViewMap       = new TreeMap<T,JPanel>();

    // SEPARATOR //
    private Separator separator;

    // CONSTRAINTS //
    private GridBagConstraints separatorViewConstraints = new GridBagConstraints( 0, 0, 1, 1, 0d, 0d,
                                                                                  GridBagConstraints.NORTH,
                                                                                  GridBagConstraints.NONE,
                                                                                  new Insets(1,0,101,12), 0, 0);
    private GridBagConstraints rackViewConstraints = new GridBagConstraints( 0, 0, 1, 1, 0d, 0d,
                                                                             GridBagConstraints.SOUTH,
                                                                             GridBagConstraints.NONE,
                                                                             new Insets(51,0,0,12), 0, 0);

    public RackSection(Separator separator) {
        this.separator = separator;
        toolboxViewJPanel.setOpaque(false);
        rackViewJPanel.setOpaque(false);
        toolboxViewJPanel.setLayout(new GridBagLayout());
        rackViewJPanel.setLayout(new GridBagLayout());

    }

    public void addToRack(T selector, MTransformJPanel appliance, boolean doRevalidate){
        if( rackDataMap.isEmpty() ){
            // ADD SEPARATOR
            rackViewJPanel.add(separator, separatorViewConstraints);
        }
        Map<ButtonKey,MTransformJPanel> applianceMap;
        if( !rackDataMap.containsKey(selector) ){
            applianceMap = new TreeMap<ButtonKey,MTransformJPanel>();
            rackDataMap.put(selector, applianceMap);
        }

    }



}

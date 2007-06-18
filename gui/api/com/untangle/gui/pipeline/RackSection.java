/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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
import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.separator.*;
import com.untangle.uvm.security.Tid;


public class RackSection<T> {

    // FOR INIT PURPOSES ONLY //
    private Map<T,List<Tid>>          tidMap  = new TreeMap<T,List<Tid>>();
    private Map<T,Map<String,Object>> nameMap = new TreeMap<T,Map<String,Object>>();

    // GUI DATA MODEL //
    private Map<T,Map<ButtonKey,MNodeJButton>> toolboxDataMap = new TreeMap<T,Map<ButtonKey,MNodeJButton>>();
    private Map<T,Map<ButtonKey,MNodeJPanel>>  rackDataMap    = new TreeMap<T,Map<ButtonKey,MNodeJPanel>>();

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

    public void addToRack(T selector, MNodeJPanel appliance, boolean doRevalidate){
        if( rackDataMap.isEmpty() ){
            // ADD SEPARATOR
            rackViewJPanel.add(separator, separatorViewConstraints);
        }
        Map<ButtonKey,MNodeJPanel> applianceMap;
        if( !rackDataMap.containsKey(selector) ){
            applianceMap = new TreeMap<ButtonKey,MNodeJPanel>();
            rackDataMap.put(selector, applianceMap);
        }

    }



}

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

package com.untangle.gui.widgets.wizard;


import com.untangle.gui.node.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.coloredTable.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;

import com.untangle.uvm.security.PasswordUtil;
import com.untangle.uvm.*;
import com.untangle.uvm.node.*;

import javax.swing.border.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;

public class MWizardPageJPanel extends javax.swing.JPanel {

    protected boolean leavingForwards(){ return true; }
    protected boolean leavingBackwards(){ return true; }
    protected boolean enteringForwards(){ return true; }
    protected boolean enteringBackwards(){ return true; }
    protected void initialFocus(){}
    
    protected void doSave(Object settings, boolean validateOnly) throws Exception {}
    
}

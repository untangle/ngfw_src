/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.widgets.wizard;


import com.metavize.gui.transform.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.coloredTable.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.security.PasswordUtil;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;

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

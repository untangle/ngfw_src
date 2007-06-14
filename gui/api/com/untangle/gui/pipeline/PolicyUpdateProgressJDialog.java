/*
 * $HeadURL:$
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

import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.uvm.policy.*;
import com.untangle.gui.configuration.*;

import java.awt.Frame;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;



public class PolicyUpdateProgressJDialog extends MProgressJDialog {

    private List<Policy> policies;

    public PolicyUpdateProgressJDialog( Frame parentFrame ) {
        super("Updating Racks", "Please wait while your racks are updated.", parentFrame);
    }

    public List<Policy> getPolicies(){ return policies; }

    public void setVisible(boolean isVisible){
        if(isVisible){
            new UpdatePolicyThread();
        }
        super.setVisible(isVisible);
    }

    private class UpdatePolicyThread extends Thread {
        public UpdatePolicyThread(){
            setDaemon(true);
            setName("MV-PolicyUpdateThread");
            PolicyUpdateProgressJDialog.this.getJProgressBar().setValue(1);
            PolicyUpdateProgressJDialog.this.getJProgressBar().setIndeterminate(true);
            PolicyUpdateProgressJDialog.this.getJProgressBar().setString("Updating...");
            start();
        }
        public void run(){
            try{
                policies = Util.getPolicyManager().getPolicyConfiguration().getPolicies();
                sleep(1500l);
            }
            catch(Exception e){
                try{ Util.handleExceptionWithRestart("Error handling policy manager action", e); }
                catch(Exception f){ Util.handleExceptionNoRestart("Error handling policy manager action", f); }
            }
            finally{
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    PolicyUpdateProgressJDialog.this.setVisible(false);
                }});
            }
        }
    }
}

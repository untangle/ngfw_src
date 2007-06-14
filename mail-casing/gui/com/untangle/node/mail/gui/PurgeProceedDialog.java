/*
 * $HeadURL:$
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

package com.untangle.node.mail.gui;

import java.awt.Dialog;

import com.untangle.gui.widgets.dialogs.*;

final public class PurgeProceedDialog extends MTwoButtonJDialog {

    public PurgeProceedDialog(Dialog topLevelDialog) {
        super( topLevelDialog );
        this.setTitle("Email Quarantine Purge Warning");
        this.cancelJButton.setIcon(null);
        this.proceedJButton.setIcon(null);
        this.cancelJButton.setText("<html><b>Cancel</b> purge</html>");
        this.proceedJButton.setText("<html><b>Continue</b> purging</html>");
        messageJLabel.setText("<html><center>Purging emails will permanently delete them<br>"
                              + "so that no one can view them.<br>"
                              + "Purged emails cannot be recovered."
                              + "<br><b>Would you like to purge?<b></center></html>");
        this.setVisible(true);
    }

}

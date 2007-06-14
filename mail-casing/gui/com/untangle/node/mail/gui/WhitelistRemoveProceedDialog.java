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

final public class WhitelistRemoveProceedDialog extends MTwoButtonJDialog {

    public WhitelistRemoveProceedDialog(Dialog topLevelDialog) {
        super( topLevelDialog );
        this.setTitle("Email Quarantine Whitelist Remove Warning");
        this.cancelJButton.setIcon(null);
        this.proceedJButton.setIcon(null);
        this.cancelJButton.setText("<html><b>Cancel</b> remove</html>");
        this.proceedJButton.setText("<html><b>Continue</b> removing</html>");
        messageJLabel.setText("<html><center>Removing a whitelist will remove all addresses<br>"
                              + "currently in that whitelist.<br>"
                              + "Any email can then end up in quarantine."
                              + "<br><b>Would you like to remove?<b></center></html>");
        this.setVisible(true);
    }

}

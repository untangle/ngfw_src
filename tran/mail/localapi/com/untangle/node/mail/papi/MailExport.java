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

package com.untangle.node.mail.papi;

import com.untangle.node.mail.papi.quarantine.QuarantineNodeView;
import com.untangle.node.mail.papi.safelist.SafelistNodeView;


public interface MailExport
{
    MailNodeSettings getExportSettings();

    /**
     * Access the Object which is used to submit Mails
     * to the quarantine.
     *
     * @return the QuarantineNodeView
     */
    QuarantineNodeView getQuarantineNodeView();

    /**
     * Access the object used to consult the Safelist manager
     * while processing mails
     *
     * @return the SafelistNodeView
     */
    SafelistNodeView getSafelistNodeView();
}

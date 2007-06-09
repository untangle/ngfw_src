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

package com.untangle.tran.mail.papi;

import com.untangle.tran.mail.papi.quarantine.QuarantineTransformView;
import com.untangle.tran.mail.papi.safelist.SafelistTransformView;


public interface MailExport
{
    MailTransformSettings getExportSettings();

    /**
     * Access the Object which is used to submit Mails
     * to the quarantine.
     *
     * @return the QuarantineTransformView
     */
    QuarantineTransformView getQuarantineTransformView();

    /**
     * Access the object used to consult the Safelist manager
     * while processing mails
     *
     * @return the SafelistTransformView
     */
    SafelistTransformView getSafelistTransformView();
}

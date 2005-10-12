/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.papi;

import com.metavize.tran.mail.papi.quarantine.QuarantineTransformView;


public interface MailExport
{
    MailTransformSettings getExportSettings();

    /**
     * Access the Object which is used to submit Mails
     * to the quarantine.
     */
    QuarantineTransformView getQuarantineTransformView();
}

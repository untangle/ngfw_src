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

package com.untangle.tran.mail.papi.quarantine;

import java.io.File;

import com.untangle.tran.mime.EmailAddress;

/**
 * Interface for the transforms to insert
 * messages into the quarantine.  This is
 * not intended to be "remoted".
 */
public interface QuarantineTransformView {

    /**
     * Quarantine the given message, destined for
     * the named recipients.
     * <br><br>
     * Callers should be prepared for the case
     * that after making this call, the underlying
     * File from the MIMEMessage may have been
     * "stolen" (moved).
     *
     * @param file the file containing the message to
     *        be quarantined
     * @param summary a summary of the mail
     * @param recipients any recipients for the mail
     *
     * @return true if the mail was quarantined.
     */
    public boolean quarantineMail(File file,
                                  MailSummary summary,
                                  EmailAddress...recipients);


}

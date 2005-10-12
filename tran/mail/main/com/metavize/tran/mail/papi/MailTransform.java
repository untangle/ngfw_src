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

import com.metavize.tran.mail.papi.quarantine.QuarantineUserView;

public interface MailTransform
{
    MailTransformSettings getMailTransformSettings();
    void setMailTransformSettings(MailTransformSettings settings);

    /**
     * Get the interface to the Quarantine used for end-user
     * interaction
     *
     * @return the QuarantineUserView
     */
    QuarantineUserView getQuarantineUserView();
}

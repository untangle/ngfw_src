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
import com.metavize.tran.mail.papi.quarantine.QuarantineMaintenenceView;
import com.metavize.tran.mail.papi.safelist.SafelistEndUserView;
import com.metavize.tran.mail.papi.safelist.SafelistAdminView;

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


    /**
     * Get the interface to the Quarantine for Administrative
     * interaction (other than {@link #setMailTransformSettings property}
     * manipulation.
     *
     * @return the QuarantineMaintenenceView
     */
    QuarantineMaintenenceView getQuarantineMaintenenceView();

    /**
     * Get the interface to the Safelist facility for end-user
     * interaction
     *
     * @return the SafelistEndUserView
     */
    SafelistEndUserView getSafelistEndUserView();


    /**
     * Get the interface to the Safelist subsystem for Administrative
     * interaction (other than {@link #setMailTransformSettings property}
     * manipulation.
     *
     * @return the SafelistAdminView
     */
    SafelistAdminView getSafelistAdminView();
}

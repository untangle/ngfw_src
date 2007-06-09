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

import com.untangle.tran.mail.papi.quarantine.QuarantineUserView;
import com.untangle.tran.mail.papi.quarantine.QuarantineMaintenenceView;
import com.untangle.tran.mail.papi.safelist.SafelistEndUserView;
import com.untangle.tran.mail.papi.safelist.SafelistAdminView;

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

    /**
     * minimum size of the space allocated for the entire store (in B or GB)
     * (e.g., minimum space allocated on HDD that store can use) 
     * -> in GB if inGB == true or in B if inGB == false
     */
    public long getMinAllocatedStoreSize(boolean inGB);

    /**
     * maximum size of the space allocated for the entire store (in B or GB)
     * (e.g., maximum space allocated on HDD that store can use) 
     * -> in GB if inGB == true or in B if inGB == false
     */
    public long getMaxAllocatedStoreSize(boolean inGB);
}

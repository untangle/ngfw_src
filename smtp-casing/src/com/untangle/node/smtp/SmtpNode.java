/**
 * $Id$
 */
package com.untangle.node.smtp;

import com.untangle.node.smtp.quarantine.QuarantineMaintenenceView;
import com.untangle.node.smtp.quarantine.QuarantineUserView;
import com.untangle.node.smtp.safelist.SafelistAdminView;
import com.untangle.node.smtp.safelist.SafelistEndUserView;

public interface SmtpNode
{
    SmtpNodeSettings getSmtpNodeSettings();
    void setSmtpNodeSettings(SmtpNodeSettings settings);
    void setSmtpNodeSettingsWithoutSafelists(SmtpNodeSettings settings);

    /**
     * Get the interface to the Quarantine used for end-user
     * interaction
     *
     * @return the QuarantineUserView
     */
    QuarantineUserView getQuarantineUserView();


    /**
     * Get the interface to the Quarantine for Administrative
     * interaction (other than {@link #setSmtpNodeSettings property}
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
     * interaction (other than {@link #setSmtpNodeSettings property}
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

    /**
     * Retrieve an authentication token for an email address.
     */
    public String createAuthToken(String account);
}

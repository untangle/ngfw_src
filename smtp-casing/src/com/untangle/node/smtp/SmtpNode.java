/**
 * $Id: SmtpNode.java 34290 2013-03-17 00:00:19Z dmorris $
 */
package com.untangle.node.smtp;

import java.util.List;

import com.untangle.node.smtp.quarantine.QuarantineMaintenenceView;
import com.untangle.node.smtp.quarantine.QuarantineUserView;
import com.untangle.node.smtp.safelist.SafelistAdminView;
import com.untangle.node.smtp.safelist.SafelistManipulation;

public interface SmtpNode
{
    SmtpNodeSettings getSmtpNodeSettings();

    void setSmtpNodeSettings(SmtpNodeSettings settings);

    void setSmtpNodeSettingsWithoutSafelists(SmtpNodeSettings settings);

    /**
     * Get the interface to the Quarantine used for end-user interaction
     * 
     * @return the QuarantineUserView
     */
    QuarantineUserView getQuarantineUserView();

    /**
     * Get the interface to the Quarantine for Administrative interaction (other than {@link #setSmtpNodeSettings
     * property} manipulation.
     * 
     * @return the QuarantineMaintenenceView
     */
    QuarantineMaintenenceView getQuarantineMaintenenceView();

    /**
     * Get the interface to the Safelist facility for end-user interaction
     * 
     * @return the SafelistEndUserView
     */
    SafelistManipulation getSafelistManipulation();

    /**
     * Get the interface to the Safelist subsystem for Administrative interaction (other than
     * {@link #setSmtpNodeSettings property} manipulation.
     * 
     * @return the SafelistAdminView
     */
    SafelistAdminView getSafelistAdminView();

    /**
     * minimum size of the space allocated for the entire store (in B or GB) (e.g., minimum space allocated on HDD that
     * store can use) -> in GB if inGB == true or in B if inGB == false
     */
    public long getMinAllocatedStoreSize(boolean inGB);

    /**
     * maximum size of the space allocated for the entire store (in B or GB) (e.g., maximum space allocated on HDD that
     * store can use) -> in GB if inGB == true or in B if inGB == false
     */
    public long getMaxAllocatedStoreSize(boolean inGB);

    /**
     * Retrieve an authentication token for an email address.
     */
    public String createAuthToken(String account);

    public List<String> getTests();
    
    public String runTests();
    
    public String runTests(String path);
}

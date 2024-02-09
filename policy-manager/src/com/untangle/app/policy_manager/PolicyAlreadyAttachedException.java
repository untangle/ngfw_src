/**
 * $Id$
 */
package com.untangle.app.policy_manager;

/**
 * Exception to be thrown when policy is being removed while already being attached to one or more apps
 */
@SuppressWarnings("serial")
public class PolicyAlreadyAttachedException extends RuntimeException {

    /**
     * Initialize instance of PolicyAlreadyAttachedException.
     * @param  policyId the policyId
     * @param  app the app name which is attached to the policy
     * @return       Instance of PolicyAlreadyAttachedException.
     */
    public PolicyAlreadyAttachedException(Integer policyId, String app) {
        super("Missing policy: " + policyId + " (Required by [" + app + "]). Cannot delete non-empty racks.");
    }

    /**
     * Initialize instance of PolicyAlreadyAttachedException.
     * @param  message Exception message
     * @return       Instance of PolicyAlreadyAttachedException.
     */
    public PolicyAlreadyAttachedException(String message) {
        super(message);
    }
    
}

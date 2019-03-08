/**
 * $Id: CloudManager.java,v 1.00 2016/10/28 11:46:12 dmorris Exp $
 */
package com.untangle.uvm;

import org.json.JSONObject;

public interface CloudManager
{
    /**
     * Login to untangle.com with the provided login and password.
     *
     * This returns the JSON result from the /api/v1/account/login from untangle.com
     * Example: {"success":true,"token":"48fdc8e8ae3ef964ee2f3a88c5e57acb"}
     * Example: {"success":false,"customerMessage":"We were unable to validate your credentials.","developerMessage":"Provided password did not match stored password."}
     *
     * This method throws Exceptions under many circumstances so the callee must handle this.
     * This can include any network failure, or parse exceptions
     */
    JSONObject accountLogin( String login, String password ) throws Exception;

    /**
     * Login to untangle.com with the provided login and password.
     *
     * uid, applianceModel, majorVersion, installType are optional parameters that are sent to the auth service
     *
     * This returns the JSON result from the /api/v1/account/login from untangle.com
     * Example: {"success":true,"token":"48fdc8e8ae3ef964ee2f3a88c5e57acb"}
     * Example: {"success":false,"customerMessage":"We were unable to validate your credentials.","developerMessage":"Provided password did not match stored password."}
     *
     * This method throws Exceptions under many circumstances so the callee must handle this.
     * This can include any network failure, or parse exceptions
     */
    JSONObject accountLogin( String email, String password, String uid, String applianceModel, String majorVersion, String installType ) throws Exception;

    /**
     * Create an untangle.com account with the provided information.
     *
     * This returns the JSON result from the /api/v1/account/create from untangle.com
     * Example: {"success":true, "token":"7a4905c9501c2eb9adc12e7e7d1176df"}
     * Example: {"success":false,"customerMessage":"We could not create your account.","developerMessage":"Could not create the Wordpress or Shopp account."}
     *
     * This method throws Exceptions under many circumstances so the callee must handle this.
     * This can include any network failure, or parse exceptions
     */
    JSONObject accountCreate( String email, String password, String firstName, String lastName, String companyName, String uid, String applianceModel, String majorVersion, String installType ) throws Exception;

    /**
     * 
     */
    void startZeroTouchMonitor();
}

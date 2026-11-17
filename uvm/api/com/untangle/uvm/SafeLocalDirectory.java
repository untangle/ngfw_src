/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.LinkedList;

/**
 * Reviewed callable operations for SafeLocalDirectory.
 */
public interface SafeLocalDirectory
{
    /**
     * addRadiusComputerAccount through the reviewed RPC contract.
     * @return The delegated result
     */
    ExecManagerResult addRadiusComputerAccount();

    /**
     * generateSecret through the reviewed RPC contract.
     * @return The delegated result
     */
    String generateSecret();

    /**
     * getRadiusLogFile through the reviewed RPC contract.
     * @return The delegated result
     */
    String getRadiusLogFile();

    /**
     * getRadiusProxyStatus through the reviewed RPC contract.
     * @return The delegated result
     */
    String getRadiusProxyStatus();

    /**
     * getUsers through the reviewed RPC contract.
     * @return The delegated result
     */
    LinkedList<LocalDirectoryUser> getUsers();

    /**
     * notifyRadiusUserLogin through the reviewed RPC contract.
     * @param userName The userName argument
     * @param macAddress The macAddress argument
     */
    void notifyRadiusUserLogin(String userName, String macAddress);

    /**
     * setUsers through the reviewed RPC contract.
     * @param users The users argument
     */
    void setUsers(LinkedList<LocalDirectoryUser> users);

    /**
     * showSecretQR through the reviewed RPC contract.
     * @param username The username argument
     * @param issuer The issuer argument
     * @param secret The secret argument
     * @return The delegated result
     */
    String showSecretQR(String username, String issuer, String secret);

    /**
     * testRadiusProxyLogin through the reviewed RPC contract.
     * @param userName The userName argument
     * @param userPass The userPass argument
     * @param userDomain The userDomain argument
     * @return The delegated result
     */
    String testRadiusProxyLogin(String userName, String userPass, String userDomain);
}

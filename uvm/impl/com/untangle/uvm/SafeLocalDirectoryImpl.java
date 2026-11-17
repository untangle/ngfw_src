/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeLocalDirectoryImpl implements SafeLocalDirectory
{
    private final Supplier<LocalDirectory> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeLocalDirectoryImpl(Supplier<LocalDirectory> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * addRadiusComputerAccount through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public ExecManagerResult addRadiusComputerAccount()
    {
        return delegate.get().addRadiusComputerAccount();
    }

    /**
     * generateSecret through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String generateSecret()
    {
        return ((LocalDirectoryImpl) delegate.get()).generateSecret();
    }

    /**
     * getRadiusLogFile through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getRadiusLogFile()
    {
        return delegate.get().getRadiusLogFile();
    }

    /**
     * getRadiusProxyStatus through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getRadiusProxyStatus()
    {
        return delegate.get().getRadiusProxyStatus();
    }

    /**
     * getUsers through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public LinkedList<LocalDirectoryUser> getUsers()
    {
        return delegate.get().getUsers();
    }

    /**
     * notifyRadiusUserLogin through the reviewed RPC contract.
     * @param userName The userName argument
     * @param macAddress The macAddress argument
     */
    @Override
    public void notifyRadiusUserLogin(String userName, String macAddress)
    {
        delegate.get().notifyRadiusUserLogin(userName, macAddress);
    }

    /**
     * setUsers through the reviewed RPC contract.
     * @param users The users argument
     */
    @Override
    public void setUsers(LinkedList<LocalDirectoryUser> users)
    {
        delegate.get().setUsers(users);
    }

    /**
     * showSecretQR through the reviewed RPC contract.
     * @param username The username argument
     * @param issuer The issuer argument
     * @param secret The secret argument
     * @return The delegated result
     */
    @Override
    public String showSecretQR(String username, String issuer, String secret)
    {
        return ((LocalDirectoryImpl) delegate.get()).showSecretQR(username, issuer, secret);
    }

    /**
     * testRadiusProxyLogin through the reviewed RPC contract.
     * @param userName The userName argument
     * @param userPass The userPass argument
     * @param userDomain The userDomain argument
     * @return The delegated result
     */
    @Override
    public String testRadiusProxyLogin(String userName, String userPass, String userDomain)
    {
        return delegate.get().testRadiusProxyLogin(userName, userPass, userDomain);
    }
}

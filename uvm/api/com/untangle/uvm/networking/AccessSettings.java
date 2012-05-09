/**
 * $Id$
 */
package com.untangle.uvm.networking;

import java.io.Serializable;



import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.Validatable;
import com.untangle.uvm.node.ValidateException;

/**
 * These are settings related to limitting and granting remote access
 * to the untangle.
 */
@SuppressWarnings("serial")
public class AccessSettings implements Serializable, Validatable
{
    /* boolean which can be used by the untangle to determine if the
     * object returned by a user interface has been modified. */
    private boolean isClean = false;

    private Long id;

    /** True iff remote untangle support is enabled */
    private boolean isSupportEnabled;

    /* True iff internal HTTP access is enabled */
    private boolean isInsideInsecureEnabled;

    /* This is the port that blockpages are rendered on. */
    private int blockPagePort = 80;

    /* True iff Access to the external HTTPs port is allowed */
    private boolean isOutsideAccessEnabled;

    /* True iff access to the external HTTPs port is restriced with
     * <code>outsideNetwork / outsideNetmask</code>. */
    private boolean isOutsideAccessRestricted;

    /* When <code>isOutsideAccessRestricted</code> is true this is the
     * network that access is restricted to. */
    private IPAddress outsideNetwork;

    /* When <code>isOutsideAccessRestricted</code> is true this is the
     * netmask that access is restricted to. */
    private IPAddress outsideNetmask;

    /* True iff administration is allowed from outside. */
    private boolean isOutsideAdministrationEnabled;

    /* True iff quarantine is allowed from outside. */
    private boolean isOutsideQuarantineEnabled;

    /* True iff reporting is allowed from outside. */
    private boolean isOutsideReportingEnabled;

    public AccessSettings()
    {
        this.isClean = false;
    }

    public Long getId() { return id; }
    public void setId(  Long id  ) { this.id = id; }

    /**
     * Get whether or not remote untangle support is enabled.
     */
    public boolean getIsSupportEnabled()
    {
        return this.isSupportEnabled;
    }


    /**
     * Set whether or not remote untangle support is enabled.
     */
    public void setIsSupportEnabled( boolean newValue )
    {
        if ( newValue != this.isSupportEnabled ) this.isClean = false;
        this.isSupportEnabled = newValue;
    }

    /**
     * Get whether or not local insecure access is enabled.
     */
    public boolean getIsInsideInsecureEnabled()
    {
        return this.isInsideInsecureEnabled;
    }

    /**
     * Set whether or not local insecure access is enabled.
     */
    public void setIsInsideInsecureEnabled( boolean newValue )
    {
        if ( newValue != this.isInsideInsecureEnabled ) this.isClean = false;
        this.isInsideInsecureEnabled = newValue;
    }

    /**
     * Get whether or not external remote access is enabled.  This is
     * is no longer used, as access to the external https port is
     * automatically opened whenever a service that requires it is
     * enabled.
     *
     * @return true iff external access is enabled.
     */
    public boolean getIsOutsideAccessEnabled()
    {
        return this.isOutsideAccessEnabled;
    }

    /**
     * Set external remote access
     * 
     * @param True iff external access is allowed
     */
    public void setIsOutsideAccessEnabled( boolean newValue )
    {
        if ( newValue != this.isOutsideAccessEnabled ) this.isClean = false;
        this.isOutsideAccessEnabled = newValue;
    }

    /**
     * Retrieve whether or not outside access is restricted.
     *
     * @return True iff outside access is restricted.
     */
    public boolean getIsOutsideAccessRestricted()
    {
        return this.isOutsideAccessRestricted;
    }

    /**
     * Set whether or not outside access is restricted.
     *
     * @param newValue True iff outside access is restricted.
     */
    public void setIsOutsideAccessRestricted( boolean newValue )
    {
        if ( newValue != this.isOutsideAccessRestricted ) this.isClean = false;
        this.isOutsideAccessRestricted = newValue;
    }

    /**
     * The netmask of the network/host that is allowed to administer
     * the box from outside.  This is ignored if restrict outside
     * access is not enabled.
     *
     * @return The network that is allowed to administer the box from
     * the internet.
     */
    public IPAddress getOutsideNetwork()
    {
        if ( this.outsideNetwork == null ) this.outsideNetwork = NetworkUtil.DEF_OUTSIDE_NETWORK;
        return this.outsideNetwork;
    }

    /**
     * Set the network of the network/host that is allowed to
     * administer the box from outside.  This is ignored if restrict
     * outside access is not enabled.
     *
     * @param newValue The network that is is allowed to administer the box from
     * the internet.
     */
    public void setOutsideNetwork( IPAddress newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.DEF_OUTSIDE_NETWORK;
        if ( !IPAddress.equals( this.outsideNetwork, newValue )) this.isClean = false;
        this.outsideNetwork = newValue;
    }

    /**
     * The netmask of the network/host that is allowed to administer
     * the box from outside.  This is ignored if restrict outside
     * access is not enabled.
     *
     * @return The netmask that is allowed to administer the box from
     * the internet.
     */
    public IPAddress getOutsideNetmask()
    {
        if ( this.outsideNetmask == null ) this.outsideNetmask = NetworkUtil.DEF_OUTSIDE_NETMASK;
        return this.outsideNetmask;
    }

    /**
     * Set the netmask of the network/host that is allowed to
     * administer the box from outside.  This is ignored if restrict
     * outside access is not enabled.
     *
     * @param newValue The netmask for the network that is is allowed to administer the box from
     * the internet.
     */
    public void setOutsideNetmask( IPAddress newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.DEF_OUTSIDE_NETMASK;
        if ( !IPAddress.equals( this.outsideNetmask, newValue )) this.isClean = false;
        this.outsideNetmask = newValue;
    }

    /**
     * Retrieve whether or not administration from the internet is allowed.
     *
     * @return True iff able to administer from the internet.
     */
    public boolean getIsOutsideAdministrationEnabled()
    {
        return this.isOutsideAdministrationEnabled;
    }

    /**
     * Set whether or not administration from the internet is allowed.
     *
     * @param newValue True iff able to administer from the internet.
     */
    public void setIsOutsideAdministrationEnabled( boolean newValue )
    {
        if ( newValue != this.isOutsideAdministrationEnabled ) this.isClean = false;
        this.isOutsideAdministrationEnabled = newValue;
    }

    /**
     * Retrieve whether or not to access the user quarantine from the
     * internet is allowed.
     *
     * @return True iff able to access user quarantines from the
     * internet.
     */
    public boolean getIsOutsideQuarantineEnabled()
    {
        return this.isOutsideQuarantineEnabled;
    }

    /**
     * Set whether or not to access the user quarantine from the
     * internet is allowed.
     *
     * @param newValue True iff able to access user quarantines from the
     * internet.
     */
    public void setIsOutsideQuarantineEnabled( boolean newValue )
    {
        if ( newValue != this.isOutsideQuarantineEnabled ) this.isClean = false;
        this.isOutsideQuarantineEnabled = newValue;
    }

    /**
     * Retrieve whether access is allowed to reports from the internet.
     *
     * @return True iff able to access reports from the internet.
     */
    public boolean getIsOutsideReportingEnabled()
    {
        return this.isOutsideReportingEnabled;
    }

    /**
     * Set whether access is allowed to reports from the internet.
     *
     * @param newValue True iff able to access reports from the
     * internet.
     */
    public void setIsOutsideReportingEnabled( boolean newValue )
    {
        if ( newValue != this.isOutsideReportingEnabled ) this.isClean = false;
        this.isOutsideReportingEnabled = newValue;
    }

    /**
     * Return true iff the settings haven't been modified since the
     * last time <code>isClean( true )</code> was called.
     */
    public boolean isClean()
    {
        return this.isClean;
    }

    /**
     * Clear or set the isClean flag.
     *
     * @param newValue The new value for the isClean flag.
     */
    public void isClean( boolean newValue )
    {
        this.isClean = newValue;
    }

    /**
     * Validate that the settings are free of errors.
     */
    public void validate() throws ValidateException
    {
        /* nothing appears to be necessary here for now */
    }

    public int getBlockPagePort()
    {
        return 80;
    }
}

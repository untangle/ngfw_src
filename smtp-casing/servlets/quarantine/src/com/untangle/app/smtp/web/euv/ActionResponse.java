/**
 * 
 */
package com.untangle.app.smtp.web.euv;

/**
 * Perform actions on quarantine inbox and safelist.
 */
public class ActionResponse
{
    static final ActionResponse EMPTY = new ActionResponse( -1, null );

    /* The new number of total records in this inbox (or -1 if the inbox wasn't affected) */
    private final int totalRecords;

    /* The new safelist (or null if the safelist wasn't affected) */
    private final String[] safelist;

    private int purgeCount = 0;
    private int releaseCount = 0;
    /* Number of addresses safeslisted (positive) or removed from the safelist (negative) */
    private int safelistCount = 0;    
    
    /**
     * Initialize inbox records and safelist.
     *
     * @param totalRecords  New count of total records in the inbox.
     * @param safelist New array of email addresses in safelist.
     */
    ActionResponse( int totalRecords, String[] safelist )
    {
        this.totalRecords = totalRecords;
        this.safelist = safelist;
    }

    /**
     * Return number of inbox messages.
     * 
     * @return number of inbox messages.
     */
    public int getTotalRecords()
    {
        return this.totalRecords;
    }

    /**
     * Return safelist.
     * 
     * @return Array of safelist addresses.
     */
    public String[] getSafelist()
    {
        return this.safelist;
    }

    /**
     * Return number of messages purged.
     * 
     * @return Number of messages purged.
     */
    public int getPurgeCount()
    {
        return this.purgeCount;
    }

    /**
     * Set the number of messages purged.
     * 
     * @param newValue New count of messages purged.
     */
    public void setPurgeCount( int newValue )
    {
        this.purgeCount = newValue;
    }

    /**
     * Return number of messages released.
     *
     * @return Number of messages released.
     */
    public int getReleaseCount()
    {
        return this.releaseCount;
    }

    /**
     * Set number of messages released.
     *
     * @param newValue New count of messages released.
     */
    public void setReleaseCount( int newValue )
    {
        this.releaseCount = newValue;
    }

    /**
     * Return number of addresses in safelist.
     * 
     * @return Number of addresses in safelist.
     */
    public int getSafelistCount()
    {
        return this.safelistCount;
    }

    /**
     * Set the number of addresses in the safelist.
     *
     * @param newValue New count of addresses in safelist.
     */
    public void setSafelistCount( int newValue )
    {
        this.safelistCount = newValue;
    }
}

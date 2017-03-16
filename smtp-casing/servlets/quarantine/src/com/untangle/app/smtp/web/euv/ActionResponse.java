
package com.untangle.app.smtp.web.euv;

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
    
    ActionResponse( int totalRecords, String[] safelist )
    {
        this.totalRecords = totalRecords;
        this.safelist = safelist;
    }

    public int getTotalRecords()
    {
        return this.totalRecords;
    }

    public String[] getSafelist()
    {
        return this.safelist;
    }

    public int getPurgeCount()
    {
        return this.purgeCount;
    }

    public void setPurgeCount( int newValue )
    {
        this.purgeCount = newValue;
    }

    public int getReleaseCount()
    {
        return this.releaseCount;
    }

    public void setReleaseCount( int newValue )
    {
        this.releaseCount = newValue;
    }

    public int getSafelistCount()
    {
        return this.safelistCount;
    }

    public void setSafelistCount( int newValue )
    {
        this.safelistCount = newValue;
    }
}

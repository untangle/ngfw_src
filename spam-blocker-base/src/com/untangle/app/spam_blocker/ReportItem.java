/**
 * $Id$
 */
package com.untangle.app.spam_blocker;

public class ReportItem
{
    private final float score;
    private final String category;

    // constructors -----------------------------------------------------------

    public ReportItem(float score, String category)
    {
        this.score = score;
        this.category = category;
    }

    // accessors --------------------------------------------------------------

    public float getScore()
    {
        return score;
    }

    public String getCategory()
    {
        return category;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return score + " " + category;
    }
}

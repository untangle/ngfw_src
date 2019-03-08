/**
 * $Id$
 */
package com.untangle.app.spam_blocker;

/**
 * Spam report item implementation
 */
public class ReportItem
{
    private final float score;
    private final String category;

    /**
     * Constructor
     * 
     * @param score
     *        The score
     * @param category
     *        The category
     */
    public ReportItem(float score, String category)
    {
        this.score = score;
        this.category = category;
    }

    /**
     * Get the score
     * 
     * @return The score
     */

    public float getScore()
    {
        return score;
    }

    /**
     * Get the category
     * 
     * @return The category
     */
    public String getCategory()
    {
        return category;
    }

    /**
     * Get the string representation
     * 
     * @return The string representation
     */
    public String toString()
    {
        return score + " " + category;
    }
}

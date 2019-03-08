/**
 * $Id: FakeDataCrumb.java 37267 2014-02-26 23:42:19Z dmorris $
 */
package com.untangle.jvector;

/**
 * A "fake" data crumb is a data crumb that actually contains no data.
 * It is used when we detect that a file descriptor backed source is directly relayed
 * to a file descriptor sink. In this case, instead of reading from the source fd, we create
 * a FakeDataCrumb and send it down the pipeline. When the sink gets the FakeDataCrumb
 * it will use splice to copy the data from the source fd to the sink fd in kernel space.
 *
 * This is just an empty crumb so we have something to pass down the pipeline
 */
public class FakeDataCrumb extends Crumb
{
    private final Source source;

    /**
     * FakeDataCrumb
     * @param source
     */
    public FakeDataCrumb( Source source )
    {
        this.source = source;
    }

    /**
     * getSource
     * @return the source
     */
    public Source getSource() { return this.source; }

    /**
     * type
     * @return the type
     */
    public int    type() { return TYPE_DATA; }

    /**
     * raze
     */
    public void   raze() { }
}

/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: TestSettings.java,v 1.6 2005/03/16 03:43:13 rbscott Exp $
 */

package com.metavize.tran.test;

import java.io.Serializable;

import com.metavize.mvvm.security.Tid;

/**
 * Settings for the Test Transform.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_TEST_SETTINGS"
 */
public class TestSettings implements Serializable
{
    private static final long serialVersionUID = 4143567998376955882L;

    private Long id;
    private Tid tid;
    private Mode mode = Mode.NORMAL;
    // private boolean doubleEnded = true;
    private boolean buffered = true;
    private boolean normal = true;
    private boolean release = true;
    private boolean quiet = false;
    private boolean randomBufferSizes = false;
    private int minRandomBufferSize = 63;
    private int maxRandomBufferSize = 16500;

    public TestSettings () { }

    public TestSettings( Tid tid) 
    { 
	this.tid = tid;
    }

    public void resetSettings()
    {
        release = false;
        normal = false;
        // doubleEnded = false;
        buffered = false;
    }

    /**
     * @hibernate.id
     * column="ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Transform id for these settings.
     *
     * @return tid for these settings
     * @hibernate.many-to-one
     * column="TID"
     * unique="true"
     * not-null="true"
     */
    public Tid getTid()
    {
        return tid;
    }

    public void setTid(Tid tid)
    {
        this.tid = tid;
    }

    /**
     * Mode to operate in.
     *
     * @return mode.
     * @hibernate.property
     * type="com.metavize.tran.test.ModeUserType"
     * column="MODE"
     */
    public Mode getMode()
    {
        return mode;
    }

    public void setMode(Mode mode)
    {
        this.mode = mode;
    }

    /**
     * Buffered?
     *
     * @return true if buffered.
     * @hibernate.property
     * column="BUFFERED"
     */
    public boolean isBuffered()
    {
        return buffered;
    }

   public void setBuffered(boolean buffered)
    {
        this.buffered = buffered;
    }

    /**
     * Normal?
     *
     * @return a <code>boolean</code> value
     * @hibernate.property
     * column="NORMAL"
     */
    public boolean isNormal()
    {
        return normal;
    }

    public void setNormal(boolean normal)
    {
        this.normal = normal;
    }

    /**
     * Release sessions, true by default.
     *
     * @return true if sessions are released.
     * @hibernate.property
     * column="RELEASE"
     */
    public boolean getRelease()
    {
        return release;
    }

    public void setRelease(boolean release)
    {
        this.release = release;
    }

    /**
     * By default, logs lots of info, quiet suppresses this.
     *
     * @return true
     * @hibernate.property
     * column="QUIET"
     */
    public boolean isQuiet()
    {
        return quiet;
    }

    public void setQuiet(boolean quiet)
    {
        this.quiet = quiet;
    }

    /**
     * Use random buffer sizes, false by default.
     *
     * @return true for random buffer sizes.
     */
    public boolean getRandomBufferSizes()
    {
        return randomBufferSizes;
    }

    public void setRandomBufferSizes(boolean randomBufferSizes)
    {
        this.randomBufferSizes = randomBufferSizes;
    }

    /**
     * Minimum buffer size when using random buffer sizes. Default is
     * 63.
     *
     * @return minimum buffer size.
     * @hibernate.property
     * column="MIN_RANDOM_BUFFER_SIZE"
     */
    public int getMinRandomBufferSize()
    {
        return minRandomBufferSize;
    }

    public void setMinRandomBufferSize(int minRandomBufferSize)
    {
        this.minRandomBufferSize = minRandomBufferSize;
    }

    /**
     * Maximum buffer size when using random buffer sizes. Default is
     * 16500;
     *
     * @return maximum buffer size.
     * @hibernate.property
     * column="MAX_RANDOM_BUFFER_SIZE"
     */
    public int getMaxRandomBufferSize()
    {
        return maxRandomBufferSize;
    }

    public void setMaxRandomBufferSize(int maxRandomBufferSize)
    {
        this.maxRandomBufferSize = maxRandomBufferSize;
    }
}

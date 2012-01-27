/**
 * $Id$
 */
package com.untangle.uvm.util;

import org.apache.log4j.Logger;

public class Pulse
{
    /* things shouldn't be firing that fast, must be a bug */
    private static final long DELAY_MINIMUM = 5000;

    /* wait at most a second for the beat to execute */
    private static final long BEAT_MAX_WAIT = 1000;

    private final Logger logger = Logger.getLogger(getClass());

    private final Thread thread;
    private final Runnable blip;
    private final Ticker ticker;

    public enum PulseState 
    {
        /* Pulse thread has never started running */
        UNBORN,

        /* Pulse thread is actively running */
        STARTING,

        /* Pulse thread is actively running */
        RUNNING,

        /* Pulse thread has been killed, but for some reason hasn't died yet */
        KILLED,

        /* Thread is totally dead and cannot be resurrected */
        DEAD
    };

    private PulseState state = PulseState.UNBORN;

    /* amount of time to wait until the next beat */
    private long delay;

    /**
     * Create a new pulse with the default name and isDaemon setting.
     */
    public Pulse( Runnable blip )
    {
        this( null, null, blip );
    }

    /**
     * Create a new pulse with the default name and set whether it is
     * a daemon thread.
     */
    public Pulse( boolean isDaemon, Runnable blip )
    {
        this( null, isDaemon, blip );
    }

    /**
     * Create a new pulse and set its name, default isDaemon.
     */
    public Pulse( String name, Runnable blip )
    {
        this( name, null, blip );
    }

    /**
     * Create a new pulse, optionally setting the name and isDaemon
     * setting.
     */
    public Pulse( String name, Boolean isDaemon, Runnable blip )
    {
        this.blip = blip;
        this.ticker = new Ticker();
        /* Check if the caller wants to set the thread name */
        if (( null == name ) || ( 0 == name.length())) {
            /* No name */
            this.thread = new Thread( this.ticker );
        } else {
            /* Set the thread name */
            this.thread = new Thread( this.ticker, name );
        }

        if ( null != isDaemon ) this.thread.setDaemon( isDaemon );
    }

    /**
     * Start the thread, you can only start a pulse once and once it
     * is stopped, you can never restart it.
     */
    public synchronized void start( long delay )
    {
        /* Can't start unless it is in the unborn state */
        if ( PulseState.UNBORN != this.state ) {
            throw new IllegalStateException( "Unable to start a pulse twice" );
        }

        this.delay = Math.max( delay, DELAY_MINIMUM );

        /* Indicate that the thread is now starting */
        this.state = PulseState.STARTING;

        /* Start a thread */
        this.thread.start();
    }

    /**
     * Stop the thread, you can only start a pulse once and once it
     * is stopped, you can never restart it.
     */
    public synchronized void stop()
    {
        switch ( this.state ) {
        case UNBORN:
            logger.warn( "Attempt to stop an unborn pulse." );
            return;

        case DEAD:
            logger.warn( "Attempt to stop a dead pulse." );
            return;

        case STARTING: /* unlikely but possible */
        case KILLED:
        case RUNNING:
            logger.debug( "Stopping the pulse." );
            this.state = PulseState.KILLED;
            /* Interrupt the thread. */
            this.thread.interrupt();
            return;
        }
    }

    /**
     * Retrieve the delay between tasks
     */
    public long getDelay()
    {
        return this.delay;
    }

    /**
     * Change the delay, this also causes it to run right now.
     */
    public synchronized void setDelay( long delay )
    {
        switch ( this.state ) {
        case UNBORN:
            logger.warn( "Attempt to set delay on an unborn pulse." );
            return;

        case DEAD:
            logger.warn( "Attempt to set delay on a dead pulse." );
            return;

        case KILLED:
            logger.warn( "Attempt to set delay on a dying pulse." );
            return;

        case STARTING: /* STARTING is unlikely but possible */
            logger.debug( "Updating the delay to " + delay );
            this.delay = Math.max( delay, DELAY_MINIMUM );
            break;

        case RUNNING:
            logger.debug( "Updating the delay to " + delay );
            this.delay = Math.max( delay, DELAY_MINIMUM );
            synchronized ( this.ticker ) {
                /* Set the trigger to zero to force it to run */
                this.ticker.setNextTrigger( 0 );
                this.ticker.notifyAll();
            }

            break;
        }
    }

    /**
     * Run the ticker now, and wait until it finishes.
     */
    public boolean beat( long maxWait )
    {
        long count = this.ticker.getCount();

        setDelay( this.delay );

        /* has ticked since beat was called. */
        if ( count != this.ticker.getCount()) return true;

        synchronized ( this.ticker ) {
            /* has ticked since beat was called. */
            if ( count != this.ticker.getCount()) return true;

            try {
                this.ticker.wait( maxWait );
            } catch ( InterruptedException e ) {
                logger.debug( "interrupted while waiting for the ticker", e );
            }
        }

        /* If the count changed, then this waited for one tick to complete */
        return ( count == this.ticker.getCount());
    }

    /**
     * Beat with the default max wait
     */
    public boolean beat()
    {
        return beat( BEAT_MAX_WAIT );
    }

    /**
     * Retrieve the current state of the pulse
     */
    public synchronized PulseState getState()
    {
        return this.state;
    }

    private synchronized void setState( PulseState state )
    {
        this.state = state;
    }

    private class Ticker implements Runnable
    {
        /* the number of times the beat has occurred */
        private long count = 0;

        /* This is the next time that you should wake up */
        private long nextTrigger = Long.MIN_VALUE;

        private Ticker()
        {}      

        public void run()
        {
            synchronized( Pulse.this ) {
                if ( PulseState.STARTING != getState()) {
                    Pulse.this.logger.warn( "Unable to start the ticker thread outside of running state" );
                    return;
                }

                setState( PulseState.RUNNING );
            }

            while ( PulseState.RUNNING == getState()) {
                /* run the blip */
                try {
                    blip.run();
                } catch ( Exception e ) {
                    logger.warn( "error running blip", e );
                }

                synchronized ( this ) {
                    /* Increment the number of counts */
                    this.count++;

                    /* Notify anyone waiting */
                    notifyAll();
                }

                try {
                    waitForNextBlip();
                } catch ( Exception e ) {
                    logger.info( "Exception waiting for next blip: ", e );
                }
            }

            logger.debug( "pulse is stopping" );
            setState( PulseState.DEAD );
        }

        /**
         * Wait until it is time to trigger the next blip.
         */
        private void waitForNextBlip()
        {
            /* used to gauge if there is funny stuff going on with the clock */
            long waitStart = ( System.nanoTime() / 1000000l );

            /* amount of time to wait */
            long delay = getDelay();

            /* the next time you should wake up assuming everything is normal */
            if ( delay < DELAY_MINIMUM ) {
                /* never wake up */
                setNextTrigger( Long.MIN_VALUE );
                /* doesn't matter, just some time that will not happen soon */
                delay = 60 * 60 * 1000;
            } else {
                /* set the time of the next trigger */
                /* add a tenth of a second little buffer */
                setNextTrigger( waitStart + delay - 100 );
            }

            while ( true ) {
                try {
                    if ( delay <= 0 ) {
                        logger.info( "Delay(" + delay + ") <= 0, firing immediately." );
                        break;
                    }

                    synchronized( this ) {
                        wait( delay );
                    }
                } catch ( InterruptedException e ) {
                    logger.debug( "interrupted while waiting for task to complete." );
                }

                long nextTrigger = getNextTrigger();

                /* isn't running anymore, time to stop */
                if ( PulseState.RUNNING != getState()) break;

                if ( nextTrigger == 0 ) {
                    logger.debug( "instructed to execute immediately." );
                    break;
                } else if ( nextTrigger == Long.MIN_VALUE ) {
                    logger.debug( "instructed to never execute, continuing." );
                    continue;
                }
                long now = ( System.nanoTime() / 1000000l );

                if ( now < waitStart ) {
                    /* time has gone backwards, just run just in case */
                    logger.debug( "time has gone backwards, executing immediately(" + now + "," + waitStart + ")" );
                    break;
                }

                /* Time to run. */
                if ( now > nextTrigger ) {
                    break;
                }

                /* Decrease the delay */
                delay = now - nextTrigger;
            }
        }

        /**
         * Return the next time the trigger is supposed to go off
         */
        private synchronized long getNextTrigger()
        {
            return this.nextTrigger;
        }

        private synchronized void setNextTrigger( long nextTrigger )
        {
            this.nextTrigger = nextTrigger;
        }

        private long getCount()
        {
            return this.count;
        }
    }
}

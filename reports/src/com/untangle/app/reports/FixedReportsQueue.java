/**
 * $Id: FixedReportsQueue.java 38792 2014-10-09 19:49:00Z cblaise $
 */
package com.untangle.app.reports;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;

import com.untangle.uvm.util.Pulse;


/**
 * Process fixed report queue, preventing uvm from being blocked an preventing potential
 * resource constraings due to multiple queue runs.
 */
class FixedReportsQueue
{
    public static final long SLEEP_TIME_MSEC = 10 * 1000;

    protected static final Logger logger = Logger.getLogger( FixedReportsQueue.class );

    private final ReportsApp app;

    /* The thread the monitor is running on */
    private Thread thread = null;

    /* Status of the monitor */
    private volatile boolean isAlive = true;

    /**
     * Pulse thread to re-read the AD into cache
     */
    private Pulse pulseRunQueue = new Pulse("run-fixed-reports-queue", new RunQueue(), SLEEP_TIME_MSEC, true);

    /**
     * Report queue instance.
     */
    class reportQueue
    {
        Integer templateId;
        long startTimestamp;
        long stopTimestamp;

        /**
         * Initialize report queue entry.
         * @param  templateId     Integer of report template id.
         * @param  startTimestamp Long beginning timestamp.
         * @param  stopTimestamp  Long ending timestamp.
         * @return                Innstance of reportQueue
         */
        public reportQueue(Integer templateId, long startTimestamp, long stopTimestamp)
        {
            this.templateId = templateId;
            this.startTimestamp = startTimestamp;
            this.stopTimestamp = stopTimestamp;
        }

    }
    private BlockingQueue<reportQueue> queue;

    /**
     * Initialize event monitor.
     *
     * @param app
     *  Intrusion Prevention application.
     */
    protected FixedReportsQueue( ReportsApp app )
    {
        this.app = app;
        this.queue = new LinkedBlockingQueue<>();
    }

    /**
     * Add to the report queue.
     *
     * @param templateId     Report template id.
     * @param startTimestamp Beginning timestamp.
     * @param stopTimestamp  Ending timestamp.
     */
    public synchronized void add(Integer templateId, long startTimestamp, long stopTimestamp){
        queue.add( new reportQueue(templateId, startTimestamp, stopTimestamp));
    }

    /**
     * Get the size of the queue.
     * @return Integer of queue size.
     */
    public synchronized int size(){
        return queue.size();
    }

    /**
     * Start the process.  If already running, force a new run.
     */
    public synchronized void start()
    {
        this.pulseRunQueue.start();
    }

    /**
     * Stop the process.
     */
    public synchronized void stop()
    {
        this.pulseRunQueue.stop();
    }

    /**
     * Extract from the queue and process as a new thrad.
     */
    private class RunQueue implements Runnable {

        /**
         * Loop looking for new files and/or last file size change.
         */
        public void run()
        {
            logger.debug( "Starting" );

            if ( !isAlive ) {
                logger.error( "died before starting" );
                return;
            }

            FixedReports fixedReports = null;
            reportQueue rq = null;

            while ( true ) {
                /* Check if the app is still running */
                if ( !isAlive )
                    break;

                if(queue.size() > 0 ){
                    if(fixedReports == null){
                        fixedReports = new FixedReports();
                    }
                    synchronized( this ) {
                        try {
                            rq = queue.take();
                        } catch (Exception e) {
                            logger.warn("Failed to run report queue.", e);
                            try {Thread.sleep(1000);} catch (Exception exc) {}
                        } 
                    }
                    runTest( fixedReports, rq );
                }

                if(queue.size() == 0 && fixedReports != null){
                    fixedReports.destroy();
                    fixedReports = null;
                }

                /* sleep */
                try {
                    Thread.sleep( SLEEP_TIME_MSEC );
                } catch ( InterruptedException e ) {
                    logger.info( "fied report queue runner was interrupted" );
                }

                /* Check if the app is still running */
                if ( !isAlive )
                    break;
            }
            if(fixedReports != null){
                fixedReports.destroy();
                fixedReports = null;
            }

            logger.debug( "Finished" );
        }

        /**
         * Run fixedReports to generate a report based on the reportqueue information.
         * @param fixedReports FixedReports instance.
         * @param report       reportQueuee to process.
         */
        private void runTest(FixedReports fixedReports, reportQueue report){
            String url = "https://" + UvmContextFactory.context().networkManager().getPublicUrl() + "/reports/";
            synchronized (this) {
                for( EmailTemplate emailTemplate : app.getSettings().getEmailTemplates() ){
                    if(!report.templateId.equals(emailTemplate.getTemplateId())){
                        continue;
                    }
                    List<ReportsUser> users = new LinkedList<>();
                    for ( ReportsUser user : app.getSettings().getReportsUsers() ) {
                        if( user.getEmailSummaries() && user.getEmailTemplateIds().contains(emailTemplate.getTemplateId()) ){
                            users.add(user);
                        }
                    }
                    if( users.size() > 0){
                        fixedReports.generate(emailTemplate, users, url, ReportsManagerImpl.getInstance(), report.startTimestamp, report.stopTimestamp);
                    } else {
                        logger.warn("Skipping report " + emailTemplate.getTitle() + " because no users (emails) receive it.");
                    }
                }
            }
        }
    }
}

/**
 * $Id$
 */
package com.untangle.node.spam_blocker;

import static com.untangle.uvm.util.Ascii.CRLF;

import java.util.LinkedList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.TemplateValues;

/**
 * Class to encapsulate a SPAM report. <br>
 * <br>
 * This class also implements {@link com.untangle.uvm.util.TemplateValues TemplateValues}. Valid key names which can be
 * derefferenced from a SpamReport begin with the literal <code>SPAMReport:</code> followed by any of the following
 * tokens:
 * <ul>
 * <li>
 * <code><b>FULL</b></code>Any Report items from the SPAM report, with each report on its own line. Each report item is
 * terminated by a CRLF.</li>
 * <li>
 * <code><b>THRESHOLD</b></code> The numerical value of the threshold above-which a score renders a SPAM judgement (e.g.
 * "5").</li>
 * <li>
 * <code><b>SCORE</b></code> The numerical value of the score (e.g. "7.2").</li>
 * </ul>
 */
public class SpamReport implements TemplateValues
{
    private static final String SPAM_REPORT_PREFIX = "SPAMReport:".toLowerCase();
    private static final String FULL_KEY = "FULL".toLowerCase();
    private static final String THRESHOLD_KEY = "THRESHOLD".toLowerCase();
    private static final String SCORE_KEY = "SCORE".toLowerCase();

    private final List<ReportItem> items;
    private final float score;
    private final float threshold;

    public static final float MAX_THRESHOLD = 1000f;
    public static final float MIN_THRESHOLD = 0f;

    public static final SpamReport EMPTY = new SpamReport(new LinkedList<ReportItem>(), MAX_THRESHOLD);

    private Logger logger = Logger.getLogger(SpamReport.class);

    // constructors -----------------------------------------------------------

    public SpamReport(List<ReportItem> items, float score, float threshold)
    {
        this.items = new LinkedList<ReportItem>(items);
        this.score = score;
        this.threshold = threshold;
    }

    public SpamReport(List<ReportItem> items, float threshold)
    {
        this.items = new LinkedList<ReportItem>(items);
        this.threshold = threshold;

        float s = 0;
        for (ReportItem ri : items) {
            s += ri.getScore();
        }
        this.score = s;
    }

    /**
     * For use in Templates (see JavaDoc at the top of this class for explanation of variable format).
     */
    public String getTemplateValue(String key)
    {
        key = key.trim().toLowerCase();
        if (key.startsWith(SPAM_REPORT_PREFIX)) {
            key = key.substring(SPAM_REPORT_PREFIX.length());
            if (key.equals(FULL_KEY)) {
                StringBuilder sb = new StringBuilder();
                for (ReportItem ri : items) {
                    sb.append('(');
                    sb.append(Float.toString(ri.getScore()));
                    sb.append(") ");
                    sb.append(ri.getCategory());
                    sb.append(CRLF);
                }
                return sb.toString();
            } else if (key.equals(SCORE_KEY)) {
                return Float.toString(score);
            } else if (key.equals(THRESHOLD_KEY)) {
                return Float.toString(threshold);
            }
        }
        return null;
    }

    public boolean isSpam()
    {
        return threshold <= score;
    }

    public float getScore()
    {
        return score;
    }

    public void addHeaders(MimeMessage msg)
    {
        StringBuilder sb = new StringBuilder();

        if (isSpam()) {
            sb.append("Yes, ");
        } else {
            sb.append("No, ");
        }

        sb.append("score=");
        sb.append(String.format("%.1f", score));
        sb.append(" required=");
        sb.append(String.format("%.1f", threshold));

        sb.append(" tests=");

        boolean first = true;
        for (ReportItem item : items) {
            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }
            sb.append(item.getCategory());
        }

        // MIMEMessageHeaders mmh = msg.getMMHeaders();

        try {
            msg.removeHeader("X-spam-status");
            msg.addHeader("X-spam-status", sb.toString());
        } catch (MessagingException exn) {
            logger.warn("could not add header: " + sb.toString(), exn);
        }
    }

    // accessors --------------------------------------------------------------

    public List<ReportItem> getItems()
    {
        return items;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        StringBuffer sb = new StringBuffer("Spam Score: ");
        sb.append(score);
        sb.append("\n");

        for (ReportItem i : items) {
            sb.append("  ");
            sb.append(i);
            sb.append("\n");
        }

        return sb.toString();
    }
}

/**
 * $Id$
 */
package com.untangle.app.bandwidth_control;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTableEntry;
import com.untangle.uvm.HookManager;
import com.untangle.uvm.Tag;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.AppSession;


/**
 * This object represents the "action" of a BandwidthControlRule
 * There are several types of actions:
 *   SET_PRIORITY
 *   TAG_HOST
 *   GIVE_HOST_QUOTA
 *   GIVE_USER_QUOTA
 */
@SuppressWarnings("serial")
public class BandwidthControlRuleAction implements JSONString, Serializable
{
    public static final int END_OF_HOUR = -1;
    public static final int END_OF_DAY  = -2; 
    public static final int END_OF_WEEK = -3;
    public static final int END_OF_MONTH = -4;
    
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * The action type enumeration
     */
    public enum ActionType {
        SET_PRIORITY, /* priority 1-7 */
        TAG_HOST, /* tagName tagTimeSec */
        GIVE_HOST_QUOTA, /* bytes #, time_sec */
        GIVE_USER_QUOTA, /* bytes #, time_sec */

        APPLY_PENALTY_PRIORITY, /* DEPRECATED */
        GIVE_CLIENT_HOST_QUOTA, /* DEPRECATED - 13.0 */
        PENALTY_BOX_CLIENT_HOST, /* DEPRECATED - 13.0 priority 1-7, time_sec */
    }

    private ActionType action = null;
    private Integer priority = null;
    private Integer quotaTimeSec = null;
    private Long quotaBytes = null;
    private String tagName = null;
    private Integer tagTimeSec = null;
        
    private BandwidthControlRule rule = null; /* the rule that owns this action */
    
    /**
     * This reference is held to that advanced actions can be applied
     */
    private BandwidthControlApp app;
    
    public BandwidthControlRuleAction()
    {
    }

    public BandwidthControlRuleAction(ActionType action, BandwidthControlApp app, Integer priority)
    {
        this(action,app,priority,0);
    }

    public BandwidthControlRuleAction(ActionType action, BandwidthControlApp app, Integer priority, Integer penaltyTimeSec)
    {
        this.setApp(app);
        this.setActionType(action);
        this.setPriority(priority);
        this.setPenaltyTime(penaltyTimeSec);
    }

    public BandwidthControlRuleAction(ActionType action, BandwidthControlApp app, Integer priority, Integer quotaTimeSec, Long quotaBytes)
    {
        this.setApp(app);
        this.setActionType(action);
        this.setQuotaTime(quotaTimeSec);
        this.setQuotaBytes(quotaBytes);
    }
    
    /**
     * Get the action type for this action
     * @return the ActionType
     */
    public ActionType getActionType()
    {
        return this.action;
    }

    /**
     * Set the action type for this action
     * @param action - the ActionType
     */
    public void setActionType(ActionType action)
    {
        // change deprecated values
        switch (action) {
        case GIVE_CLIENT_HOST_QUOTA:
            action = ActionType.GIVE_HOST_QUOTA;
            break;
        case PENALTY_BOX_CLIENT_HOST:
            action = ActionType.TAG_HOST;
            setTagName("penalty-box");
            break;
        default: break;
        }
        this.action = action;
    }

    /**
     * Get the priority for this action
     * 
     * Not all action types use this setting
     * For those actions that do not use this value is undefined
     *
     * @return the priority or null
     */
    public Integer getPriority()
    {
        return this.priority;
    }

    /**
     * Set the priority for this action
     * @param priority - the priority
     */
    public void setPriority(Integer priority)
    {
        this.priority = priority;
    }

    /**
     * Get the tag time for this action
     * This only applies for the TAG_HOST action
     * @return - the tag lifetime
     */
    public Integer getTagTime()
    {
        return this.tagTimeSec;
    }

    /**
     * Set the tag time for this action
     * This only applies for the TAG_HOST action
     * @param seconds - the tag lifetime
     */
    public void setTagTime(Integer seconds)
    {
        this.tagTimeSec = seconds;
    }

    /**
     * DEPRECATED 
     * Keep the old deprecated name so JSON serialization works
     * @param seconds - deprecated
     */
    public void setPenaltyTime(Integer seconds)
    {
        this.tagTimeSec = seconds;
    }
    
    /**
     * Get the tag name for this action
     * This only applies for the TAG_HOST action
     * @return the tag name
     */
    public String getTagName()
    {
        return this.tagName;
    }

    /**
     * Set the tag name for this action
     * This only applies for the TAG_HOST action
     * @param newValue - the new tag name
     */
    public void setTagName(String newValue)
    {
        this.tagName = newValue;
    }
    
    /**
     * Get the quota time for this action
     * This only applies for the GIVE_HOST_QUOTA, GIVE_USER_QUOTA action
     * @return the quota lifetime
     */
    public Integer getQuotaTime()
    {
        return this.quotaTimeSec;
    }

    /**
     * Set the quota time for this action
     * This only applies for the GIVE_HOST_QUOTA, GIVE_USER_QUOTA action
     * @param seconds - the quota lifetime
     */
    public void setQuotaTime(Integer seconds)
    {
        this.quotaTimeSec = seconds;
    }

    /**
     * Get the quota bytes for this action
     * This only applies for the GIVE_HOST_QUOTA, GIVE_USER_QUOTA action
     * @return the quota size
     */
    public Long getQuotaBytes()
    {
        return this.quotaBytes;
    }

    /**
     * Set the quota bytes for this action
     * This only applies for the GIVE_HOST_QUOTA, GIVE_USER_QUOTA action
     * @param bytes - the new quota size
     */
    public void setQuotaBytes(Long bytes)
    {
        this.quotaBytes = bytes;
    }
    
    /**
     * Sets the app that owns this rule and rule action
     * This reference is necessary to effect some actions (like adding to penalty box)
     * @param app - the bandwidth control app
     */
    public void setApp( BandwidthControlApp app )
    {
        this.app = app;
    }

    /**
     * Sets the rule associated with this action
     * @param rule - the rule that holds this action
     */
    public void setRule( BandwidthControlRule rule )
    {
        this.rule = rule;
    }
    
    /**
     * This prints this action in JSON format
     * This is used for JSON serialization
     * @return - the json string equivalent of this object
     */
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * This applies the action to the given session
     * @param sess - the session
     */
    public void apply( AppSession sess )
    {
        if (this.action == null) {
            logger.warn("Ignoring improperly initialized action");
            return;
        }

        int pri;
        String reason;
        long expireTime;
        InetAddress address;
        
        switch (this.action) {

        case SET_PRIORITY:

            pri = this.priority.intValue();

            logger.debug( "Applying Action    : " + 
                         sess.getClientAddr().getHostAddress() + ":" + sess.getClientPort() + " -> " +
                         sess.getServerAddr().getHostAddress() + ":" + sess.getServerPort() + " Set Priority : " + priority);

            if (pri != ((BandwidthControlSessionState)sess.attachment()).lastPriority) {
                sess.setClientQosMark(pri);
                sess.setServerQosMark(pri);
                ((BandwidthControlSessionState)sess.attachment()).lastPriority = pri;
                
                this.app.incrementCount( BandwidthControlApp.STAT_PRIORITIZE, 1 );
                this.app.logEvent( new PrioritizeEvent( sess.sessionEvent(), pri, this.rule.getRuleId() ) );
            }
            
            break;

        case TAG_HOST:
            address = sess.sessionEvent().getLocalAddr();
            reason = "Bandwidth Control" + " ( " + I18nUtil.marktr("policy") + ": " + this.app.getAppSettings().getPolicyId() + " " + I18nUtil.marktr("rule") + ": " + this.rule.getRuleId() + ")";
            logger.debug( "Applying Action    : " + "Tagging " + address + " with " + this.tagName + " for " + this.tagTimeSec + " seconds");
            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry( address, true );

            if ( entry == null ) {
                logger.warn( "No HostTableEntry. Failed to tag host: " + address.getHostAddress());
            } else {
                boolean wasAlreadyTagged = entry.hasTag( this.tagName );
                Tag tag = new Tag( this.tagName );
                tag.setLifetimeMillis( this.tagTimeSec*1000L );
                entry.addTag(tag);

                this.app.incrementCount( BandwidthControlApp.STAT_TAGGED, 1 );
                if (!wasAlreadyTagged)
                    UvmContextFactory.context().hookManager().callCallbacks( HookManager.HOST_TABLE_TAGGED, address );
            }

            break;

        case PENALTY_BOX_CLIENT_HOST:
            logger.warn("PENALTY_BOX_CLIENT_HOST used but is now DEPRECATED");
            break;
        case APPLY_PENALTY_PRIORITY: 
            logger.warn("APPLY_PENALTY_PRIORITY used but is now DEPRECATED");
            break;

        case GIVE_CLIENT_HOST_QUOTA:
        case GIVE_HOST_QUOTA: 
            if ( sess == null || sess.sessionEvent() == null ) {
                logger.warn("Missing session info: " + sess);
                break;
            }
            address = sess.sessionEvent().getLocalAddr();
            if ( address == null ) {
                logger.warn("Unknown local address for session: " + sess);
                break;
            }
            logger.debug( "Applying Action    : " + "Give Host Quota: " + address);

            expireTime = calculateQuotaExpireTime( this.quotaTimeSec );
            reason = "Bandwidth Control" + " ( " + I18nUtil.marktr("policy") + ": " + this.app.getAppSettings().getPolicyId() + " " + I18nUtil.marktr("rule") + ": " + this.rule.getRuleId() + ")";
            logger.debug("Giving " + address.getHostAddress() + " a Quota of " + this.quotaBytes + " bytes (expires in " + expireTime + " seconds)");
            UvmContextFactory.context().hostTable().giveHostQuota( address, this.quotaBytes, (int)expireTime, reason );

            break;

        case GIVE_USER_QUOTA:
            logger.debug( "Applying Action    : " + "Give User Quota: " + sess.user());
            if ( sess.user() == null ) {
                break;
            }

            expireTime = calculateQuotaExpireTime( this.quotaTimeSec );
            reason = "Bandwidth Control" + " ( " + I18nUtil.marktr("policy") + ": " + this.app.getAppSettings().getPolicyId() + " " + I18nUtil.marktr("rule") + ": " + this.rule.getRuleId() + ")";
            logger.debug("Giving " + sess.user() + " a Quota of " + this.quotaBytes + " bytes (expires in " + expireTime + " seconds)");
            UvmContextFactory.context().userTable().giveUserQuota( sess.user(), this.quotaBytes, (int)expireTime, reason );

            break;
            
        default:
            logger.error("Unknown action: " + this.action);

            break;
        }
    }

    private long calculateQuotaExpireTime( Integer quotaTimeSec )
    {
        switch(this.quotaTimeSec) {

        case END_OF_HOUR: {
            GregorianCalendar calendar = new GregorianCalendar();
            Date now = calendar.getTime();
            Date expireDate = null;
            calendar.add(Calendar.HOUR, 1);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            expireDate = calendar.getTime();
            long expireTime = (expireDate.getTime() - now.getTime()) / 1000;
            logger.debug("New Quota expires on : " + expireDate + " in " + expireTime + " seconds ");
            return expireTime;
        }
                
        case END_OF_DAY: {
            GregorianCalendar calendar = new GregorianCalendar();
            Date now = calendar.getTime();
            Date expireDate = null;
            calendar.add(Calendar.DAY_OF_WEEK, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.add(Calendar.SECOND, -1);
            /* subtract one second so as to avoid the whole AM/PM midnight confusion*/

            expireDate = calendar.getTime();
            long expireTime = (expireDate.getTime() - now.getTime()) / 1000;
            logger.debug("New Quota expires on : " + expireDate + " in " + expireTime + " seconds ");
            return expireTime;
        }
        case END_OF_WEEK: {
            GregorianCalendar calendar = new GregorianCalendar();
            Date now = calendar.getTime();
            Date expireDate = null;
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
            calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.add(Calendar.SECOND, -1);
            /* subtract one second so as to avoid the whole AM/PM midnight confusion*/

            expireDate = calendar.getTime();
            long expireTime = (expireDate.getTime() - now.getTime()) / 1000;
            logger.info("New Quota expires on : " + expireDate + " in " + expireTime + " seconds ");
            return expireTime;
        }
        case END_OF_MONTH: {
            GregorianCalendar calendar = new GregorianCalendar();
            Date now = calendar.getTime();
            Date expireDate = null;
            calendar.add(Calendar.MONTH, 1);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.add(Calendar.SECOND, -1);
            /* subtract one second so as to avoid the whole AM/PM midnight confusion*/

            expireDate = calendar.getTime();
            long expireTime = (expireDate.getTime() - now.getTime()) / 1000;
            logger.info("New Quota expires on : " + expireDate + " in " + expireTime + " seconds ");
            return expireTime;
        }
        default:
            return quotaTimeSec;
        }
    }
    
}

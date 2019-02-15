/**
 * $Id$
 */
package com.untangle.app.directory_connector;

import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.app.directory_connector.GroupEntry;
import com.untangle.app.directory_connector.UserEntry;
import com.untangle.uvm.app.License;
import com.untangle.uvm.app.GroupMatcher;
import com.untangle.uvm.app.DomainMatcher;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.util.Pulse.PulseState;

/**
 * Group manager process that checks for membership changes on AD servers.
 */
public class GroupManager
{
    /* Default amount of time between updating the Group Cache */
    private static long DEFAULT_GROUP_RENEW_MS = (long) 30 * 60 * 1000; /* every 30 minutes */

    private static final int CACHE_COUNT_MAX = 4000;

    private final Logger logger = Logger.getLogger( getClass());

    /**
     * Mapping from users to the groups they are in.  Using a boolean because
     * it will cache the fact that a user is not in a group so it won't have to recurse.
     * This map is not synchronized, but the maps inside of it are.
     */
    private Map<String,Map<String,Map<String,Boolean>>> domainsGroupsUsersCache = null;

    /**
     * Mapping from group to the groups that are in that group.
     * This map is not synchronized because it is never modified, nor are the items inside of it.
     */
    private Map<String,Map<String,Set<String>>> domainsGroupsChildrenCache = null;

    /**
     * Mapping users in domain.
     */
    private Map<String,Map<String,Boolean>> domainsUsersCache = null;

    private Map<String,Map<DomainMatcher,Boolean>> domainMatcherCache = new ConcurrentHashMap<>();
    private Map<String,Map<GroupManager,Boolean>> groupMatcherCache = new ConcurrentHashMap<>();

    /**
     * Pulse thread to re-read the AD into cache
     */
    private Pulse pulseRenewCache = new Pulse("renew-ad-cache", new RenewCache(), DEFAULT_GROUP_RENEW_MS, true);

    /**
     * This is used to cap the number of negative cache hits. This way someone
     * can't use all of the memory in the box by just making requests from a
     * number of invalid user names. Once it hits the cache limit it only caches
     * positive hits, not negative ones.
     */
    private int cacheCount = 0;

    private DirectoryConnectorApp app;

    /**
     * Initialize manager.
     *
     * @param app
     *  Directory connector application.
     * @return
     *  GroupManager object.
     */
    public GroupManager(DirectoryConnectorApp app)
    {
        this.app = app;
    }

    /**
     * Start the process.  If already running, force a new run.
     */
    public synchronized void start()
    {
        if ( this.pulseRenewCache.getState() == PulseState.RUNNING) {
            this.pulseRenewCache.forceRun();
        } else {
            this.pulseRenewCache.start();
        }
    }

    /**
     * Stop the process.
     */
    public synchronized void stop()
    {
            this.pulseRenewCache.stop();
    }

    /**
     * Checks to see if user is within a domain.
     *
     * @param user
     *  Username to check.
     * @param domain
     *  Domain name to check.
     *
     * @return
     *  true if in domain, false if not.
     */
    public boolean isMemberOfDomain( String user, String domain )
    {
        if ( user == null ) {
            return false;
        }

        if ( domain == null ) {
            return false;
        }

        user = user.trim().toLowerCase();
        domain = domain.trim().toLowerCase();

        if ( user.length() == 0 || domain.length() == 0 ) {
            return false;
        }

        /* Map is not initialized yet */
        if ( this.domainsUsersCache == null ) {
            return false;
        }

        if(domainsUsersCache.get(domain) == null){
            return false;
        }
        return ( domainsUsersCache.get(domain).get(user) != null ) ? true : false;
    }

    /**
     * Checks to see if user is within a domain.
     *
     * @param user
     *  Username to check.
     * @param domainMatcher
     *  Domain name to check.
     *
     * @return
     *  true if in domain, false if not.
     */
    public boolean isMemberOfDomain( String user, DomainMatcher domainMatcher )
    {
        if ( user == null ) {
            return false;
        }

        if ( domainMatcher == null ) {
            return false;
        }

        user = user.trim().toLowerCase();

        if ( user.length() == 0) {
            return false;
        }

        /* Map is not initialized yet */
        if ( this.domainsUsersCache == null ) {
            return false;
        }

        Boolean isMember = false;
        for (String domain : domainsUsersCache.keySet()) {
            // if(domainMatcherCache.get(domain)){
            //     isMember = domainMatcherCache.get(domain).get(domainMatcher);
            //     if(isMember == nul){

            //     }
            // }
            // if (domainMatcher.isMatch(domain)) {
            //     return ( domainsUsersCache.get(domain).get(user) != null ) ? true : false;
            // }
        }
        return false;
    }

    /**
     * Get list of domains this username is part of.
     *
     * @param user
     *  Username to lookup.
     *
     * @return
     *  List of domain name strings.
     */
    public List<String> memberOfDomain(String user)
    {
        List<String> userDomains = new LinkedList<>();
        for(String domain : domainsGroupsUsersCache.keySet()){
            Set<String> allGroups = this.domainsGroupsUsersCache.get(domain).keySet();

            for ( String group : allGroups ) {
                if (isMemberOfGroup(user, group)) {
                    userDomains.add(domain);
                }
            }
        }
        return userDomains;
    }

    /**
     * Checks to see if user is within a group.
     *
     * @param user
     *  Username to check.
     * @param group
     *  Group name to check.
     *
     * @return
     *  true if in group, false if not.
     */
    public boolean isMemberOfGroup( String user, String group )
    {
        return isMemberOfGroup(user, group, null, null);
    }


    /**
     * Checks to see if user is within a group across any domain
     *
     * @param user
     *  Username to check.
     * @param groupMatcher
     *  GroupMatcher to check.
     *
     * @return
     *  true if in group, false if not.
     */
    public boolean isMemberOfGroup( String user, GroupMatcher groupMatcher)
    {
        return isMemberOfGroup(user, null, groupMatcher, null);
    }

    /**
     * Checks to see if user is within a group within a specific domain.
     *
     * @param user
     *  Username to check.
     * @param group
     *  Group name to check.
     * @param groupMatcher
     *  Group name to check.
     * @param targetDomain
     *  Domain name to check.
     *
     * @return
     *  true if in group/domain, false if not.
     */
    public boolean isMemberOfGroup( String user, String group, GroupMatcher groupMatcher, String targetDomain )
    {
        if ( user == null ) {
            return false;
        }

        if ( group == null && groupMatcher == null) {
            return false;
        }

        user = user.trim().toLowerCase();
        if(group != null){
            group = group.trim().toLowerCase();
            if ( group.length() == 0 ) {
                return false;
            }

        }

        if ( user.length() == 0) {
            return false;
        }

        /* Map is not initialized yet */
        if ( this.domainsGroupsUsersCache == null ) {
            return false;
        }

        Boolean isMember = null;
        for(String domain : domainsGroupsUsersCache.keySet()){
            if(targetDomain != null && !domain.equals(targetDomain)){
                continue;
            }

            if(groupMatcher != null){
                for(String groupSearch : this.domainsGroupsUsersCache.get(domain).keySet()){
                    if(groupMatcher.isMatch(groupSearch)){
                        isMember = isMemberOfGroupSearch(domain, user, groupSearch, this.domainsGroupsUsersCache.get(domain).get(groupSearch));
                        if(isMember == null){
                            continue;
                        }
                        if(isMember == true){
                            break;
                        }
                    }
                }
            }else{
                Map<String,Boolean> groupsUsers = this.domainsGroupsUsersCache.get(domain).get(group);
                isMember = isMemberOfGroupSearch(domain, user, group, groupsUsers);
            }
            if (isMember == null) {
                continue;
            }
            return isMember;
        }

        for(String domain : domainsGroupsUsersCache.keySet()){
            if(targetDomain != null && !domain.equals(targetDomain)){
                continue;
            }
            Boolean foundUser = false;
            if ( this.cacheCount < CACHE_COUNT_MAX ) {
                Map<String,Boolean> groupsUsers = null;
                if(groupMatcher != null){
                    // Not possible to negatively cache groupmatchers yet.
                }else{
                    groupsUsers = this.domainsGroupsUsersCache.get(domain).get(group);
                    if ( groupsUsers == null ) {
                        continue;
                    }
                    foundUser = true;
                    groupsUsers.put(user, false);
                }
                if(foundUser){
                    /**
                     * If it hasn't been found at this point
                     * This user is not in the group nor any parents
                     * Cache the fact that the user is not in the group.
                     */
                    synchronized( this ) {
                        this.cacheCount++;
                    }
                }
            }
        }

        return false;
    }

    /**
     * [isMemberOfGroupSearch description]
     * @param  domain       [description]
     * @param  user       [description]
     * @param  group      [description]
     * @param  groupsUsers [description]
     * @return            [description]
     */
    private Boolean isMemberOfGroupSearch(String domain, String user, String group, Map<String,Boolean> groupsUsers)
    {
        /* These are updated in a refresh cache. */
        if ( groupsUsers == null ) {
            return null;
        }

        /* This caches if the user is in and out of the group. */
        Boolean isMember = groupsUsers.get(user);
        if(isMember != null){
            return isMember;
        }

        /**
          * At this point we have determined the user is not *directly* in this group
          * However, it could be be in a group that is a child of this group
          * Check all the children
          * Note: We don't need to recurse here
          * this child group includes all grandchildren etc that been expanded already
           */
        Set<String> groups = this.domainsGroupsChildrenCache.get(domain).get(group);
        if ( groups != null ) {
            for ( String childGroup : groups ) {
                Map<String,Boolean> inChild = this.domainsGroupsUsersCache.get(domain).get(childGroup);

                if ( inChild != null ) {
                    isMember = inChild.get(user);
                    if (( isMember != null ) && isMember ) {
                        /**
                          * Cache the positive result
                          * This just stores the current user as in the parent group
                          * so we won't have to recurse next time
                          */
                        groupsUsers.put( user, true );
                        return true;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Get list of groups this username is part of across all domains.
     *
     * @param user
     *  Username to lookup.
     *
     * @return
     *  List of group name strings.
     */
    public List<String> memberOfGroup(String user)
    {
        List<String> userGroups = new LinkedList<>();
        if (this.domainsGroupsUsersCache == null){
            return userGroups;
        }

        for( String domain : this.domainsGroupsUsersCache.keySet()){
            Set<String> allGroups = this.domainsGroupsUsersCache.get(domain).keySet();

            for ( String group : allGroups ) {
                if (isMemberOfGroup(user, group)) {
                    userGroups.add(group);
                }
            }
        }
        return userGroups;
    }

    /**
     * Get list of groups this username within the specified domain.
     *
     * @param user
     *  Username to lookup.
     * @param targetDomain
     *  Domain to use.
     *
     * @return
     *  List of group name strings.
     */
    public List<String> memberOfGroup(String user, String targetDomain)
    {
        List<String> myGroups = new LinkedList<>();

        if (this.domainsGroupsUsersCache == null)
            return myGroups;

        boolean found;
        for( String domain : this.domainsGroupsUsersCache.keySet()){
            if(targetDomain != null && !domain.equals(targetDomain)){
                continue;
            }
            Set<String> allGroups = this.domainsGroupsUsersCache.get(domain).keySet();

            for ( String group : allGroups ) {
                if (isMemberOfGroup(user, group, null, domain)) {
                    found = false;
                    for(String myGroup: myGroups){
                        if(myGroup.equals(group)){
                            found = true;
                        }
                    }
                    if(found == false){
                        myGroups.add(group);
                    }
                }
            }
        }

        return myGroups;
    }

    /**
     * Force a new run against AD server.
     */
    protected void refreshGroupCache()
    {
        this.pulseRenewCache.forceRun();
    }

    /**
     * Check that the directory connector license is valid.
     *
     * @return
     *  true if valid, otherwise false.
     */
    private boolean isLicenseValid()
    {
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.DIRECTORY_CONNECTOR))
            return true;
        return false;
    }

    /**
     * Renew the doman/group cache across all available domains.
     */
    private class RenewCache implements Runnable
    {
        /* Typically it should be able to recurse all of the parents in
         * a few loops, but if someone has a very nested weird structure, we just
         * give up.  (40 descendants would be insane).
         */
        private static final int MAX_UPDATE_GROUP_MAPS = 40;

        /**
         * Cache update process.
         */
        public void run()
        {
            if ( !isRenewEnabled()) {
                return;
            }

            logger.info("Renewing AD Group Cache...");

            List<String> domains = null;
            try {
                domains = app.getActiveDirectoryManager().getDomains();
            } catch ( Exception ex ) {
                logger.warn("Unable to retrieve the domains", ex);
                return;
            }

            Map<String,Map<String,Map<String,Boolean>>> domainsGroupsUsersCache = new ConcurrentHashMap<>(domains.size());
            Map<String,Map<String,Set<String>>> domainsGroupsChildrenCache = new ConcurrentHashMap<>(domains.size());
            Map<String,Map<String,Boolean>> domainsUsersCache = new ConcurrentHashMap<>(domains.size());
            for( String domain : domains){
                List<GroupEntry> groupList = null;
                try {
                    groupList = app.getActiveDirectoryManager().getGroupEntries(domain, true);
                } catch ( Exception ex ) {
                    logger.warn("Unable to retrieve the group entries", ex);
                    return;
                }
                Map<String,Boolean> domainUsersMap = new ConcurrentHashMap<String,Boolean>();

                /** Create new user and group maps */
                int numGroups = groupList.size();
                Map<String,Map<String,Boolean>> groupsUsersMap = new ConcurrentHashMap<>(numGroups);
                Map<String,String> groupDNToAccountName = new ConcurrentHashMap<>(numGroups);
                Map<String,Set<String>> groupsChildrenMap = new ConcurrentHashMap<>(numGroups);

                /* Build a mapping of all of the groups to users, this has to recurse. */
                for ( GroupEntry groupEntry : groupList ) {
                    String groupName = groupEntry.getSAMAccountName();
                    logger.debug("Building Group Cache: Processing Group: " + groupName);

                    /* Update the mapping from the dn to the account name */
                    groupDNToAccountName.put(groupEntry.getDN(), groupName);

                    Map<String,Boolean> groupUsers = new ConcurrentHashMap<>();

                    /* Get all of the groups and users in this group. */
                    try {
                        List<UserEntry> userList = app.getActiveDirectoryManager().getGroupUsers(domain, groupName);

                        for ( UserEntry user : userList ) {
                            logger.debug("Building Group Cache: Adding User " + user.getUid() + " to " + groupName);
                            groupUsers.put(user.getUid(),true);
                            domainUsersMap.put(user.getUid(),true);
                        }
                    } catch ( Exception ex ) {
                        logger.warn("Unable to refresh users",ex);
                    }

                    groupsUsersMap.put(groupName, groupUsers);
                }

                /**
                 * Now that all of the DNs are mapped, create an initial child map
                 */
                for ( GroupEntry groupEntry : groupList ) {
                    String groupName = groupEntry.getSAMAccountName();
                    logger.debug("Building Group Cache: Processing Group Mapping: " + groupName);

                    Set<String> children = groupsChildrenMap.get(groupName);
                    if (children == null) {
                        children = new HashSet<>();
                        groupsChildrenMap.put(groupName, children);
                    }

                    /* Now add all of the items that this group is a member of */
                    Set<String> groupParentSet = new HashSet<>();

                    for ( String parent : groupEntry.getMemberOf()) {
                        /* Convert from DN to parentName */
                        String parentName = groupDNToAccountName.get(parent);
                        if ( parentName == null ) {
                            logger.warn( "Missing the account name for the DN: '" + parent + "'");
                            continue;
                        }

                        /**
                         * Add child to childMap 
                         * Could still be null, so just create an empty set
                         */
                        children = groupsChildrenMap.get(parentName);
                        if (children == null) {
                            children = new HashSet<>();
                            groupsChildrenMap.put(parentName, children);
                        }
                        logger.debug("Building Group Cache: Processing Group Mapping: " + parentName + " adding child: " + groupName);
                        children.add(groupName);
                    }
                }
                /**
                 * Now we must iterate deep into the tree and add grand children, great grand children
                 * and so on
                 */
                boolean hasUpdates = true;
                int c = 0;
                for ( c = 0 ; hasUpdates && c < MAX_UPDATE_GROUP_MAPS ; c++ ) {
                    hasUpdates = false;

                    for ( GroupEntry groupEntry : groupList ) {
                        String groupName = groupEntry.getSAMAccountName();
                        logger.debug("Building Group Cache: Processing Group Hierarchy: " + groupName);
                        Set<String> childSet = groupsChildrenMap.get(groupName);
                        Set<String> newChildren = new HashSet<>();

                        if ( childSet == null ) {
                            /* should never happen */
                            logger.warn("Child set of " + groupName + "is null");
                            continue;
                        }

                        /* Get the children of all of the child groups, and add them as well */
                        for ( String childGroup : childSet ) {
                            Set<String> grandChildSet = groupsChildrenMap.get(childGroup);

                            if ( grandChildSet == null ) {
                                /* should never happen */
                                logger.warn("Child set of " + childGroup + "is null");
                                continue;
                            }

                            /* Add all of the parents parents */
                            logger.debug("Building Group Cache: Processing Group Hierarchy: " + groupName + " adding grand-" + c + "-children: " + grandChildSet);
                            newChildren.addAll(grandChildSet);
                        }

                        if ( childSet.addAll(newChildren)) {
                            hasUpdates = true;
                        }
                    }
                }

                /**
                 * For debugging
                 */
                for ( GroupEntry groupEntry : groupList ) {
                    String groupName = groupEntry.getSAMAccountName();
                    logger.debug("Group: " + groupName + " children: " + groupsChildrenMap.get(groupName));
                }

                logger.debug( "Required " + c + " Updates to refresh parents." );

                /**
                 * Update the global caches that are used.
                 */
                domainsGroupsUsersCache.put(domain, groupsUsersMap);
                domainsGroupsChildrenCache.put(domain, groupsChildrenMap);
                domainsUsersCache.put(domain, domainUsersMap);
                logger.info("Renewing AD Group Cache: domain=" + domain + ", users=" + domainUsersMap.size() + ", groups=" + groupsUsersMap.size());
            }
            GroupManager.this.domainsGroupsUsersCache = domainsGroupsUsersCache;
            GroupManager.this.domainsGroupsChildrenCache = domainsGroupsChildrenCache;
            GroupManager.this.domainsUsersCache = domainsUsersCache;

            GroupManager.this.domainMatcherCache.clear();
            GroupManager.this.groupMatcherCache.clear();

            /**
             * Reset the cache count, this is just used so negative requests don't get out
             * of control.
             */
            GroupManager.this.cacheCount  = 0;

            logger.info("Renewing AD Group Cache: done");
        }

        /** 
         * Check to see if domain connector license is still active.
         *
         * @return
         *  true if still active, false otherwise.
         */
        private boolean isRenewEnabled()
        {
            if ( !isLicenseValid() ) {
                logger.warn( "Invalid license, not renewing group cache.");
                return false;
            }

            return true;
        }
    }
}

/*
 * $Id$
 */
package com.untangle.app.directory_connector;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.app.directory_connector.GroupEntry;
import com.untangle.app.directory_connector.UserEntry;
import com.untangle.uvm.app.License;
import com.untangle.uvm.app.App;
import com.untangle.uvm.app.AppSettings.AppState;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.util.Pulse.PulseState;

public class GroupManager
{
    /* Default amount of time between updating the Group Cache */
    private static long DEFAULT_GROUP_RENEW_MS = 30 * 60 * 1000; /* every 30 minutes */

    private static final int CACHE_COUNT_MAX = 4000;

    private final Logger logger = Logger.getLogger( getClass());

    /**
     * Mapping from users to the groups they are in.  Using a boolean because
     * it will cache the fact that a user is not in a group so it won't have to recurse.
     * This map is not synchronized, but the maps inside of it are.
     */
    private Map<String,Map<String,Boolean>> userToGroupMap;
    
    /**
     * Mapping from group to the groups that are in that group.
     * This map is not synchronized because it is never modified, nor are the items inside of it.
     */
    private Map<String,Set<String>> groupToChildrenMap;

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

    private DirectoryConnectorApp node;
    
    public GroupManager(DirectoryConnectorApp node)
    {
        this.node = node;
    }
    
    public synchronized void start()
    {
        if ( this.pulseRenewCache.getState() == PulseState.RUNNING) {
            this.pulseRenewCache.forceRun();
        } else {
            this.pulseRenewCache.start();
        }
    }
    
    public synchronized void stop()
    {
            this.pulseRenewCache.stop();
    }
    
    public boolean isMemberOf( String user, String group )
    {
        if ( ! isLicenseValid() ) {
            return false;
        }
        
        if ( user == null ) {
            return false;
        }
        
        if ( group == null ) {
            return false;
        }
        
        user = user.trim().toLowerCase();
        group = group.trim().toLowerCase();
        
        if ( user.length() == 0 || group.length() == 0 ) {
            return false;
        }

        /* Map is not initialized yet */
        if ( this.userToGroupMap == null ) {
            return false;
        }
        
        Map<String,Boolean> thisGroupsUsers = this.userToGroupMap.get(group);
        
        /* These are updated in a refresh cache. */
        if ( thisGroupsUsers == null ) {
            return false;
        }

        /* This caches if the user is in and out of the group. */
        Boolean isMember = thisGroupsUsers.get(user);
        if ( isMember != null ) {
            return isMember;
        }
        
        /**
         * At this point we have determined the user is not *directly* in this group
         * However, it could be be in a group that is a child of this group
         * Check all the children
         * Note: We don't need to recurse here
         * this child group includes all grandchildren etc that been expanded already
         */
        Set<String> groups = this.groupToChildrenMap.get(group);
        if ( groups != null ) {
            for ( String childGroup : groups ) {
                Map<String,Boolean> inChild = this.userToGroupMap.get(childGroup);
            
                if ( inChild != null ) {
                    isMember = inChild.get(user);
                    if (( isMember != null ) && isMember ) {
                        /**
                         * Cache the positive result
                         * This just stores the current user as in the parent group
                         * so we won't have to recurse next time
                         */
                        thisGroupsUsers.put( user, true );
                        return true;
                    }
                }
            }
        }
        
        /**
         * If it hasn't been found at this point
         * This user is not in the group nor any parents
         * Cache the fact that the user is not in the group.
         */
        if ( this.cacheCount < CACHE_COUNT_MAX ) {
            synchronized( this ) {
                this.cacheCount++;
            }
            thisGroupsUsers.put(user, false);
        }
        
        return false;
    }

    public List<String> memberOf(String user)
    {
        List<String> myGroups = new LinkedList<String>();

        if (this.userToGroupMap == null)
            return myGroups;

        Set<String> allGroups = this.userToGroupMap.keySet();

        for ( String group : allGroups ) {
            if (isMemberOf(user, group)) {
                myGroups.add(group);
            }
        }

        return myGroups;
    }

    protected void refreshGroupCache()
    {
        this.pulseRenewCache.forceRun();
    }
    
    private boolean isLicenseValid()
    {
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.DIRECTORY_CONNECTOR))
            return true;
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.DIRECTORY_CONNECTOR_OLDNAME))
            return true;
        return false;
    }

    private class RenewCache implements Runnable
    {
        /* Typically it should be able to recurse all of the parents in
         * a few loops, but if someone has a very nested weird structure, we just
         * give up.  (40 descendants would be insane).
         */
        private static final int MAX_UPDATE_GROUP_MAPS = 40;

        public void run()
        {
            if ( !isRenewEnabled()) {
                return;
            }

            logger.info("Renewing AD Group Cache...");
            
            List<GroupEntry> groupList = null;
            try {
                groupList = node.getActiveDirectoryManager().getActiveDirectoryGroupEntries(true);
            } catch ( Exception ex ) {
                logger.warn("Unable to retrieve the group entries", ex);
                return;
            }
            
            /** Create new user and group maps */
            int numGroups = groupList.size();
            Map<String,Map<String,Boolean>> userToGroupMap = new ConcurrentHashMap<String,Map<String,Boolean>>(numGroups);
            Map<String,String> groupDNToAccountName = new ConcurrentHashMap<String,String>(numGroups);
            Map<String,Set<String>> groupToChildrenMap = new ConcurrentHashMap<String,Set<String>>(numGroups);
            
            /* Build a mapping of all of the groups to users, this has to recurse. */
            for ( GroupEntry groupEntry : groupList ) {
                String groupName = groupEntry.getSAMAccountName();
                logger.debug("Building Group Cache: Processing Group: " + groupName);
                
                /* Update the mapping from the dn to the account name */
                groupDNToAccountName.put(groupEntry.getDN(), groupName);
                
                Map<String,Boolean> groupUsers = new ConcurrentHashMap<String,Boolean>();

                /* Get all of the groups and users in this group. */
                try {
                    List<UserEntry> userList = node.getActiveDirectoryManager().getActiveDirectoryGroupUsers(groupName);

                    for ( UserEntry user : userList ) {
                        logger.debug("Building Group Cache: Adding User " + user.getUid() + " to " + groupName);
                        groupUsers.put(user.getUid(),true);
                    }
                } catch ( Exception ex ) {
                    logger.warn("Unable to refresh users",ex);
                }
                
                userToGroupMap.put(groupName, groupUsers);
            }
            
            /**
             * Now that all of the DNs are mapped, create an initial child map
             */
            for ( GroupEntry groupEntry : groupList ) {
                String groupName = groupEntry.getSAMAccountName();
                logger.debug("Building Group Cache: Processing Group Mapping: " + groupName);

                Set<String> children = groupToChildrenMap.get(groupName);
                if (children == null) {
                    children = new HashSet<String>();
                    groupToChildrenMap.put(groupName, children);
                }

                /* Now add all of the items that this group is a member of */
                Set<String> groupParentSet = new HashSet<String>();
                
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
                    children = groupToChildrenMap.get(parentName);
                    if (children == null) {
                        children = new HashSet<String>();
                        groupToChildrenMap.put(parentName, children);
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
                    Set<String> childSet = groupToChildrenMap.get(groupName);
                    Set<String> newChildren = new HashSet<String>();
                    
                    if ( childSet == null ) {
                        /* should never happen */
                        logger.warn("Child set of " + groupName + "is null");
                        continue;
                    }

                    /* Get the children of all of the child groups, and add them as well */
                    for ( String childGroup : childSet ) {
                        Set<String> grandChildSet = groupToChildrenMap.get(childGroup);

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
                logger.debug("Group: " + groupName + " children: " + groupToChildrenMap.get(groupName));
            }
 
            logger.debug( "Required " + c + " Updates to refresh parents." );
            
            /**
             * Update the global caches that are used.
             */
            GroupManager.this.userToGroupMap = userToGroupMap;
            GroupManager.this.groupToChildrenMap = groupToChildrenMap;

            /**
             * Reset the cache count, this is just used so negative requests don't get out
             * of control.
             */
            GroupManager.this.cacheCount  = 0;

            logger.info("Renewing AD Group Cache... done");
        }

        private boolean isRenewEnabled()
        {
            if ( !GroupManager.this.isLicenseValid()) {
                logger.warn( "Invalid license, not renewing group cache.");
                return false;
            }
            
            return true;
        }
    }
}

/*
 * $Id$
 */
package com.untangle.uvm;

import com.untangle.uvm.node.IPSessionDesc;
import com.untangle.uvm.node.SessionEndpoints;

public class SessionMatcherFactory
{
    private SessionMatcherFactory()
    {
    }

    static final SessionMatcher NULL_MATCHER = new SessionMatcher() {
            public boolean isMatch( Long policyId, IPSessionDesc client, IPSessionDesc server )
            {
                return false;
            }
        };

    static final SessionMatcher ALL_MATCHER = new SessionMatcher() {
            public boolean isMatch( Long policyId, IPSessionDesc client, IPSessionDesc server )
            {
                return true;
            }
        };

    static final SessionMatcher TCP_MATCHER = new SessionMatcher() {
            public boolean isMatch( Long policyId, IPSessionDesc client, IPSessionDesc server )
            {
                return ( client.protocol() == SessionEndpoints.PROTO_TCP ) ? true : false;
            }
        };

    static final SessionMatcher UDP_MATCHER = new SessionMatcher() {
            public boolean isMatch( Long policyId, IPSessionDesc client, IPSessionDesc server )
            {

                return ( client.protocol() == SessionEndpoints.PROTO_UDP ) ? true : false;
            }
        };

    public static SessionMatcher getNullInstance()
    {
        return NULL_MATCHER;
    }

    public static SessionMatcher getAllInstance()
    {
        return ALL_MATCHER;
    }

    public static SessionMatcher getTcpInstance()
    {
        return TCP_MATCHER;
    }

    public static SessionMatcher getUdpInstance()
    {
        return UDP_MATCHER;
    }

    public static SessionMatcher makePolicyInstance( final Long policyId )
    {
        /* null policy is a service, use an all matcher */
        if ( null == policyId ) return getAllInstance();

        return new SessionMatcher() {
                public boolean isMatch( Long sessionPolicyId, IPSessionDesc client, IPSessionDesc server )
                {
                    return ( policyId.equals( sessionPolicyId ));
                }
            };
    }

}

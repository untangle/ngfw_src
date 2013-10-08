/**
 * $Id: PipeSpec.java 35447 2013-07-29 17:24:43Z dmorris $
 */
package com.untangle.uvm.vnet;

import java.util.Set;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import com.untangle.uvm.node.Node;

/**
 * Describes the fittings for this pipe.
 *
 */
public abstract class PipeSpec
{
    private final String name;
    private final Node node;

    private volatile Set<Subscription> subscriptions;
    private volatile boolean enabled = true;

    // constructors -----------------------------------------------------------

    /**
     * Creates a new PipeSpec, with a subscription for all traffic.
     *
     * @param name display name for this PipelineConnector.
     */
    protected PipeSpec(String name, Node node)
    {
        this.name = name;
        this.node = node;

        this.subscriptions = new CopyOnWriteArraySet<Subscription>();
        this.subscriptions.add(new Subscription(Protocol.TCP));
        this.subscriptions.add(new Subscription(Protocol.UDP));
    }

    /**
     * Creates a new PipeSpec.
     *
     * @param name display name of the PipeSpec.
     * @param subscriptions set of Subscriptions.
     */
    protected PipeSpec(String name, Node node, Set<Subscription> subscriptions)
    {
        this.name = name;
        this.node = node;

        this.subscriptions = null == subscriptions
            ? new CopyOnWriteArraySet<Subscription>()
            : new CopyOnWriteArraySet<Subscription>(subscriptions);
    }

    /**
     * Creates a new PipeSpec.
     *
     * @param name display name of the PipeSpec.
     * @param subscription the Subscription.
     */
    protected PipeSpec(String name, Node node, Subscription subscription)
    {
        this.name = name;
        this.node = node;

        this.subscriptions = new CopyOnWriteArraySet<Subscription>();
        if (null != subscription) {
            this.subscriptions.add(subscription);
        }
    }

    // public abstract methods ------------------------------------------------

    public abstract void connectPipelineConnector();
    public abstract void disconnectPipelineConnector();
    public abstract List<PipelineConnector> getPipelineConnectors();
    public abstract List<NodeSession> liveSessions();

    // accessors --------------------------------------------------------------

    public String getName()
    {
        return name;
    }

    public Node getNode()
    {
        return node;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    // public methods ---------------------------------------------------------
    public void setSubscriptions(Set<Subscription> subscriptions)
    {
        synchronized (this) {
            this.subscriptions = new CopyOnWriteArraySet<Subscription>(subscriptions);
        }
    }

    public void addSubscription(Subscription subscription)
    {
        synchronized (this) {
            subscriptions.add(subscription);
        }
    }

    public void removeSubscription(Subscription subscription)
    {
        synchronized (this) {
            subscriptions.remove(subscription);
        }
    }
    
    public boolean matches(com.untangle.uvm.node.SessionTuple tuple)
    {
        if ( !enabled ) {
            return false;
        }
        
        Set<Subscription> s = subscriptions;

        for ( Subscription subscription : s ) {
            if ( subscription.matches( tuple ) ) {
                return true;
            }
        }
        return false;
    }

}

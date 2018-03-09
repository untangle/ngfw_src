Ext.define('Ung.apps.wan-balancer.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-wan-balancer-status',
    itemId: 'status',
    title: 'Status'.t(),
    scrollable: true,

    layout: 'border',
    items: [{
        region: 'center',
        border: false,
        bodyPadding: 10,
        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/icons/apps/wan-balancer.svg" width="80" height="80"/>' +
                '<h3>WAN Balancer</h3>' +
                '<p>' + 'WAN Balancer spreads network traffic across multiple internet connections for better performance.'.t() + '</p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appstate',
        }, {
            xtype: 'app-wan-balancer-allocation',
        }, {
            xtype: 'button',
            text: 'Configure additional WAN interfaces'.t(),
            iconCls: 'fa fa-cogs',
            handler: function() { Ung.app.redirectTo('#config/network'); }
        }, {
            xtype: 'appreports'
        }]
    }, {
        region: 'west',
        border: false,
        width: Math.ceil(Ext.getBody().getViewSize().width / 4),
        split: true,
        items: [{
            xtype: 'appmetrics',
            region: 'center'
        }],
        bbar: [{
            xtype: 'appremove',
            width: '100%'
        }]
    }]
});

Ext.define('Ung.apps.wan-balancer.view.Allocation', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-wan-balancer-allocation',
    itemId: 'Allocation',
    title: 'Current Traffic Allocation'.t(),
    padding: '0 0 20 0',
    border: false,

    items: [{
        xtype: 'component',
        html: 'Currently, WAN Balancer is attempting to share traffic over the existing WAN interfaces with the ratio displayed below. To change this ratio click on Traffic Allocation.'.t(),
        margin: '0 0 15 0'
    }, {
        xtype: 'component',
        padding: '10 0 10 0',
        bind: { html: '{trafficAllocation}' }
    }]
});

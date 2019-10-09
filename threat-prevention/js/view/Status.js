Ext.define('Ung.apps.ipreputation.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ip-reputation-status',
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
            html: '<img src="/icons/apps/ip-reputation.svg" width="80" height="80"/>' +
                '<h3>IP Reputation</h3>' +
                '<p>' + 'Firewall is a simple application that flags and blocks sessions based on rules.'.t() + '</p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appstate',
        }, {
            xtype: 'appreports'
        }]
    }, {
        region: 'west',
        border: false,
        width: Math.ceil(Ext.getBody().getViewSize().width / 4),
        split: true,
        layout: 'fit',
        items: [{
            xtype: 'appmetrics'
        }],
        bbar: [{
            xtype: 'appremove',
            width: '100%'
        }]
    }]

});

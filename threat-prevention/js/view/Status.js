Ext.define('Ung.apps.threatprevention.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-threat-prevention-status',
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
            html: '<img src="/icons/apps/threat-prevention.svg" width="80" height="80"/>' +
                '<h3>Threat Prevention</h3>' +
                '<p>' + 'Threat Prevention prevents threats associated with  untrustworthy IP Addresses and Websites based on their reputation.'.t() + '</p>'
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

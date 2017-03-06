Ext.define('Ung.apps.intrusionprevention.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-intrusionprevention-status',
    itemId: 'status',
    title: 'Status'.t(),

    layout: 'border',
    items: [{
        region: 'center',
        border: false,
        bodyPadding: 10,

        layout: {
            type: 'vbox',
            align: 'stretch'
        },

        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/skins/modern-rack/images/admin/apps/untangle-node-intrusion-prevention_80x80.png" width="80" height="80"/>' +
                '<h3>Intrusion Prevention</h3>' +
                '<p>' + 'Intrusion Prevention blocks scans, detects, and blocks attacks and suspicious traffic using signatures.'.t() + '</p>'
        }, {
            xtype: 'appstate',
        }, {
            xtype: 'appreports'
        }, {
            xtype: 'appremove'
        }]
    }, {
        region: 'west',
        xtype: 'appmetrics',
        border: false,
        width: 350,
        minWidth: 300,
        split: true,
        collapsible: true,
        titleCollapse: true,
        animCollapse: false
    }]
});

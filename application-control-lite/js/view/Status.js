Ext.define('Ung.apps.applicationcontrollite.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-application-control-lite-status',
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
            html: '<img src="/icons/apps/application-control-lite.svg" width="80" height="80"/>' +
                '<h3>Application Control Lite</h3>' +
                '<p>' + 'Application Control scans sessions and identifies the associated applications allowing each to be flagged and/or blocked.'.t() + '</p>'
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
        items: [{
            xtype: 'appsessions',
            region: 'north',
            height: 200,
            split: true
        }, {
            xtype: 'appmetrics',
            region: 'center',
            height: '40%'
        }],
        bbar: [{
            xtype: 'appremove',
            width: '100%'
        }]
    }]

});

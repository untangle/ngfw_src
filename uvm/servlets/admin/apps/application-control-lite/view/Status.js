Ext.define('Ung.apps.applicationcontrollite.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-application-control-lite-status',
    itemId: 'status',
    title: 'Status'.t(),

    layout: 'border',
    items: [{
        region: 'center',
        border: false,
        bodyPadding: 10,
        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/skins/modern-rack/images/admin/apps/untangle-node-application-control-lite_80x80.png" width="80" height="80"/>' +
                '<h3>Application Control Lite</h3>' +
                '<p>' + 'Application Control scans sessions and identifies the associated applications allowing each to be flagged and/or blocked.'.t() + '</p>'
        }, {
            xtype: 'appstate',
        }, {
            xtype: 'appreports'
        }, {
            xtype: 'appremove'
        }]
    }, {
        region: 'west',
        border: false,
        width: 350,
        minWidth: 300,
        split: true,
        layout: 'border',
        items: [{
            xtype: 'appsessions',
            region: 'north',
            height: 200,
            split: true
        }, {
            xtype: 'appmetrics',
            region: 'center',
            height: '40%'
        }]
    }]

});

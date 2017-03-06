Ext.define('Ung.apps.webfilter.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-webfilter-status',
    itemId: 'status',
    title: 'Status'.t(),

    viewModel: true,

    layout: 'border',
    items: [{
        region: 'center',
        border: false,
        bodyPadding: 10,
        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/skins/modern-rack/images/admin/apps/untangle-node-web-filter_80x80.png" width="80" height="80"/>' +
                '<h3>Web Filter</h3>' +
                '<p>' + 'Web Filter scans and categorizes web traffic to monitor and enforce network usage policies.'.t() + '</p>'
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
        // layout: {
        //     type: 'hbox'
        // },
        items: [{
            xtype: 'appsessions',
            region: 'north',
            height: 200,
            split: true,
        }, {
            xtype: 'appmetrics',
            region: 'center',
            sourceConfig: {
                // attachments:       { displayName: 'Attachments'.t() }
            },
        }]
    }]
});

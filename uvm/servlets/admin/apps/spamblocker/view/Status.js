Ext.define('Ung.apps.spamblocker.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app.spamblocker.status',
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
            html: '<img src="/skins/modern-rack/images/admin/apps/untangle-node-spam-blocker_80x80.png" width="80" height="80"/>' +
                '<h3>Spam Blocker</h3>' +
                '<p>' + 'Spam Blocker detects, blocks, and quarantines spam before it reaches users\' mailboxes.'.t() + '</p>'
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

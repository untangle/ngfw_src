Ext.define('Ung.apps.virusblocker.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-virus-blocker-status',
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
            html: '<img src="/skins/modern-rack/images/admin/apps/virus-blocker_80x80.png" width="80" height="80"/>' +
                '<h3>Virus Blocker</h3>' +
                '<p>' + 'Virus Blocker detects and blocks malware before it reaches users\' desktops or mailboxes.'.t() + '</p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appstate',
        }, {
            xtype: 'displayfield',
            fieldLabel: '<STRONG>' + 'Signatures were last updated'.t() + '</STRONG>',
            labelWidth: 250,
            bind: '{getSignatureTimestamp}'
        }, {
            xtype: 'displayfield',
            value: 'Scanning Engine &copy; BitDefender 1997-2017',
            bind: {
                hidden: '{!isFileScannerAvailable}'
            }
        }, {
            xtype: 'appreports'
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
            split: true,
        }, {
            xtype: 'appmetrics',
            region: 'center'
        }],
        bbar: [{
            xtype: 'appremove',
            width: '100%'
        }]
    }]
});

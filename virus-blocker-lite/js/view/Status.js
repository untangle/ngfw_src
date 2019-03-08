Ext.define('Ung.apps.virusblockerlite.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-virus-blocker-lite-status',
    itemId: 'status',
    title: 'Status'.t(),
    scrollable: true,

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
            html: '<img src="/icons/apps/virus-blocker-lite.svg" width="80" height="80"/>' +
                '<h3>Virus Blocker Lite</h3>' +
                '<p>' + 'Virus Blocker detects and blocks malware before it reaches users\' desktops or mailboxes.'.t() + '</p>'
        }, {
            xtype: 'appstate',
        }, {
            xtype: 'displayfield',
            fieldLabel: '<STRONG>' + 'Signatures were last updated'.t() + '</STRONG>',
            labelWidth: 250,
            bind: '{getSignatureTimestamp}'
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

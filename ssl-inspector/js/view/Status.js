Ext.define('Ung.apps.sslinspector.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ssl-inspector-status',
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
            html: '<img src="/skins/modern-rack/images/admin/apps/ssl-inspector_80x80.png" width="80" height="80"/>' +
                '<h3>SSL Inspector</h3>' +
                '<p>' + 'SSL Inspector allows for full decryption of HTTPS and SMTPS so that other applications can process the encrytped streams.'.t() + '</p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appstate',
        }, {
            xtype: 'component',
            bind: { html: '{serverCertificateVerification}' }
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
            region: 'center',
            height: 200
        }, {
            xtype: 'appmetrics',
            region: 'south',
            split: true,
            height: 'auto'
        }],
        bbar: [{
            xtype: 'appremove',
            width: '100%'
        }]
    }]

});

Ext.define('Ung.apps.phishblocker.view.Email', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-phishblocker-email',
    itemId: 'email',
    title: 'Email'.t(),

    tbar: [{
        xtype: 'checkbox',
        boxLabel: '<strong>' + 'Scan SMTP'.t() + '</strong>',
        bind: '{settings.smtpConfig.scan}',
        padding: 5
    }],

    layout: 'fit',

    items: [{
        xtype: 'container',
        padding: 10,
        disabled: true,
        scrollable: 'y',
        layout: {
            type: 'vbox',
            // align: 'stretch'
        },
        bind: {
            disabled: '{!settings.smtpConfig.scan}'
        },
        defaults: {
            margin: '5 0',
        },
        items: [{
            xtype: 'combo',
            editable: false,
            store: [
                ['MARK', 'Mark'.t()],
                ['PASS', 'Pass'.t()],
                ['DROP', 'Drop'.t()],
                ['QUARANTINE', 'Quarantine'.t()]
            ],
            fieldLabel: 'Action'.t(),
            width: 300,
            queryMode: 'local',
            bind: {
                value: '{settings.smtpConfig.msgAction}'
            }
        }, {
            xtype: 'fieldset',
            width: '100%',
            title: 'Note'.t(),
            margin: '10 0',
            padding: 10,
            items: [{
                xtype: 'component',
                bind: {
                    html: '<p>{lastUpdate}</p>'
                }
            }]
        }]
    }]
});

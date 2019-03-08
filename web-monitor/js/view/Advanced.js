Ext.define('Ung.apps.webmonitor.view.Advanced', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-web-monitor-advanced',
    itemId: 'advanced',
    title: 'Advanced'.t(),
    scrollable: true,

    bodyPadding: 10,

    items: [{
        xtype: 'fieldset',
        title: 'Process HTTPS traffic by SNI (Server Name Indication) information if present'.t(),
        collapsible: true,
        checkboxToggle: true,
        checkbox: {
            bind: '{settings.enableHttpsSni}'
        },
        padding: 10,
        items: [{
            xtype: 'checkbox',
            margin: '0 0 0 20',
            boxLabel: 'Process HTTPS traffic by hostname in server certificate when SNI information not present'.t(),
            disabled: true,
            bind: {
                value: '{settings.enableHttpsSniCertFallback}',
                disabled: '{!settings.enableHttpsSni}'
            },
            listeners: {
                disable: function (ck) {
                    ck.setValue(false);
                }
            }
        }, {
            xtype: 'checkbox',
            margin: '0 0 0 20',
            boxLabel: 'Process HTTPS traffic by server IP if both SNI and certificate hostname information are not available'.t(),
            disabled: true,
            bind: {
                value: '{settings.enableHttpsSniIpFallback}',
                disabled: '{!settings.enableHttpsSniCertFallback}'
            },
            listeners: {
                disable: function (ck) {
                    ck.setValue(false);
                }
            }
        }]
    }, {
        xtype: 'button',
        text: 'Clear Category URL Cache.'.t(),
        iconCls: 'fa fa-trash-o fa-red',
        handler: 'clearHostCache'
    }]

});

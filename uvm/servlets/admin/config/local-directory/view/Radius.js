Ext.define('Ung.config.local-directory.view.Radius', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-local-directory-radius',
    itemId: 'radius',
    title: 'RADIUS',
    scrollable: true,
    viewModel: true,

    bodyPadding: 10,

    items: [{
        xtype: 'fieldset',
        padding: '10 20',
        width: 500,
        title: 'Wi-Fi Authentication (RADIUS server)'.t(),
        items: [{
            xtype: 'checkbox',
            reference: 'externalAccess',
            padding: '5 0',
            boxLabel: 'Enable external access point authentication'.t(),
            bind: {
                value: '{systemSettings.radiusServerEnabled}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'RADIUS password'.t(),
            labelWidth: 120,
            width: '100%',
            allowBlank: false,
            bind: {
                value: '{systemSettings.radiusServerSecret}',
                disabled: '{!externalAccess.checked}'
            }
        }, {
            xtype: 'button',
            iconCls: 'fa fa-cog',
            text: 'Configure Server Certificate'.t(),
            margin: '10, 0',
            bind: {
                disabled: '{!systemSettings.radiusServerEnabled}',
            },
            handler: 'configureCertificate'
        }]
    }]

});

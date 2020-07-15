Ext.define('Ung.config.local-directory.view.RadiusServer', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-local-directory-radius-server',
    itemId: 'radius-server',
    title: 'RADIUS Server',
    scrollable: true,
    viewModel: true,

    bodyPadding: 10,

    items: [{
        xtype: 'fieldset',
        padding: '10 20',
        itemId: 'radius-server',
        width: 600,
        title: 'Wi-Fi Authentication (RADIUS Server)'.t(),
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

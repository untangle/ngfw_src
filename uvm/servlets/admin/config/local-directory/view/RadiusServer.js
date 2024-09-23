Ext.define('Ung.config.local-directory.view.RadiusServer', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-local-directory-radius-server',
    itemId: 'radius-server',
    title: 'RADIUS Server',
    scrollable: true,
    viewModel: true,
    withValidation: true,
    bodyPadding: 10,

    items: [{
        xtype: 'component',
        margin: '0 0 10 0',
        html: 'The RADIUS Server can be enabled to allow wireless clients of 802.1x network access points to authenticate with their Local Directory username and password.'.t()
    }, {
        xtype: 'fieldset',
        padding: '10 20',
        itemId: 'radius-server',
        width: 600,
        title: 'Wi-Fi Authentication'.t(),
        items: [{
            xtype: 'checkbox',
            reference: 'externalAccess',
            padding: '5 0',
            boxLabel: 'Enable external access point authentication'.t(),
            bind: {
                value: '{systemSettings.radiusServerEnabled}'
            },
            listeners: {
                change: {
                    fn: function(box,nval,oval,opts) {
                        if (nval === false) {
                            vm = box.ownerCt.ownerCt.getViewModel();
                            vm.set('systemSettings.radiusProxyEnabled', false);
                        }
                    }
                }
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'RADIUS password'.t(),
            inputType: 'password',
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

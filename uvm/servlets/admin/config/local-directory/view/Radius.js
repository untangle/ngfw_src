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
        itemId: 'radius-server',
        width: 600,
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
    }, {
        xtype: 'fieldset',
        padding: '10 20',
        itemId: 'radius-proxy',
        width: 600,
        title: 'Active Directory (RADIUS Proxy)'.t(),
        items: [{
            xtype: 'checkbox',
            reference: 'activeProxy',
            padding: '5 0',
            boxLabel: 'Enable Active Directory Proxy'.t(),
            bind: {
                value: '{systemSettings.radiusProxyEnabled}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'AD Server'.t(),
            labelWidth: 120,
            width: '100%',
            allowBlank: false,
            bind: {
                value: '{systemSettings.radiusProxyServer}',
                disabled: '{!activeProxy.checked}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'AD Workgroup'.t(),
            labelWidth: 120,
            width: '100%',
            allowBlank: false,
            bind: {
                value: '{systemSettings.radiusProxyWorkgroup}',
                disabled: '{!activeProxy.checked}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'AD Domain'.t(),
            labelWidth: 120,
            width: '100%',
            allowBlank: false,
            bind: {
                value: '{systemSettings.radiusProxyRealm}',
                disabled: '{!activeProxy.checked}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'AD Admin Username'.t(),
            labelWidth: 120,
            width: '100%',
            allowBlank: false,
            bind: {
                value: '{systemSettings.radiusProxyUsername}',
                disabled: '{!activeProxy.checked}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'AD Admin Password'.t(),
            labelWidth: 120,
            width: '100%',
            allowBlank: false,
            bind: {
                value: '{systemSettings.radiusProxyPassword}',
                disabled: '{!activeProxy.checked}'
            }
        }, {
            xtype: 'button',
            iconCls: 'fa fa-link',
            text: 'Create AD Computer Account',
            margin: '10, 0',
            bind: {
                disabled: '{!systemSettings.radiusProxyEnabled}',
            },
            handler: 'createComputerAccount'
        }, {
            xtype: 'button',
            iconCls: 'fa fa-refresh',
            text: 'Refresh AD Account Status',
            margin: '10, 10',
            bind: {
                disabled: '{!systemSettings.radiusProxyEnabled}',
            },
            target: 'radiusProxyStatus',
            handler: 'refreshRadiusProxyStatus'
        }, {
            xtype: 'textarea',
            fieldLabel: 'AD Account Status'.t(),
            labelWidth: 120,
            width: '100%',
            allowBlank: true,
            readOnly: true,
            bind: {
                disabled: '{!activeProxy.checked}'
            }
        }]
    }, {
        xtype: 'fieldset',
        padding: '10 20',
        itemId: 'radius-log',
        width: '100%',
        title: 'RADIUS Server Log'.t(),
        items: [{
            xtype: 'button',
            iconCls: 'fa fa-refresh',
            text: 'Refresh',
            target: 'radiusLogFile',
            handler: 'refreshRadiusLogFile'
        }, {
            xtype: 'textarea',
            itemId: 'radiusLogFile',
            spellcheck: false,
            padding: '5 0',
            border: true,
            width: '100%',
            height: 500,
            bind: '{radiusLogFile}',
            fieldStyle: {
                'fontFamily'   : 'courier new',
                'fontSize'     : '12px'
            }
        }]
    }]
});

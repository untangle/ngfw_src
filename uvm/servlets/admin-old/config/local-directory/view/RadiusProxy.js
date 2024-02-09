Ext.define('Ung.config.local-directory.view.RadiusProxy', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-local-directory-radius-proxy',
    itemId: 'radius-proxy',
    title: 'RADIUS Proxy',
    scrollable: true,
    viewModel: true,
    withValidation: true,
    bodyPadding: 10,

    items: [{
        xtype: 'component',
        margin: '0 0 10 0',
        html: 'The RADIUS Proxy can be enabled to allow wireless clients to authenticate using account credentials stored in an Active Directory server.'.t()
    },{
        xtype: 'checkbox',
        reference: 'activeProxy',
        padding: '5 0',
        boxLabel: 'Enable Active Directory Proxy'.t(),
        _onFirstChange: true,
        bind: {
            value: '{systemSettings.radiusProxyEnabled}',
            disabled: '{!systemSettings.radiusServerEnabled}'
        },
        listeners: {
            change: 'radiusProxyDirtyFieldsHandler'
        }
    }, {
        xtype: 'fieldset',
        padding: '10 20',
        itemId: 'radius-proxy',
        width: 600,
        title: 'Active Directory Server'.t(),
        items: [{
            xtype: 'textfield',
            fieldLabel: 'AD Server'.t(),
            labelWidth: 120,
            width: '100%',
            allowBlank: false,
            vtype: 'hostName',
            _onFirstChange: true,
            bind: {
                value: '{systemSettings.radiusProxyServer}',
                disabled: '{!activeProxy.checked}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'AD Workgroup'.t(),
            labelWidth: 120,
            width: '100%',
            fieldStyle: 'text-transform:uppercase',
            allowBlank: false,
            _onFirstChange: true,
            bind: {
                value: '{systemSettings.radiusProxyWorkgroup}',
                disabled: '{!activeProxy.checked}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'AD Domain'.t(),
            labelWidth: 120,
            fieldIndex: 'adDomain',
            width: '100%',
            allowBlank: false,
            vtype: 'domainName',
            _onFirstChange: true,
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
            _onFirstChange: true,
            bind: {
                value: '{systemSettings.radiusProxyUsername}',
                disabled: '{!activeProxy.checked}'
            }
        }, {
            xtype: 'fieldcontainer',
            layout: 'hbox',
            width: '100%',
            items: [{
                xtype: 'textfield',
                fieldLabel: 'AD Admin Password'.t(),
                labelWidth: 120,
                width: '96%',
                allowBlank: false,
                inputType: 'password',
                _onFirstChange: true,
                bind: {
                    value: '{systemSettings.radiusProxyPassword}',
                    disabled: '{!activeProxy.checked}'
                },
                listeners: {
                    change: 'radiusProxyDirtyFieldsHandler',
                }
            }, {
                xtype: 'button',
                iconCls: 'fa fa-eye',
                tooltip: 'Show Password',
                handler: 'showOrHideRadiusPassword',
                bind: {
                    disabled: '{!activeProxy.checked}'
                }
            }]
        }],
        defaults: {
            listeners: {
                change: 'radiusProxyDirtyFieldsHandler',
            }
        },
    }, {
        xtype: 'fieldset',
        padding: '10 20',
        itemId: 'radius-test',
        width: 600,
        title: 'Active Directory Test'.t(),
        disabled: true,
        items: [{
            xtype: 'textfield',
            fieldLabel: 'Test Username'.t(),
            fieldIndex: 'testUsername',
            labelWidth: 120,
            width: '100%',
            allowBlank: true,
            _neverDirty: true,
        }, {
            xtype: 'fieldcontainer',
            layout: 'hbox',
            width: '100%',
            items: [{
                xtype: 'textfield',
                fieldLabel: 'Test Password'.t(),
                fieldIndex: 'testPassword',
                labelWidth: 120,
                width: '96%',
                allowBlank: true,
                _neverDirty: true,
                inputType: 'password',
            }, {
                xtype: 'button',
                iconCls: 'fa fa-eye',
                tooltip: 'Show Password',
                handler: 'showOrHideRadiusPassword',
            }]
        }, {
            xtype: 'button',
            iconCls: 'fa fa-cogs',
            text: 'Test Authentication',
            margin: '10, 0',
            handler: 'testRadiusProxyLogin'
        }]
    },
    {
        xtype: 'fieldset',
        padding: '10 20',
        itemId: 'radius-account',
        width: 600,
        disabled: true,
        title: 'Active Directory Status'.t(),
        items: [{
            xtype: 'button',
            iconCls: 'fa fa-refresh',
            text: 'Refresh AD Account Status',
            margin: '10, 0',
            target: 'radiusProxyStatus',
            handler: 'refreshRadiusProxyStatus'
        }, {
            xtype: 'button',
            iconCls: 'fa fa-link',
            text: 'Create AD Computer Account',
            margin: '10, 10',
            handler: 'createComputerAccount'
        }, {
            xtype: 'textarea',
            labelWidth: 120,
            width: '100%',
            allowBlank: true,
            readOnly: true,
        }]
    }]
});

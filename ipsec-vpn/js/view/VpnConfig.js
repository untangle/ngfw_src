Ext.define('Ung.apps.ipsecvpn.view.VpnConfig', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ipsec-vpn-vpnconfig',
    itemId: 'vpn-config',
    title: 'VPN Config'.t(),
    scrollable: true,

    viewModel: {
        formulas: {
           _btnConfigureDirectory: function (get) {
                switch (get('settings.authenticationType')) {
                case 'LOCAL_DIRECTORY': return 'Configure Local Directory'.t();
                case 'RADIUS_SERVER': return 'Configure RADIUS'.t();
                default: return '';
                }
            }
        }
    },

    bodyPadding: 10,
    withValidation: true,

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'The VPN Config tab contains settings used to configure the server to support IPsec L2TP, Xauth, and IKEv2 VPN client connections.'.t()
    }],

    items: [{
        xtype: 'checkbox',
        bind: '{settings.vpnflag}',
        fieldLabel: 'Enable L2TP/Xauth/IKEv2 Server'.t(),
        labelWidth: 200
    },{
        xtype: 'fieldset',
        margin: '10 10 10 -10',
        border: 0,
        hidden: true,
        disabled: true,
        bind: {
            hidden: '{settings.vpnflag == false}',
            disabled: '{settings.vpnflag == false}'
        },

        defaults:{
            labelWidth: 200
        },

        items: [{
            xtype: 'textfield',
            bind: '{settings.virtualAddressPool}',
            vtype: 'cidrBlock',
            allowBlank: false,
            fieldLabel: 'L2TP Address Pool'.t()
        },{
            xtype: 'textfield',
            bind: '{settings.virtualXauthPool}',
            vtype: 'cidrBlock',
            allowBlank: false,
            fieldLabel: 'Xauth/IKEv2 Address Pool'.t()
        },{
            xtype: 'textfield',
            bind: '{settings.virtualDnsOne}',
            vtype: 'ipAddress',
            allowBlank: true,
            fieldLabel: 'Custom DNS Server 1'.t()
        },{
            xtype: 'textfield',
            bind: '{settings.virtualDnsTwo}',
            vtype: 'ipAddress',
            allowBlank: true,
            fieldLabel: 'Custom DNS Server 2'.t()
        },{
            xtype: 'displayfield',
            value:  '<STRONG>' + 'NOTE: Leave the Custom DNS fields empty to have clients automatically configured to use this server for DNS resolution.'.t() + '</STRONG>'
        },{
            xtype: 'textfield',
            bind: '{settings.virtualSecret}',
            allowBlank: false,
            width: 600,
            fieldLabel: 'IPsec Secret'.t()
        },{
            xtype: 'checkbox',
            bind: '{settings.allowConcurrentLogins}',
            fieldLabel: 'Allow Concurrent Logins',
        }, {
            xtype: 'container',
            layout: 'column',
            margin: '0 0 5 0',
            items: [{
                xtype:'checkbox',
                bind: '{settings.phase1Manual}',
                fieldLabel: 'Phase 1 IKE/ISAKMP Manual Configuration'.t(),
                labelWidth: 300
            }]
        }, {
            xtype: 'container',
            layout: 'column',
            margin: '0 0 5 40',
            bind: { hidden: '{!settings.phase1Manual}' },
            items: [{
                xtype:'combobox',
                bind: {
                    value: '{settings.phase1Cipher}',
                    store: '{P1CipherStore}'
                },
                fieldLabel: 'Encryption'.t(),
                labelWidth: 120,
                width: 350,
                editable: false,
                displayField: 'name',
                valueField: 'value'
            }, {
                xtype: 'displayfield',
                margin: '0 0 0 10',
                value: 'Default = 3DES'.t()
            }]
        }, {
            xtype: 'container',
            layout: 'column',
            margin: '0 0 5 40',
            bind: { hidden: '{!settings.phase1Manual}' },
            items: [{
                xtype:'combobox',
                bind: {
                    value: '{settings.phase1Hash}',
                    store: '{P1HashStore}'
                },
                fieldLabel: 'Hash'.t(),
                labelWidth: 120,
                editable: false,
                displayField: 'name',
                valueField: 'value'
            }, {
                xtype: 'displayfield',
                margin: '0 0 0 10',
                value: 'Default = MD5'.t()
            }]
        }, {
            xtype: 'container',
            layout: 'column',
            margin: '0 0 5 40',
            bind: { hidden: '{!settings.phase1Manual}' },
            items: [{
                xtype:'combobox',
                bind: {
                    value: '{settings.phase1Group}',
                    store: '{P1GroupStore}'
                },
                fieldLabel: 'DH Key Group'.t(),
                labelWidth: 120,
                editable: false,
                displayField: 'name',
                valueField: 'value'
            }, {
                xtype: 'displayfield',
                margin: '0 0 0 10',
                value: 'Default = 14 (modp2048)'.t()
            }]
        }, {
            xtype: 'container',
            layout: 'column',
            margin: '0 0 5 40',
            bind: { hidden: '{!settings.phase1Manual}' },
            items: [{
                xtype:'numberfield',
                bind: {
                    value: '{settings.phase1Lifetime}',
                },
                fieldLabel: 'Lifetime'.t(),
                labelWidth: 120,
                allowBlank: false,
                allowDecimals: false,
                minValue: 3600,
                maxValue: 86400
            }, {
                xtype: 'displayfield',
                margin: '0 0 0 10',
                value: 'Default = 28800 seconds, min = 3600, max = 86400'.t()
            }]
        }, {
            xtype: 'container',
            layout: 'column',
            margin: '0 0 5 0',
            items: [{
                xtype:'checkbox',
                bind: '{settings.phase2Manual}',
                fieldLabel: 'Phase 2 ESP Manual Configuration'.t(),
                labelWidth: 300
            }]
        }, {
            xtype: 'container',
            layout: 'column',
            margin: '0 0 5 40',
            bind: { hidden: '{!settings.phase2Manual}' },
            items: [{
                xtype:'combobox',
                bind: {
                    value: '{settings.phase2Cipher}',
                    store: '{P2CipherStore}'
                },
                fieldLabel: 'Encryption'.t(),
                labelWidth: 120,
                width: 350,
                editable: false,
                displayField: 'name',
                valueField: 'value'
            }, {
                xtype: 'displayfield',
                margin: '0 0 0 10',
                value: 'Default = 3DES'.t()
            }]
        }, {
            xtype: 'container',
            layout: 'column',
            margin: '0 0 5 40',
            bind: { hidden: '{!settings.phase2Manual}' },
            items: [{
                xtype:'combobox',
                bind: {
                    value: '{settings.phase2Hash}',
                    store: '{P2HashStore}'
                },
                fieldLabel: 'Hash'.t(),
                labelWidth: 120,
                editable: false,
                displayField: 'name',
                valueField: 'value'
            }, {
                xtype: 'displayfield',
                margin: '0 0 0 10',
                value: 'Default = MD5'.t()
            }]
        }, {
            xtype: 'container',
            layout: 'column',
            margin: '0 0 5 40',
            bind: { hidden: '{!settings.phase2Manual}' },
            items: [{
                xtype:'combobox',
                bind: {
                    value: '{settings.phase2Group}',
                    hidden: '{!settings.phase2Manual}',
                    store: '{P2GroupStore}'
                },
                fieldLabel: 'PFS Key Group'.t(),
                labelWidth: 120,
                editable: false,
                displayField: 'name',
                valueField: 'value'
            }, {
                xtype: 'displayfield',
                margin: '0 0 0 10',
                value: 'Default = 14 (modp2048)'.t()
            }]
        }, {
            xtype: 'container',
            layout: 'column',
            margin: '0 0 5 40',
            bind: { hidden: '{!settings.phase2Manual}' },
            items: [{
                xtype:'numberfield',
                bind: {
                    value: '{settings.phase2Lifetime}',
                    hidden: '{!settings.phase2Manual}'
                },
                fieldLabel: 'Lifetime'.t(),
                labelWidth: 120,
                allowBlank: false,
                allowDecimals: false,
                minValue: 3600,
                maxValue: 86400
            }, {
                xtype: 'displayfield',
                margin: '0 0 0 10',
                value: 'Default = 3600 seconds, min = 3600, max = 86400'.t()
            }]
        },{
            xtype: 'radiogroup',
            bind: '{settings.authenticationType}',
            simpleValue: 'true',
            columns: 1,
            vertical: true,
            items: [
                { boxLabel: '<strong>' + 'Local Directory'.t() + '</strong>', inputValue: 'LOCAL_DIRECTORY' },
                { boxLabel: '<strong>' + 'RADIUS'.t() + '</strong> (' + 'requires'.t() + ' Directory Connector)', inputValue: 'RADIUS_SERVER' }
            ]
        },{
            xtype: 'button',
            iconCls: 'fa fa-cog',
            width: 200,
            bind: {
                text: '{_btnConfigureDirectory}',
            },
                handler: 'configureAuthTarget'
        },{
            xtype: 'fieldset',
            margin: '20 0 0 0',
            title: 'Server Listen Addresses'.t(),
            padding: 10,

            layout: {
                type: 'vbox',
                align: 'stretch'
            },

            items: [{
                xtype: 'ungrid',
                tbar: ['@addInline'],
                recordActions: ['delete'],
                listProperty: 'settings.virtualListenList.list',

                emptyText: 'No Server Listen Addresses Defined'.t(),

                emptyRow: {
                    javaClass: 'com.untangle.app.ipsec_vpn.VirtualListen',
                    'address': ''
                },

                bind: '{listenList}',

                columns: [{
                    header: 'Address'.t(),
                    dataIndex: 'address',
                    width: Renderer.ipWidth,
                    flex: 1,
                    editor: {
                        bind: '{record.address}',
                        xtype: 'textfield',
                        vtype: 'ipAddress'
                    }
                }]
            }]
        }]
    }]
});

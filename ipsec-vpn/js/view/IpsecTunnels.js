Ext.define('Ung.apps.ipsecvpn.view.IpsecTunnels', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-ipsec-vpn-ipsectunnels',
    itemId: 'ipsec-tunnels',
    title: 'IPsec Tunnels'.t(),
    viewModel: true,
    scrollable: true,
    withValidation: false,
    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'The IPsec Tunnels tab is used to create and manage IPsec tunnels'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    emptyText: 'No IPSec Tunnels defined'.t(),

    recordActions: ['edit', 'copy', 'delete'],
    copyAppendField: 'description',

    listProperty: 'settings.tunnels.list',
    emptyRow: {
        javaClass: 'com.untangle.app.ipsec_vpn.IpsecVpnTunnel',
        'active': true,
        'ikeVersion': 2,
        'conntype': 'tunnel',
        'runmode': 'start',
        'dpddelay': '30',
        'dpdtimeout': '120',
        'phase1Manual': true,
        'phase1Cipher': 'aes256',
        'phase1Hash': 'sha1',
        'phase1Group': 'modp2048',
        'phase1Lifetime' : '28800',
        'phase2Manual': true,
        'phase2Cipher': 'aes256gcm128',
        'phase2Hash': 'sha1',
        'phase2Group': 'modp2048',
        'phase2Lifetime' : '3600',
        'allSubnetNegotation': true,
        'left': [['Ung.util.Util.getAppStorageValue'],['ipsec.leftDefault']],
        'leftId': '',
        'leftSourceIp': '',
        'leftSubnet': [['Ung.util.Util.getAppStorageValue'],['ipsec.leftSubnetDefault']],
        'right': '',
        'rightId': '',
        'rightSourceIp': '',
        'rightSubnet': '',
        'description': '',
        'secret': '',
        'localInterface': 0,
        'rightAny': false,
        'leftSourceIpAny': false,
        'pingAddress': ''
        },

    importValidationJavaClass: true,
    importValidationForComboBox: true,

    bind: '{tunnelList}',

    columns: [{
        xtype: 'checkcolumn',
        header: 'Enabled'.t(),
        width: Renderer.booleanWidth,
        dataIndex: 'active',
        resizable: false
    }, {
        header: 'External IP'.t(),
        width: 200,
        dataIndex: 'left',
        renderer: Ung.apps.ipsecvpn.Main.leftRenderer
    }, {
        header: 'Remote Host'.t(),
        width: Renderer.hostnameWidth,
        dataIndex: 'right',
        renderer: Ung.apps.ipsecvpn.Main.rightRenderer
    }, {
        header: 'Local Source IP Address'.t(),
        width: 200,
        dataIndex: 'leftSourceIp',
        renderer: Ung.apps.ipsecvpn.Main.sourceRenderer
    }, {
        header: 'Local Network'.t(),
        width: Renderer.networkWidth,
        dataIndex: 'leftSubnet',
    }, {
        header: 'Remote Source IP Address'.t(),
        width: 200,
        dataIndex: 'rightSourceIp',
        renderer: Ung.apps.ipsecvpn.Main.sourceRenderer
    }, {
        header: 'Remote Network'.t(),
        width: Renderer.networkWidth,
        dataIndex: 'rightSubnet',
    }, {
        header: 'Description'.t(),
        width: Renderer.messageWidth,
        dataIndex: 'description',
        flex: 1,
    }],

    editorHeight: Ext.getBody().getViewSize().height - 60,
    editorWidth: Ext.getBody().getViewSize().width - 60,

    editorXtype: 'ung.cmp.unipsecvpntunnelsrecordeditor',
    editorFields: [{
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype: 'checkbox',
            bind: '{record.active}',
            fieldLabel: 'Enabled'.t(),
            labelWidth: 180
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype: 'textfield',
            bind: '{record.description}',
            fieldLabel: 'Description'.t(),
            labelWidth: 180,
            allowBlank: false,
            width: 500
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype:'combobox',
            fieldLabel: 'Connection Type'.t(),
            labelWidth: 180,
            editable: false,
            width: 400,
            bind: '{record.conntype}',
            store: [['tunnel','Tunnel'],['transport','Transport']]
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: 'We recommended selecting <B>Tunnel</B> unless you have a specific reason to use <B>Transport</B>'.t()
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype:'combobox',
            fieldLabel: 'IKE Version'.t(),
            labelWidth: 180,
            editable: false,
            bind: '{record.ikeVersion}',
            store: [[1,'IKEv1'],[2,'IKEv2']]
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: 'Both sides of the tunnel must be configured to use the same IKE version.'.t()
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype:'combobox',
            fieldLabel: 'Connect Mode'.t(),
            labelWidth: 180,
            editable: false,
            bind: {
                disabled: '{record.rightAny == true}',
                value: '{record.runmode}',
            },
            store: [['start','Always Connected'],['route','On Demand']]
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype: 'combobox',
            fieldLabel: 'Interface'.t(),
            labelWidth: 180,
            bind: {
                store: '{wanListStore}',
                value: '{record.localInterface}'
            },
            allowblank: true,
            editable: false,
            queryMode: 'local',
            displayField: 'name',
            valueField: 'index',
            listeners: {
                change: 'interfaceChange'
            }
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype: 'textfield',
            itemId: 'externalAddress',
            bind: {
                value: '{record.left}',
                hidden: '{record.localInterface != 0}'
            },
            fieldLabel: 'Local Address'.t(),
            fieldIndex: 'externalAddress',
            labelWidth: 180,
            allowBlank: false,
            // vtype: 'ipAddress'
        },{
            xtype: 'displayfield',
            fieldLabel: 'Local Address'.t(),
            labelWidth: 180,
            itemId: 'externalAddressCurrent',
            value: '',
            bind: {
                hidden: '{record.localInterface == 0}'
            },
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: 'WAN IP address of this server'.t()
        }]
    },{
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype:'checkbox',
            bind: '{record.rightAny}',
            fieldLabel: 'Any Remote Host'.t(),
            labelWidth: 180,
            listeners: {
                change: 'anyRemoteHostChange'
            }
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: 'Allow connections from any IP address'.t()
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        bind: {
            hidden: '{record.rightAny == true}',
            disabled: '{record.rightAny == true}'
        },
        items: [{
            xtype: 'textfield',
        bind: {
            value: '{record.right}',
            hidden: '{record.rightAny == true}',
            disabled: '{record.rightAny == true}'
        },
            fieldLabel: 'Remote Host'.t(),
            labelWidth: 180,
            allowBlank: false,
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: 'The public hostname or IP address of the remote IPsec gateway'.t()
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype: 'textfield',
            bind: '{record.leftId}',
            fieldLabel: 'Local Identifier'.t(),
            labelWidth: 180
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: 'The authentication ID of the local IPsec gateway. Default = same as <B>External IP</B>'.t()
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        bind: {
            hidden: '{record.rightAny == true}',
            disabled: '{record.rightAny == true}'
        },
        items: [{
            xtype: 'textfield',
            bind: '{record.rightId}',
            fieldLabel: 'Remote Identifier'.t(),
            labelWidth: 180
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: 'The authentication ID of the remote IPsec gateway. Default = same as <B>Remote Host</B>'.t()
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype:'checkbox',
            bind: "{record.allSubnetNegotation}",
            fieldLabel: 'Full Tunnel Mode Negotiation'.t(),
            labelWidth: 180
        }]
    },{
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype:'checkbox',
            bind: '{record.leftSourceIpAny}',
            fieldLabel: 'Local Source IP Address'.t(),
            labelWidth: 180,
            listeners: {
                change: 'anyLeftSourceChange'
            }
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: 'Request From Peer'.t()
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        bind: {
            hidden: '{record.leftSourceIpAny == true}',
            disabled: '{record.leftSourceIpAny == true}'
        },
        items: [{
            xtype:'textfield',
            bind: "{record.leftSourceIp}",
            fieldLabel: '&nbsp;',
            labelSeparator: '',
            labelWidth: 180,
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype: 'textfield',
            bind: {
                value: '{record.leftSubnet}',
                hidden: '{record.ikeVersion !== 1}',
                disabled: '{record.ikeVersion !== 1}'
            },
            fieldLabel: 'Local Network'.t(),
            labelWidth: 180,
            width: 500,
            allowBlank: false,
            vtype: 'cidrBlock'
        }, {
            xtype: 'textfield',
            bind: {
                value: '{record.leftSubnet}',
                hidden: '{record.ikeVersion !== 2}',
                disabled: '{record.ikeVersion !== 2}'
            },
            fieldLabel: 'Local Network'.t(),
            labelWidth: 180,
            width: 500,
            allowBlank: false,
            vtype: 'cidrBlockList'
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: 'The private network attached to the local side of the tunnel'.t(),
            bind: { hidden: '{record.ikeVersion !== 1}' }
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: 'The private networks attached to the local side of the tunnel'.t(),
            bind: { hidden: '{record.ikeVersion !== 2}' }
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        bind: {
            hidden: '{record.rightAny == true}',
            disabled: '{record.rightAny == true}'
        },
        items: [{
            xtype:'textfield',
            bind: "{record.rightSourceIp}",
            fieldLabel: 'Remote Source IP Address'.t(),
            labelWidth: 180,
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        bind: {
            hidden: '{record.rightAny == true}',
            disabled: '{record.rightAny == true}'
        },
        items: [{
            xtype: 'textfield',
            bind: {
                value: '{record.rightSubnet}',
                hidden: '{record.ikeVersion !== 1 || record.rightAny == true}',
                disabled: '{record.ikeVersion !== 1 || record.rightAny == true}'
            },
            fieldLabel: 'Remote Network'.t(),
            labelWidth: 180,
            width: 500,
            allowBlank: false,
            vtype: 'cidrBlock'
        }, {
            xtype: 'textfield',
            bind: {
                value: '{record.rightSubnet}',
                hidden: '{record.ikeVersion !== 2 || record.rightAny == true}',
                disabled: '{record.ikeVersion !== 2 || record.rightAny == true}'
            },
            fieldLabel: 'Remote Network'.t(),
            labelWidth: 180,
            width: 500,
            allowBlank: false,
            vtype: 'cidrBlockList'
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: 'The private network attached to the remote side of the tunnel'.t(),
            bind: { hidden: '{record.ikeVersion !== 1}' }
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: 'The private networks attached to the remote side of the tunnel'.t(),
            bind: { hidden: '{record.ikeVersion !== 2}' }
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype: 'textfield',
            bind: '{record.secret}',
            fieldLabel: 'Shared Secret'.t(),
            labelWidth: 180,
            width: 700,
            allowBlank: false
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        bind: {
            hidden: '{record.rightAny == true}',
            disabled: '{record.rightAny == true}'
        },
        items: [{
            xtype:'numberfield',
            bind: '{record.dpddelay}',
            fieldLabel: 'DPD Interval'.t(),
            labelWidth: 180,
            allowBlank: false,
            allowDecimals: false,
            minValue: 0,
            maxValue: 3600
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: 'The number of seconds between R_U_THERE messages.  Enter 0 to disable.'.t()
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        bind: {
            hidden: '{record.rightAny == true}',
            disabled: '{record.rightAny == true}'
        },
        items: [{
            xtype:'numberfield',
            bind: '{record.dpdtimeout}',
            fieldLabel: 'DPD Timeout'.t(),
            labelWidth: 180,
            allowBlank: false,
            allowDecimals: false,
            minValue: 0,
            maxValue: 3600
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: 'The number of seconds for a dead peer tunnel to be restarted'.t()
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        bind: {
            hidden: '{record.rightAny == true}',
            disabled: '{record.rightAny == true}'
        },
        items: [{
            xtype: 'textfield',
            bind: '{record.pingAddress}',
            fieldLabel: 'Ping Address'.t(),
            vtype: 'ipAddress',
            labelWidth: 180
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: 'An IP address on the remote network to ping for connectivity verification. Leave blank to disable.'.t()
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype:'checkbox',
            bind: '{record.phase1Manual}',
            fieldLabel: 'Phase 1 IKE/ISAKMP Manual Configuration'.t(),
            labelWidth: 300
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 40',
        bind: { hidden: '{!record.phase1Manual}' },
        items: [{
            xtype:'combobox',
            bind: {
                value: '{record.phase1Cipher}',
                store: '{P1CipherStore}'
            },
            fieldLabel: 'Encryption'.t(),
            labelWidth: 180,
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
        bind: { hidden: '{!record.phase1Manual}' },
        items: [{
            xtype:'combobox',
            bind: {
                value: '{record.phase1Hash}',
                store: '{P1HashStore}'
            },
            fieldLabel: 'Hash'.t(),
            labelWidth: 180,
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
        bind: { hidden: '{!record.phase1Manual}' },
        items: [{
            xtype:'combobox',
            bind: {
                value: '{record.phase1Group}',
                store: '{P1GroupStore}'
            },
            fieldLabel: 'DH Key Group'.t(),
            labelWidth: 180,
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
        bind: { hidden: '{!record.phase1Manual}' },
        items: [{
            xtype:'numberfield',
            bind: {
                value: '{record.phase1Lifetime}',
            },
            fieldLabel: 'Lifetime'.t(),
            labelWidth: 180,
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
            bind: '{record.phase2Manual}',
            fieldLabel: 'Phase 2 ESP Manual Configuration'.t(),
            labelWidth: 300
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 40',
        bind: { hidden: '{!record.phase2Manual}' },
        items: [{
            xtype:'combobox',
            bind: {
                value: '{record.phase2Cipher}',
                store: '{P2CipherStore}'
            },
            fieldLabel: 'Encryption'.t(),
            labelWidth: 180,
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
        bind: { hidden: '{!record.phase2Manual}' },
        items: [{
            xtype:'combobox',
            bind: {
                value: '{record.phase2Hash}',
                store: '{P2HashStore}'
            },
            fieldLabel: 'Hash'.t(),
            labelWidth: 180,
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
        bind: { hidden: '{!record.phase2Manual}' },
        items: [{
            xtype:'combobox',
            bind: {
                value: '{record.phase2Group}',
                hidden: '{!record.phase2Manual}',
                store: '{P2GroupStore}'
            },
            fieldLabel: 'PFS Key Group'.t(),
            labelWidth: 180,
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
        bind: { hidden: '{!record.phase2Manual}' },
        items: [{
            xtype:'numberfield',
            bind: {
                value: '{record.phase2Lifetime}',
                hidden: '{!record.phase2Manual}'
            },
            fieldLabel: 'Lifetime'.t(),
            labelWidth: 180,
            allowBlank: false,
            allowDecimals: false,
            minValue: 3600,
            maxValue: 86400
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: 'Default = 3600 seconds, min = 3600, max = 86400'.t()
        }]
    }]
});

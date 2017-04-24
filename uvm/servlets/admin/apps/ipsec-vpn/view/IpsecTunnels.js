Ext.define('Ung.apps.ipsecvpn.view.IpsecTunnels', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-ipsec-vpn-ipsectunnels',
    itemId: 'ipsec_tunnels',
    title: 'IPsec Tunnels'.t(),
    viewModel: true,

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

    recordActions: ['edit', 'delete'],
    listProperty: 'settings.tunnels.list',
    emptyRow: {
        javaClass: 'com.untangle.app.ipsec_vpn.IpsecVpnTunnel',
        'active': true,
        'ikeVersion': 1,
        'conntype': 'tunnel',
        'runmode': 'start',
        'dpddelay': '30',
        'dpdtimeout': '120',
        'phase1Cipher': '3des',
        'phase1Hash': 'md5',
        'phase1Group': 'modp1024',
        'phase1Lifetime' : '28800',
        'phase2Cipher': '3des',
        'phase2Hash': 'md5',
        'phase2Group': 'modp1024',
        'phase2Lifetime' : '3600',
        'left': '', // FIXME - this should use vm.get('leftDefault')
        'leftId': '',
        'leftSubnet': '', // FIXME - this should use vm.get('leftSubnetDefault')
        'right': '',
        'rightId': '',
        'rightSubnet': '',
        'description': '',
        'secret': ''
        },

    bind: '{tunnelList}',

    columns: [{
        xtype: 'checkcolumn',
        header: 'Enabled'.t(),
        width: 80,
        dataIndex: 'active',
        resizable: false
    }, {
        header: 'Local IP'.t(),
        width: 150,
        dataIndex: 'left',
    }, {
        header: 'Remote Host'.t(),
        width: 150,
        dataIndex: 'right',
    }, {
        header: 'Local Network'.t(),
        width: 200,
        dataIndex: 'leftSubnet',
    }, {
        header: 'Remote Network'.t(),
        width: 200,
        dataIndex: 'rightSubnet',
    }, {
        header: 'Description'.t(),
        width:300,
        dataIndex: 'description',
        flex: 1,
    }],

    editorHeight: Ext.getBody().getViewSize().height - 60,
    editorWidth: Ext.getBody().getViewSize().width - 60,

    editorFields: [{
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype: 'checkbox',
            bind: '{record.active}',
            fieldLabel: 'Enabled'.t(),
            labelWidth: 120
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype: 'textfield',
            bind: '{record.description}',
            fieldLabel: 'Description'.t(),
            labelWidth: 120,
            allowBlank: false,
            width: 500
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype:'combo',
            fieldLabel: 'Connection Type'.t(),
            labelWidth: 120,
            editable: false,
            width: 400,
            bind: '{record.conntype}',
            store: [['tunnel','Tunnel'],['transport','Transport']]
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: '(We recommended selecting <B>Tunnel</B> unless you have a specific reason to use <B>Transport</B>)'.t()
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype:'combo',
            fieldLabel: 'IKE Version'.t(),
            labelWidth: 120,
            editable: false,
            bind: '{record.ikeVersion}',
            store: [['1','IKEv1'],['2','IKEv2']]
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
            xtype:'combo',
            fieldLabel: 'Connect Mode'.t(),
            labelWidth: 120,
            editable: false,
            bind: '{record.runmode}',
            store: [['start','Always Connected'],['add','On Demand']]
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype: 'combo',
            fieldLabel: 'Interface'.t(),
            labelWidth: 120,
            bind: {
                store: '{wanListStore}'
            },
            allowblank: true,
            editable: false,
            queryMode: 'local',
            displayField: 'name',
            valueField: 'address'
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype: 'textfield',
            bind: '{record.left}',
            fieldLabel: 'External IP'.t(),
            labelWidth: 120,
            allowBlank: false,
            vtype: 'ipAddress'
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: '(The external IP address of this server)'.t()
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype: 'textfield',
            bind: '{record.right}',
            fieldLabel: 'Remote Host'.t(),
            labelWidth: 120,
            allowBlank: false,
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: '(The public hostname or IP address of the remote IPsec gateway)'.t()
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype: 'textfield',
            bind: '{record.leftId}',
            fieldLabel: 'Local Identifier'.t(),
            labelWidth: 120
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: '(The authentication ID of the local IPsec gateway. Default = same as <B>External IP</B>)'.t()
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype: 'textfield',
            bind: '{record.rightId}',
            fieldLabel: 'Remote Identifier'.t(),
            labelWidth: 120
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: '(The authentication ID of the remote IPsec gateway. Default = same as <B>Remote Host</B>)'.t()
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype: 'textfield',
            bind: '{record.leftSubnet}',
            fieldLabel: 'Local Network'.t(),
            labelWidth: 120,
            allowBlank: false,
            vtype: 'cidrBlock'
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: '(The private network attached to the local side of the tunnel)'.t()
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype: 'textfield',
            bind: '{record.rightSubnet}',
            fieldLabel: 'Remote Network'.t(),
            labelWidth: 120,
            allowBlank: false,
            vtype: 'cidrBlock'
        }, {
            xtype: 'displayfield',
            margin: '0 0 0 10',
            value: '(The private network attached to the remote side of the tunnel)'.t()
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype: 'textfield',
            bind: '{record.secret}',
            fieldLabel: 'Shared Secret'.t(),
            labelWidth: 120,
            width: 700,
            allowBlank: false
        }]
    }, {
        xtype: 'container',
        layout: 'column',
        margin: '0 0 5 0',
        items: [{
            xtype:'numberfield',
            bind: '{record.dpddelay}',
            fieldLabel: 'DPD Interval'.t(),
            labelWidth: 120,
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
        items: [{
            xtype:'numberfield',
            bind: '{record.dpdtimeout}',
            fieldLabel: 'DPD Timeout'.t(),
            labelWidth: 120,
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
            labelWidth: 120,
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
        bind: { hidden: '{!record.phase1Manual}' },
        items: [{
            xtype:'combobox',
            bind: {
                value: '{record.phase1Group}',
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
            value: 'Default = 2 (1024 bit)'.t()
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
            labelWidth: 120,
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
        bind: { hidden: '{!record.phase2Manual}' },
        items: [{
            xtype:'combobox',
            bind: {
                value: '{record.phase2Group}',
                hidden: '{!record.phase2Manual}',
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
            value: 'Default = 2 (1024 bit)'.t()
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
    }]
});

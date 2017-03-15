Ext.define('Ung.apps.ipsecvpn.view.IpsecTunnels', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-ipsec-vpn-ipsectunnels',
    itemId: 'ipsectunnels',
    title: 'IPsec Tunnels'.t(),

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
        javaClass: 'com.untangle.node.ipsec_vpn.IpsecVpnTunnel',
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
        'left': Ung.apps.ipsecvpn.Data.leftDefault,
        'leftId': '',
        'leftSubnet': Ung.apps.ipsecvpn.Data.leftSubnetDefault,
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
        flex: 1,
        dataIndex: 'description',
    }],

    editorFields: [{
        xtype: 'checkbox',
        bind: '{record.active}',
        fieldLabel: 'Enabled'.t()
    }, {
        xtype: 'textfield',
        bind: '{record.description}',
        fieldLabel: 'Description'.t()
    }, {
        xtype:'combo',
        fieldLabel: 'Connection Type'.t(),
        editable: false,
        bind: '{record.conntype}',
        store: [['tunnel','Tunnel'],['transport','Transport']]
    }, {
        xtype:'combo',
        fieldLabel: 'IKE Version'.t(),
        editable: false,
        bind: '{record.ikeVersion}',
        store: [['1','IKEv1'],['2','IKEv2']],
    }, {
        xtype:'combo',
        fieldLabel: 'Connect Mode'.t(),
        editable: false,
        bind: '{record.runmode}',
        store: [['start','Always Connected'],['add','On Demand']]
    }, {
        xtype: 'combo',
        fieldLabel: 'Interface'.t(),
        editable: false,
//        store: Ung.apps.ipsecvpn.Data.wanList,
    }, {
        xtype: 'textfield',
        bind: '{record.left}',
        fieldLabel: 'Local IP'.t(),
    }, {
        xtype: 'textfield',
        bind: '{record.right}',
        fieldLabel: 'Remote Host'.t(),
    }, {
        xtype: 'textfield',
        bind: '{record.leftId}',
        fieldLabel: 'Local Identifier'.t()
    }, {
        xtype: 'textfield',
        bind: '{record.rightId}',
        fieldLabel: 'Remote Identifier'.t()
    }, {
        xtype: 'textfield',
        bind: '{record.leftSubnet}',
        fieldLabel: 'Local Network'.t(),
    }, {
        xtype: 'textfield',
        bind: '{record.rightSubnet}',
        fieldLabel: 'Remote Network'.t(),
    }, {
        xtype: 'textfield',
        bind: '{record.secret}',
        fieldLabel: 'Shared Secret'.t(),
    }, {
        xtype:'numberfield',
        bind: '{record.dpddelay}',
        fieldLabel: 'DPD Interval'.t(),
        allowBlank: false,
        allowDecimals: false,
        minValue: 0,
        maxValue: 3600
    }, {
        xtype:'numberfield',
        bind: '{record.dpdtimeout}',
        fieldLabel: 'DPD Timeout'.t(),
        allowBlank: false,
        allowDecimals: false,
        minValue: 0,
        maxValue: 3600
    }, {
        xtype:'checkbox',
        bind: '{record.phase1Manual}',
        fieldLabel: 'Phase 1 IKE/ISAKMP Manual Configuration'.t(),
    }, {
        xtype:'combobox',
        bind: {
            value: '{record.phase1Cipher}',
            hidden: '{!record.phase1Manual}',
            store: '{P1CipherStore}'
        },
        fieldLabel: 'Encryption'.t(),
        editable: false,
        displayField: 'name',
        valueField: 'value'
    }, {
        xtype:'combobox',
        bind: {
            value: '{record.phase1Hash}',
            hidden: '{!record.phase1Manual}',
            store: '{P1HashStore}'
        },
        fieldLabel: 'Hash'.t(),
        editable: false,
        displayField: 'name',
        valueField: 'value'
    }, {
        xtype:'combobox',
        bind: {
            value: '{record.phase1Group}',
            hidden: '{!record.phase1Manual}',
            store: '{P1GroupStore}'
        },
        fieldLabel: 'DH Key Group'.t(),
        editable: false,
        displayField: 'name',
        valueField: 'value'
    }, {
        xtype:'numberfield',
        bind: {
            value: '{record.phase1Lifetime}',
            hidden: '{!record.phase1Manual}'
        },
        fieldLabel: 'Lifetime'.t(),
        allowBlank: false,
        allowDecimals: false,
        minValue: 3600,
        maxValue: 86400,
    }, {
        xtype:'checkbox',
        bind: '{record.phase2Manual}',
        fieldLabel: 'Phase 2 ESP Manual Configuration'.t(),
    }, {
        xtype:'combobox',
        bind: {
            value: '{record.phase2Cipher}',
            hidden: '{!record.phase2Manual}',
            store: '{P2CipherStore}'
        },
        fieldLabel: 'Encryption'.t(),
        editable: false,
        displayField: 'name',
        valueField: 'value'
    }, {
        xtype:'combobox',
        bind: {
            value: '{record.phase2Hash}',
            hidden: '{!record.phase2Manual}',
            store: '{P2HashStore}'
        },
        fieldLabel: 'Hash'.t(),
        editable: false,
        displayField: 'name',
        valueField: 'value'
    }, {
        xtype:'combobox',
        bind: {
            value: '{record.phase2Group}',
            hidden: '{!record.phase2Manual}',
            store: '{P2GroupStore}'
        },
        fieldLabel: 'PFS Key Group'.t(),
        editable: false,
        displayField: 'name',
        valueField: 'value'
    }, {
        xtype:'numberfield',
        bind: {
            value: '{record.phase2Lifetime}',
            hidden: '{!record.phase2Manual}'
        },
        fieldLabel: 'Lifetime'.t(),
        allowBlank: false,
        allowDecimals: false,
        minValue: 3600,
        maxValue: 86400,
    }]
});

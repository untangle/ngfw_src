Ext.define('Ung.apps.openvpn.view.Server', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-openvpn-server',
    itemId: 'server',
    title: 'Server'.t(),
    viewModel: true,

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'The Server tab is used to configure OpenVPN to operate as a server for remote clients'.t()
    }],

    defaults: {
        labelWidth: 180,
        padding: '0 0 0 10'
    },

    items: [{
        fieldLabel: 'Site Name'.t(),
        xtype: 'textfield',
        bind: '{settings.siteName}',
        allowBlank: false,
        padding: '10 0 0 10'
    },{
        fieldLabel: 'Site URL'.t(),
        xtype: 'displayfield',
        bind: '{getSiteUrl}'
    },{
        fieldLabel: 'Server Enabled'.t(),
        xtype: 'checkbox',
        bind: '{settings.serverEnabled}'
    },{
        fieldLabel: 'Address Space'.t(),
        xtype: 'textfield',
        vtype: 'cidrBlock',
        bind: '{settings.addressSpace}'
    },{
        fieldLabel: 'NAT OpenVPN Traffic'.t(),
        xtype: 'checkbox',
        bind: '{settings.natOpenVpnTraffic}'
    },{
        xtype: 'app-openvpn-server-tab-panel',
        padding: '20 20 20 20',
        border: true,
    }]
});

Ext.define('Ung.apps.openvpn.view.ServerTabs', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.app-openvpn-server-tab-panel',
    itemId: 'server-tab-panel',
    viewModel: true,
    width: 800,
    height: 400,

    items: [{
        title: 'Remote Clients'.t(),
        items: [
            { xtype: 'app-openvpn-remote-clients-grid' }
            ]
    },{
        title: 'Groups'.t(),
        items: [
            { xtype: 'app-openvpn-groups-grid' }
        ]
    },{
        title: 'Exported Networks'.t(),
        items: [
            { xtype: 'app-openvpn-exported-networks-grid' }
        ]
    }]

});

Ext.define('Ung.apps.openvpn.view.RemoteClientsGrid', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-openvpn-remote-clients-grid',
    itemId: 'remote-clients-grid',

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add']
    }],

    recordActions: ['edit', 'delete'],
    listProperty: 'settings.remoteClients.list',
    emptyRow: {
        javaClass: 'com.untangle.app.openvpn.OpenVpnRemoteClient',
        'enabled': true,
        'name': '',
        'groupId': '1',
        'export': false
        },

    bind: '{remoteClients}',

    columns: [{
        xtype: 'checkcolumn',
        header: 'Enabled'.t(),
        width: 80,
        dataIndex: 'enabled',
        resizable: false
    }, {
        header: 'Client Name'.t(),
        width: 150,
        flex: 1,
        dataIndex: 'name',
    }, {
        header: 'Group'.t(),
        width: 120,
        dataIndex: 'groupId',
    }, {
        xtype: 'actioncolumn',
        header: 'Download Client'.t(),
        width: 120,
        iconCls: 'fa fa-download',
        align: 'center',
    }],

    editorFields: [{
        xtype: 'checkbox',
        bind: '{record.enabled}',
        fieldLabel: 'Enabled'.t()
    }, {
        xtype: 'textfield',
        bind: '{record.name}',
        fieldLabel: 'Client Name'.t()
    }, {
        xtype:'combo',
        fieldLabel: 'Group'.t(),
        editable: false,
        bind: '{record.groupId}',
        store: [['1','Default Group']]
    }, {
        xtype:'combo',
        fieldLabel: 'Type'.t(),
        editable: false,
        bind: '{record.export}',
        store: [[false,'Individual Client'.t()],[true,'Network'.t()]],
    }]

});

Ext.define('Ung.apps.openvpn.view.GroupsGrid', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-openvpn-groups-grid',
    itemId: 'groups-grid',

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add']
    }],

    recordActions: ['edit', 'delete'],
    listProperty: 'settings.groups.list',
    emptyRow: {
        javaClass: 'com.untangle.app.openvpn.OpenVpnGroup',
        'enabled': true,
        'name': '',
        'groupId': '1',
        'export': false
        },

    bind: '{groups}',

    columns: [{
        header: 'Group Name'.t(),
        width: 150,
        flex: 1,
        dataIndex: 'name',
    }, {
        header: 'Full Tunnel'.t(),
        width: 120,
        dataIndex: 'fullTunnel',
    }, {
        header: 'Push DNS'.t(),
        width: 120,
        dataIndex: 'pushDns'
    }],

    editorFields: [{
        xtype: 'textfield',
        fieldLabel: 'Group Name'.t(),
        bind: '{record.name}'
    }, {
        xtype: 'checkbox',
        fieldLabel: 'Full Tunnel'.t(),
        bind: '{record.fullTunnel}'
    }, {
        xtype: 'checkbox',
        fieldLabel: 'Push DNS'.t(),
        bind: '{record.pushDns}'
    }, {
        xtype: 'displayfield',
        value: '<STRONG>' + 'Push DNS Configuration'.t() + '</STRONG>',
        bind: {
            hidden: '{!record.pushDns}'
        }
    }, {
        xtype:'combo',
        fieldLabel: 'Push DNS Server'.t(),
        editable: false,
        store: [[true,'OpenVPN Server'.t()],[false,'Custom'.t()]],
        bind: {
            value: '{record.pushDnsSelf}',
            hidden:'{!record.pushDns}'
        }
    }, {
        xtype: 'textfield',
        fieldLabel: 'Push DNS Custom 1'.t(),
        bind: {
            value: '{record.pushDns1}',
            disabled: '{record.pushDnsSelf}',
            hidden:'{!record.pushDns}'
        }
    }, {
        xtype: 'textfield',
        fieldLabel: 'Push DNS Custom 2'.t(),
        bind: {
            value: '{record.pushDns2}',
            disabled: '{record.pushDnsSelf}',
            hidden:'{!record.pushDns}'
        }
    }, {
        xtype:'textfield',
        fieldLabel: 'Push DNS Domain'.t(),
        bind: {
            value: '{record.pushDnsDomain}',
            hidden:'{!record.pushDns}'
        }
    }]

});

Ext.define('Ung.apps.openvpn.view.ExportedNetworksGrid', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-openvpn-exported-networks-grid',
    itemId: 'exported-clients-grid',

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add']
    }],

    recordActions: ['edit', 'delete'],
    listProperty: 'settings.exports.list',
    emptyRow: {
        javaClass: 'com.untangle.app.openvpn.OpenVpnExport',
        'enabled': true,
        'name': '',
        'network': ''
        },

    bind: '{exportedNetworks}',

    columns: [{
        xtype: 'checkcolumn',
        header: 'Enabled'.t(),
        width: 80,
        dataIndex: 'enabled',
        resizable: false
    }, {
        header: 'Export Name'.t(),
        width: 150,
        flex: 1,
        dataIndex: 'name',
    }, {
        header: 'Network'.t(),
        width: 150,
        dataIndex: 'network',
    }],

    editorFields: [{
        xtype: 'checkbox',
        bind: '{record.enabled}',
        fieldLabel: 'Enabled'.t()
    }, {
        xtype: 'textfield',
        bind: '{record.name}',
        fieldLabel: 'Export Name'.t()
    }, {
        xtype:'textfield',
        vtype: 'cidrBlock',
        fieldLabel: 'Network'.t(),
        bind: '{record.network}'
    }]

});

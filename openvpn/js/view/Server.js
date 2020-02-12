Ext.define('Ung.apps.openvpn.view.Server', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-openvpn-server',
    itemId: 'server',
    title: 'Server'.t(),
    scrollable: true,

    withValidation: true,

    viewModel: {
        formulas: {
            _btnConfigureDirectory: function (get) {
                switch (get('settings.authenticationType')) {
                case 'LOCAL_DIRECTORY': return 'Configure Local Directory'.t();
                case 'RADIUS': return 'Configure RADIUS'.t();
                case 'ACTIVE_DIRECTORY': return 'Configure Active Directory'.t();
                case 'ANY_DIRCON': return 'Configure Directory Connector'.t();
                default: return '';
                }
            }
        }
    },

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'The Server tab is used to configure OpenVPN to operate as a server for remote clients'.t()
    }],

    layout: 'border',

    items: [{
        xtype: 'panel',
        region: 'west',
        width: Math.ceil(Ext.getBody().getViewSize().width / 4),
        split: true,
        scrollable: true,
        layout: {
            type: 'anchor'
        },
        bodyPadding: 10,
        defaults: {
            anchor: '100%',
            labelAlign: 'top'
        },
        items: [{
            xtype: 'checkbox',
            boxLabel: 'Server Enabled'.t(),
            bind: '{settings.serverEnabled}'
        },{
            fieldLabel: 'Site Name'.t(),
            xtype: 'textfield',
            vtype: 'openvpnName',
            disabled: true,
            bind: {
                value: '{settings.siteName}',
                disabled: '{settings.serverEnabled == false}'
            },
            allowBlank: false
        },{
            fieldLabel: 'Address Space'.t(),
            xtype: 'textfield',
            vtype: 'cidrBlockOnlyRanges',
            disabled: true,
            bind: {
                value: '{settings.addressSpace}',
                disabled: '{settings.serverEnabled == false}'
            },
        },{
            xtype: 'checkbox',
            boxLabel: 'NAT OpenVPN Traffic'.t(),
            disabled: true,
            bind: {
                value: '{settings.natOpenVpnTraffic}',
                disabled: '{settings.serverEnabled == false}'
            },
        },{
            fieldLabel: 'Site URL'.t(),
            xtype: 'displayfield',
            disabled: true,
            bind: {
                value: '{getSiteUrl}',
                disabled: '{settings.serverEnabled == false}'
            },
        },{
            xtype: 'checkbox',
            boxLabel: 'Username/Password Authentication',
            disabled: true,
            bind: {
                value: '{settings.authUserPass}',
                disabled: '{settings.serverEnabled == false}'
            },
        },{
            xtype: 'container',
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            hidden: true,
            disabled: true,
            bind: {
                disabled: '{settings.serverEnabled == false}',
                hidden: '{!settings.authUserPass}'
            },
            items: [{
                xtype: 'displayfield',
                margin: '10 0 10 0',
                value: '<B>NOTE:</B> Enabling or disabling Username/Password Authentication will require existing remote client configuration files to be downloaded again.'
            }, {
                xtype: 'radiogroup',
                fieldLabel: 'Authentication Method'.t(),
                labelAlign: 'top',
                bind: '{settings.authenticationType}',
                simpleValue: 'true',
                columns: 1,
                vertical: true,
                items: [
                    { boxLabel: '<strong>' + 'Local Directory'.t() + '</strong>', inputValue: 'LOCAL_DIRECTORY' },
                    { boxLabel: '<strong>' + 'RADIUS'.t() + '</strong> (' + 'requires'.t() + ' Directory Connector)', inputValue: 'RADIUS' },
                    { boxLabel: '<strong>' + 'Active Directory'.t() + '</strong> (' + 'requires'.t() + ' Directory Connector)', inputValue: 'ACTIVE_DIRECTORY' },
                    { boxLabel: '<strong>' + 'Any Directory Connector'.t() + '</strong> (' + 'requires'.t() + ' Directory Connector)', inputValue: 'ANY_DIRCON' }
                ]
            }, {
                // todo: update this button later
                xtype: 'button',
                iconCls: 'fa fa-cog',
                margin: '5 0',
                scale: 'medium',
                bind: {
                    text: '{_btnConfigureDirectory}'
                },
                handler: 'configureAuthenticationMethod'
            }]
        }]
    }, {
        xtype: 'tabpanel',
        itemId: 'server',
        region: 'center',

        defaults: {
            border: false
        },
        disabled: true,
        bind: {
            disabled: '{settings.serverEnabled == false}',
        },
        items: [{
            title: 'Remote Clients'.t(),
            itemId: 'remote_clients',
            xtype: 'app-openvpn-remote-clients-grid'
        },{
            title: 'Groups'.t(),
            itemId: 'groups',
            xtype: 'app-openvpn-groups-grid'
        },{
            title: 'Exported Networks'.t(),
            itemId: 'exported_networks',
            xtype: 'app-openvpn-exported-networks-grid'
        }]
    }]
});

Ext.define('Ung.apps.openvpn.cmp.RemoteClientsGrid', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-openvpn-remote-clients-grid',
    itemId: 'remote-clients-grid',
    viewModel: true,
    controller: 'app-openvpn-special',

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete'],
    listProperty: 'settings.remoteClients.list',
    emptyRow: {
        javaClass: 'com.untangle.app.openvpn.OpenVpnRemoteClient',
        'enabled': true,
        'name': '',
        'groupId': 1,
        'export': false,
        'existing': false
    },

    bind: '{remoteClients}',

    columns: [{
        xtype: 'checkcolumn',
        header: 'Enabled'.t(),
        width: Renderer.booleanWidth,
        dataIndex: 'enabled',
        resizable: false
    }, {
        header: 'Client Name'.t(),
        width: Renderer.usernameWidth,
        flex: 1,
        dataIndex: 'name',
    }, {
        header: 'Group'.t(),
        width: Renderer.usernameWidth,
        flex: 1,
        dataIndex: 'groupId',
        renderer: Ung.apps.openvpn.SpecialGridController.groupRenderer
    }, {
        xtype: 'actioncolumn',
        header: 'Download Client'.t(),
        width: Renderer.usernameWidth,
        iconCls: 'fa fa-download',
        align: 'center',
        handler: 'downloadClient',
    }],

    editorFields: [{
        xtype: 'checkbox',
        fieldLabel: 'Enabled'.t(),
        bind: '{record.enabled}'
    }, {
        xtype: 'textfield',
        vtype: 'openvpnName',
        fieldLabel: 'Client Name'.t(),
        allowBlank: false,
        bind: {
            value: '{record.name}',
            readOnly: '{record.existing}'
        }
    }, {
        xtype: 'combobox',
        fieldLabel: 'Group'.t(),
        bind: {
            value: '{record.groupId}',
            store: '{groups}'
        },
        allowBlank: false,
        editable: false,
        queryMode: 'local',
        displayField: 'name',
        valueField: 'groupId'
    }, {
        xtype: 'combo',
        fieldLabel: 'Type'.t(),
        editable: false,
        bind: '{record.export}',
        store: [[false,'Individual Client'.t()],[true,'Network'.t()]]
    }, {
        xtype: 'textfield',
        fieldLabel: 'Remote Networks'.t(),
        bind: {
            value: '{record.exportNetwork}',
            disabled: '{!record.export}',
            hidden: '{!record.export}'
        },
        allowBlank: false,
        vtype: 'cidrBlockList'
    }]

});

Ext.define('Ung.apps.openvpn.cmp.GroupsGrid', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-openvpn-groups-grid',
    itemId: 'groups-grid',
    viewModel: true,

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete'],
    listProperty: 'settings.groups.list',
    emptyRow: {
        javaClass: 'com.untangle.app.openvpn.OpenVpnGroup',
        'enabled': true,
        'name': '',
        'groupId': -1,
        'export': false
    },

    bind: '{groups}',

    columns: [{
        header: 'Group Name'.t(),
        allowBlank: false,
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'name',
    }, {
        header: 'Full Tunnel'.t(),
        width: Renderer.booleanWidth + 20,
        dataIndex: 'fullTunnel',
    }, {
        header: 'Push DNS'.t(),
        width: Renderer.booleanWidth + 20,
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

Ext.define('Ung.apps.openvpn.cmp.ExportedNetworksGrid', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-openvpn-exported-networks-grid',
    itemId: 'exported-clients-grid',
    viewModel: true,

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
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
        width: Renderer.booleanWidth,
        dataIndex: 'enabled',
        resizable: false
    }, {
        header: 'Export Name'.t(),
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'name',
    }, {
        header: 'Network'.t(),
        width: Renderer.networkWidth,
        dataIndex: 'network',
    }],

    editorFields: [{
        xtype: 'checkbox',
        bind: '{record.enabled}',
        fieldLabel: 'Enabled'.t()
    }, {
        xtype: 'textfield',
        bind: '{record.name}',
        allowBlank: false,
        fieldLabel: 'Export Name'.t()
    }, {
        xtype:'textfield',
        vtype: 'cidrBlock',
        allowBlank: false,
        fieldLabel: 'Network'.t(),
        bind: '{record.network}'
    }]

});

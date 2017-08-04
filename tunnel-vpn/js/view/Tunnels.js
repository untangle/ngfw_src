Ext.define('Ung.apps.tunnel-vpn.view.Tunnels', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-tunnel-vpn-tunnels',
    itemId: 'tunnels',
    title: 'Tunnels'.t(),
    viewModel: true,

    controller: 'untunnelgrid',
    editorXtype: 'ung.cmp.untunnelrecordeditor',

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600 },
            html: 'Configure Tunnel VPN tunnels (connections) to remote VPN services'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add']
    }],

    recordActions: ['edit', 'delete'],
    listProperty: 'settings.tunnels.list',

    bind: '{tunnels}',

    columns: [{
        xtype: 'checkcolumn',
        header: 'Enabled'.t(),
        width: Renderer.idWidth,
        dataIndex: 'enabled',
        resizable: false
    }, {
        header: 'Tunnel ID'.t(),
        width: Renderer.idWidth,
        dataIndex: 'tunnelId',
        rtype: 'tunnelid'
    }, {
        header: 'Tunnel Name'.t(),
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'name',
        editor: {
            xtype: 'textfield',
            bind: '{record.name}'
        }
    }, {
        header: 'Provider'.t(),
        width: Renderer.messageWidth,
        dataIndex: 'provider',
    }, {
        header: 'Username'.t(),
        width: Renderer.usernameWidth,
        dataIndex: 'username',
        editor: {
            xtype: 'textfield',
            bind: '{record.username}'
        }
    }, {
        header: 'Password'.t(),
        width: Renderer.usernameWidth,
        dataIndex: 'password',
        editor: {
            xtype: 'textfield',
            bind: '{record.password}'
        }
    }],

    emptyRow: {
        enabled: true,
        name: 'tunnel',
        provider: '',
        javaClass: 'com.untangle.app.tunnel_vpn.TunnelVpnTunnelSettings',
        tunnelId: -1
    },

    editorFields: [
        Field.enableRule(),
    {
        xtype: 'textfield',
        fieldLabel: 'Tunnel Name'.t(),
        name: 'tunnelName',
        bind: '{record.name}'
    },{
        xtype: 'hidden',
        name: 'tunnelId',
        bind: '{record.tunnelId}'
    }, {
        xtype: 'combo',
        name: 'provider',
        fieldLabel: 'Provider'.t(),
        editable: false,
        valueField: 'name',
        displayField: 'description',
        bind: {
            value: '{record.provider}',
            store: '{providersComboList}'
        },
        listeners: {
            change: 'providerChange'
        }
    },{
        xtype: 'component',
        margin: '0 10 0 190',
        bind: {
            html: '{tunnelProviderTitle}'
        }
    },{
        xtype: 'component',
        margin: '0 10 0 190',
        bind: {
            html: '{tunnelProviderInstructions}'
        }
    },{
        xtype: 'form',
        name: 'upload_form',
        border: false,
        margin: '10 10 0 170',
        items: [{
            xtype: 'container',
            layout: 'hbox',
            items: [{
                xtype: 'filefield',
                label: 'Upload Config File'.t(),
                name: 'upload_file',
                buttonText: 'Select VPN Config File'.t(),
                buttonOnly: true,
                listeners: {
                    change: 'uploadFile'
                },
                bind: {
                    disabled: '{tunnelProviderSelected == false}'
                },
                validation: 'Must upload VPN config file'.t()
            },{
                xtype: 'label',
                margin: '5 0 0 0',
                bind: {
                    text: '{fileResult}',
                    disabled: '{tunnelProviderSelected == false}'
                }
            }]
        },{
            xtype: 'hidden',
            name: 'type',
            value: 'tunnel_vpn'
        },{
            xtype: 'hidden',
            name: 'argument',
            bind: {
                value: '{record.provider}',
            },
        }]
    }, {
        xtype: 'textfield',
        fieldLabel: 'Username'.t(),
        bind: {
            value: '{record.username}',
            disabled: '{tunnelProviderSelected == false}',
            hidden: '{tunnelUsernameHidden == true}'
        },
    }, {
        xtype: 'textfield',
        fieldLabel: 'Password'.t(),
        bind: {
            value: '{record.password}',
            disabled: '{tunnelProviderSelected == false}',
            hidden: '{tunnelPasswordHidden == true}'
        },
    }]

});

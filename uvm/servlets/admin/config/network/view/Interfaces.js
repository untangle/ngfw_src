Ext.define('Ung.config.network.view.Interfaces', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-interfaces',
    itemId: 'interfaces',
    scrollable: true,

    title: 'Interfaces'.t(),
    layout: 'border',

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: '<strong>' + 'Interface configuration'.t() + '</strong> <br/>' +  'Use this page to configure each interface\'s configuration and its mapping to a physical network card.'.t()
    }],

    items: [{
        xtype: 'ungrid',
        itemId: 'interfacesGrid',
        reference: 'interfacesGrid',
        region: 'center',
        border: false,
        split: true,
        selModel: 'rowmodel',
        tbar: [{
            text: 'Refresh'.t(),
            iconCls: 'fa fa-refresh',
            handler: 'externalAction',
            action: 'loadSettings'
        }, {
            text: 'Add Tagged VLAN Interface'.t(),
            iconCls: 'fa fa-plus',
            hidden: true,
            bind: { hidden: '{!settings.vlansEnabled}' },
            handler: 'externalAction',
            action: 'editInterface'
        }, {
            text: 'Remap Interfaces'.t(),
            iconCls: 'fa fa-random',
            handler: 'externalAction',
            action: 'remapInterfaces'
        }],

        layout: 'fit',

        bind: '{interfaces}',
        sortableColumns: false,
        fields: [
            'interfaceId'
        ],
        columns: [{
            dataIndex: 'connected',
            width: Renderer.iconWidth,
            align: 'center',
            resizable: false,
            sortable: false,
            hideable: false,
            renderer: Ung.config.network.MainController.connectedIconRenderer
        }, {
            header: 'Id'.t(),
            dataIndex: 'interfaceId',
            width: Renderer.idWidth,
            resizable: false,
            align: 'right',
            renderer: Renderer.id
        }, {
            width: Renderer.iconWidth,
            align: 'center',
            resizable: false,
            sortable: false,
            hideable: false,
            menuDisabled: true,
            renderer: Ung.config.network.MainController.interfacetypeRenderer
        }, {
            header: 'Name'.t(),
            dataIndex: 'name',
            width: Renderer.messageWidth,
            flex: 1
        }, {
            header: 'Connected'.t(),
            dataIndex: 'connected',
            width: Renderer.idWidth,
            renderer: Ung.config.network.MainController.connectedRenderer
        }, {
            header: 'Device'.t(),
            dataIndex: 'physicalDev',
            width: Renderer.idWidth,
            renderer: Ung.config.network.MainController.deviceRenderer
        }, {
            header: 'Speed'.t(),
            dataIndex: 'mbit',
            width: Renderer.sizeWidth,
            renderer: Ung.config.network.MainController.speedRenderer
        }, {
            header: 'Duplex'.t(),
            dataIndex: 'duplex',
            width: Renderer.idWidth,
            renderer: Ung.config.network.MainController.duplexRenderer
        }, {
            header: 'Config'.t(),
            dataIndex: 'configType',
            width: Renderer.idWidth,
            renderer: Ung.config.network.MainController.addressedRenderer
        }, {
            header: 'Current Address'.t(),
            dataIndex: 'v4Address',
            width: Renderer.networkWidth,
            renderer: Ung.config.network.MainController.addressRenderer
        }, {
            header: 'is WAN'.t(),
            width: Renderer.booleanWidth,
            resizable: false,
            dataIndex: 'isWan',
            align: 'center',
            renderer: Ung.config.network.MainController.iswanRenderer
        // }, {
        //     header: 'is Vlan'.t(),
        //     width: Renderer.booleanWidth,
        //     hidden: true,
        //     dataIndex: 'isVlanInterface',
        //     align: 'center',
        //     renderer: Ung.config.network.MainController.isvlanRenderer
        // }, {
        //     header: 'Vendor'.t(),
        //     hidden: true,
        //     width: Renderer.messageWidth,
        //     dataIndex: 'vendor'
        }, {
            xtype: 'actioncolumn',
            width: Renderer.actionWidth,
            header: 'Edit'.t(),
            align: 'center',
            resizable: false,
            tdCls: 'action-cell',
            iconCls: 'fa fa-pencil',
            handler: 'externalAction',
            action: 'editInterface',
            menuDisabled: true,
            hideable: false
        }, {
            xtype: 'actioncolumn',
            width: Renderer.actionWidth,
            header: 'Delete'.t(),
            align: 'center',
            resizable: false,
            tdCls: 'action-cell',
            iconCls: 'fa fa-trash-o fa-red',
            menuDisabled: true,
            hideable: false,
            isDisabled: function (table, rowIndex, colIndex, item, record) {
                return !(record.get('isVlanInterface') || record.get('connected') === 'MISSING');
            },
            handler: function (table, rowIndex, colIndex, item, e, record) {
                var msg = '';
                if (record.get('isVlanInterface')) {
                    msg = 'Delete VLAN Interface'.t();
                }
                if (record.get('connected') === 'MISSING') {
                    msg = 'Delete missing Interface'.t();
                }
                Ext.Msg.confirm(msg,
                    'Are you sure you want to delete <strong>' + record.get('name') + '</strong> Interface?',
                    function (button) {
                        if (button === 'yes') { record.drop(); }
                    });
            }
        }]
    }, {
        xtype: 'panel',
        region: 'south',
        split: true,
        collapsible: false,
        height: '50%',
        hidden: true,
        layout: 'border',
        bind: {
            hidden: '{!interfacesGrid.selection}',
        },
        items: [{
            title: 'Status'.t(),
            region: 'center',
            itemId: 'interfaceStatus',
            xtype: 'unpropertygrid',
            collapsible: false,
            emptyText: 'Status not available'.t(),
            bind: {
                source: '{siStatus}',
            },
            sourceConfig: {
                device: { displayName: 'Device'.t() },
                macAddress: { displayName: 'MAC Address'.t() },
                address: { displayName: 'IPv4 Address'.t() },
                v6Addr: { displayName: 'IPv6 Address'.t() },
                rxbytes: {
                    displayName: 'Rx Bytes'.t(),
                    renderer: Renderer.datasize
                },
                rxpkts: {
                    displayName: 'Rx Packets'.t(),
                    renderer: Renderer.count
                },
                rxerr: {
                    displayName: 'Rx Errors'.t(),
                    renderer: Renderer.count
                },
                rxdrop: {
                    displayName: 'Rx Drop'.t(),
                    renderer: Renderer.count
                },
                txbytes: {
                    displayName: 'Tx Bytes'.t(),
                    renderer: Renderer.datasize
                },
                txpkts: {
                    displayName: 'Tx Packets'.t(),
                    renderer: Renderer.count
                },
                txerr: {
                    displayName: 'Tx Errors'.t(),
                    renderer: Renderer.count
                },
                txdrop: {
                    displayName: 'Tx Drop'.t(),
                    renderer: Renderer.count
                }
            },
            tbar: [{
                text: 'Refresh'.t(),
                iconCls: 'fa fa-refresh',
                handler: 'externalAction',
                action: 'getInterfaceStatus'
            }],
        }, {
            xtype: 'ungrid',
            region: 'east',
            width: '60%',
            split: true,
            itemId: 'interfaceArp',
            title: 'ARP Entries'.t(),

            emptyText: 'No ARP Entries defined'.t(),

            bind: '{interfaceArp}',
            columns: [{
                header: 'MAC Address'.t(),
                dataIndex: 'macAddress',
                width: Renderer.macWidth
            }, {
                header: 'IP Address'.t(),
                dataIndex: 'address',
                width: Renderer.ipWidth,
                flex: 1
            }],
            tbar: [{
                xtype: 'button',
                iconCls: 'fa fa-refresh',
                text: 'Refresh',
                handler: 'externalAction',
                action: 'getInterfaceArp'
            }]
        }]
    }]
});

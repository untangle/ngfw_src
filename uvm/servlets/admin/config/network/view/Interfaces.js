Ext.define('Ung.config.network.view.Interfaces', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-interfaces',
    itemId: 'interfaces',

    title: 'Interfaces'.t(),
    layout: 'border',

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: '<strong>' + 'Interface configuration'.t() + '</strong> <br/>' +  'Use this page to configure each interface\'s configuration and its mapping to a physical network card.'.t()
    }],

    items: [{
        xtype: 'grid',
        itemId: 'interfacesGrid',
        reference: 'interfacesGrid',
        region: 'center',
        border: false,
        split: true,
        tbar: [{
            text: 'Refresh'.t(),
            iconCls: 'fa fa-refresh',
            handler: 'loadSettings'
        }, {
            text: 'Add Tagged VLAN Interface'.t(),
            iconCls: 'fa fa-plus',
            hidden: true,
            bind: { hidden: '{!settings.vlansEnabled}' },
            handler: 'editInterface'
        }, {
            text: 'Remap Interfaces'.t(),
            iconCls: 'fa fa-random',
            handler: 'remapInterfaces'
        }],

        layout: 'fit',

        bind: '{interfaces}',
        sortableColumns: false,
        fields: [
            'interfaceId'
        ],
        columns: [{
            dataIndex: 'connected',
            width: 40,
            align: 'center',
            resizable: false,
            sortable: false,
            hideable: false,
            renderer: function (value) {
                switch (value) {
                case 'CONNECTED': return '<i class="fa fa-circle fa-green"></i>';
                case 'DISCONNECTED': return '<i class="fa fa-circle fa-gray"></i>';
                case 'MISSING': return '<i class="fa fa-exclamation-triangle fa-orange"></i>';
                default: return '<i class="fa fa-question-circle fa-gray"></i>';
                }
            }
        }, {
            header: 'Id'.t(),
            dataIndex: 'interfaceId',
            width: 70,
            resizable: false,
            align: 'right'
        }, {
            header: 'Name'.t(),
            dataIndex: 'name',
            minWidth: 200,
            flex: 1
        }, {
            header: 'Connected'.t(),
            dataIndex: 'connected',
            width: 130,
            renderer: function (value) {
                switch (value) {
                case 'CONNECTED': return 'connected'.t();
                case 'DISCONNECTED': return 'disconnected'.t();
                case 'MISSING': return 'missing'.t();
                default: return 'unknown'.t();
                }
            }
        }, {
            header: 'Device'.t(),
            dataIndex: 'physicalDev',
            width: 100,
            renderer: function (value, metadata, record) {
                if (record.get('isVlanInterface')) {
                    return record.get('systemDev');
                }
                return value;
            }
        }, {
            header: 'Speed'.t(),
            dataIndex: 'mbit',
            width: 100
        }, {
            header: 'Physical Dev'.t(),
            dataIndex: 'physicalDev',
            hidden: true,
            width: 80
        }, {
            header: 'System Dev'.t(),
            dataIndex: 'systemDev',
            hidden: true,
            width: 80
        }, {
            header: 'Symbolic Dev'.t(),
            dataIndex: 'symbolicDev',
            hidden: true,
            width: 80
        }, {
            header: 'IMQ Dev'.t(),
            dataIndex: 'imqDev',
            hidden: true,
            width: 80
        }, {
            header: 'Duplex'.t(),
            dataIndex: 'duplex',
            hidden: true,
            width: 100,
            renderer: function (value) {
                switch (value) {
                case 'FULL_DUPLEX': return 'full-duplex'.t();
                case 'HALF_DUPLEX': return 'half-duplex'.t();
                default: return 'unknown'.t();
                }
            }
        }, {
            header: 'Config'.t(),
            dataIndex: 'configType',
            width: 100,
            renderer: function (value) {
                switch (value) {
                case 'ADDRESSED': return 'Addressed'.t();
                case 'BRIDGED': return 'Bridged'.t();
                case 'DISABLED': return 'Disabled'.t();
                default: value.t();
                }
            }
        }, {
            header: 'Current Address'.t(),
            dataIndex: 'v4Address',
            width: 150,
            renderer: function (value, metaData, record) {
                return Ext.isEmpty(value) ? '' : value + ' / ' + record.get('v4PrefixLength');
            }
        }, {
            header: 'is WAN'.t(),
            width: 80,
            resizable: false,
            dataIndex: 'isWan',
            align: 'center',
            renderer: function (value, metaData, record) {
                return record.get('configType') === 'ADDRESSED' ? (value ? 'true'.t() : 'false'.t()) : '';
            }
        }, {
            header: 'is Vlan'.t(),
            hidden: true,
            dataIndex: 'isVlanInterface',
            align: 'center',
            renderer: function (value) {
                return value ? '<i class="fa fa-check fa-lg"></i>' : '<i class="fa fa-minus fa-lg"></i>';
            }
        }, {
            header: 'MAC Address'.t(),
            hidden: true,
            width: 160,
            dataIndex: 'macAddress'
        }, {
            header: 'Vendor'.t(),
            hidden: true,
            width: 160,
            dataIndex: 'vendor'
        }, {
            xtype: 'actioncolumn',
            width: 60,
            header: 'Edit'.t(),
            align: 'center',
            resizable: false,
            tdCls: 'action-cell',
            iconCls: 'fa fa-pencil',
            handler: 'editInterface',
            menuDisabled: true,
            hideable: false
        }, {
            xtype: 'actioncolumn',
            width: 60,
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
        // maxHeight: '50%',
        hidden: true,
        layout: 'border',
        bind: {
            // title: '{si.name} ({si.physicalDev})',
            hidden: '{!interfacesGrid.selection}',
        },
        // tbar: [{
        //     bind: {
        //         text: '<strong>' + 'Edit'.t() + ' {si.name} ({si.physicalDev})' + '</strong>',
        //         iconCls: 'fa fa-pencil',
        //         scale: 'large',
        //         width: '100%',
        //         handler: 'editInterface'
        //     }
        // }],
        items: [{
            title: 'Status'.t(),
            region: 'center',
            // border: false,
            itemId: 'interfaceStatus',
            xtype: 'propertygrid',
            // header: false,
            hideHeaders: true,
            sortableColumns: false,
            align: 'right',
            nameColumnWidth: 150,
            // hidden: true,
            bind: {
                source: '{siStatus}',
                // hidden: '{isDisabled}'
            },
            sourceConfig: {
                device: { displayName: 'Device'.t() },
                macAddress: { displayName: 'MAC Address'.t() },
                address: { displayName: 'IPv4 Address'.t() },
                mask: { displayName: 'Mask'.t() },
                v6Addr: { displayName: 'IPv6'.t() },
                rxpkts: { displayName: 'Rx Packets'.t() },
                rxerr: { displayName: 'Rx Errors'.t() },
                rxdrop: { displayName: 'Rx Drop'.t() },
                txpkts: { displayName: 'Tx Packets'.t() },
                txerr: { displayName: 'Tx Errors'.t() },
                txdrop: { displayName: 'Tx Drop'.t() }
            },
            tbar: [{
                text: 'Refresh'.t(),
                iconCls: 'fa fa-refresh',
                handler: 'getInterfaceStatus'
            }],
            listeners: {
                beforeedit: function () { return false; }
            }
        }, {
            xtype: 'grid',
            region: 'east',
            width: '60%',
            split: true,
            // border: false,
            itemId: 'interfaceArp',
            title: 'ARP Entry List'.t(),

            // viewConfig: {
            //     emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-exclamation-triangle fa-2x"></i> <br/>No Data!</p>',
            // },
            bind: '{interfaceArp}',
            columns: [{
                header: 'MAC Address'.t(),
                dataIndex: 'macAddress'
            }, {
                header: 'IP Address'.t(),
                dataIndex: 'address'
            }, {
                header: 'Type'.t(),
                dataIndex: 'type'
            }],
            tbar: [{
                xtype: 'button',
                iconCls: 'fa fa-refresh',
                text: 'Refresh',
                handler: 'getInterfaceArp'
            }]
        }]
    }
    // {
    //     region: 'east',
    //     weight: 20,
    //     bind: {
    //         source: '{interfacesGrid.selection.data}'
    //     },
    //     title: 'Interface',
    //     xtype: 'propertygrid',
    //     split: true,
    //     collapsible: true,
    //     shadow: false,
    //     animCollapse: false,
    //     titleCollapse: true,
    //     editable: false,
    //     width: 400
    // }
    ]
});

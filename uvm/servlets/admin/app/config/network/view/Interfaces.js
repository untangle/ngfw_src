Ext.define('Ung.config.network.view.Interfaces', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.network.interfaces', //..

    title: 'Interfaces'.t(),
    layout: 'border',
    itemId: 'interfaces',

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: '<strong>' + 'Interface configuration'.t() + '</strong> <br/>' +  "Use this page to configure each interface's configuration and its mapping to a physical network card.".t()
    }],

    actions: {
        refresh: {
            xtype: 'button',
            iconCls: 'fa fa-refresh',
            text: 'Refresh'.t(),
            handler: 'loadSettings'
        }
    },

    items: [{
        xtype: 'grid',
        itemId: 'interfacesGrid',
        reference: 'interfacesGrid',
        region: 'center',
        border: false,

        tbar: ['@refresh'],

        layout: 'fit',
        forceFit: true,
        // viewConfig: {
        //     plugins: {
        //         ptype: 'gridviewdragdrop',
        //         dragText: 'Drag and drop to reorganize'.t(),
        //         // allow drag only from drag column icons
        //         dragZone: {
        //             onBeforeDrag: function (data, e) {
        //                 return Ext.get(e.target).hasCls('fa-arrows');
        //             }
        //         }
        //     }
        // },
        // title: 'Interfaces'.t(),
        bind: '{interfaces}',

        columns: [
        // {
        //     xtype: 'gridcolumn',
        //     header: '<i class="fa fa-sort"></i>',
        //     align: 'center',
        //     width: 30,
        //     tdCls: 'action-cell',
        //     // iconCls: 'fa fa-arrows'
        //     renderer: function() {
        //         return '<i class="fa fa-arrows" style="cursor: move;"></i>';
        //     },
        // },
        {
            header: 'Id'.t(),
            dataIndex: 'interfaceId',
            width: 50,
            align: 'right'
        }, {
            header: 'Name'.t(),
            dataIndex: 'name',
            minWidth: 200
            // flex: 1
        }, {
            header: 'Connected'.t(),
            dataIndex: 'connected',
            width: 130,
            renderer: function (value) {
                switch (value) {
                case 'CONNECTED': return 'connected'.t();
                case 'DISCONNECTED': return 'disconnected'.t();
                case 'MISSING': return 'missing'.t();
                default: 'unknown'.t();
                }
            }
        }, {
            header: 'Device'.t(),
            dataIndex: 'physicalDev',
            width: 100
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
            dataIndex: 'isWan',
            align: 'center',
            renderer: function (value, metaData, record) {
                return (record.get('configType') === 'ADDRESSED') ? (value ? '<i class="fa fa-check fa-lg"></i>' : '<i class="fa fa-minus fa-lg"></i>') : '<i class="fa fa-minus fa-lg"></i>'; // if its addressed return value
            }
        }, {
            header: 'is Vlan'.t(),
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
        }]
    }, {
        xtype: 'panel',
        region: 'east',
        split: 'true',
        collapsible: false,
        width: 350,
        maxWidth: 450,
        hidden: true,
        layout: 'border',
        bind: {
            title: '{si.name} ({si.physicalDev})',
            hidden: '{!si}',
            // activeItem: '{activePropsItem}'
        },
        tbar: [{
            bind: {
                text: '<strong>' + 'Edit'.t() + ' {si.name} ({si.physicalDev})' + '</strong>',
                iconCls: 'fa fa-pencil',
                scale: 'large',
                width: '100%',
                handler: 'editInterface'
            }
        }],
        items: [{
            title: 'Status'.t(),
            region: 'center',
            border: false,
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
                xtype: 'button',
                iconCls: 'fa fa-refresh',
                text: 'Refresh'
            }]
        }, {
            xtype: 'grid',
            region: 'south',
            height: '30%',
            split: true,
            itemId: 'interfaceArp',
            title: 'ARP Entry List'.t(),
            forceFit: true,
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
            }]
        }]
    }]
});

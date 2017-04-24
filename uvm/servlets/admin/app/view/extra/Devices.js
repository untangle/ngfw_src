Ext.define('Ung.view.extra.Devices', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.devices',

    /* requires-start */
    requires: [
        'Ung.view.extra.DevicesController'
    ],
    /* requires-end */
    controller: 'devices',

    layout: 'border',

    dockedItems: [{
        xtype: 'toolbar',
        ui: 'navigation',
        dock: 'top',
        border: false,
        style: {
            background: '#333435',
            zIndex: 9997
        },
        defaults: {
            xtype: 'button',
            border: false,
            hrefTarget: '_self'
        },
        items: Ext.Array.insert(Ext.clone(Util.subNav), 0, [{
            xtype: 'component',
            margin: '0 0 0 10',
            style: {
                color: '#CCC'
            },
            html: 'Current Devices'.t()
        }])
    }],

    defaults: {
        border: false
    },

    viewModel: {
        // formulas: {
        //     deviceDetails: function (get) {
        //         if (get('devicesgrid.selection')) {
        //             var data = get('devicesgrid.selection').getData();
        //             console.log(data);
        //             delete data._id;
        //             delete data.javaClass;
        //             return data;
        //         }
        //         return;
        //     }
        // }
    },

    items: [{
        xtype: 'ungrid',
        region: 'center',
        itemId: 'devicesgrid',
        reference: 'devicesgrid',
        title: 'Current Devices'.t(),
        store: 'devices',
        forceFit: false,
        stateful: true,

        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'delete'],
        emptyRow: {
            macAddress: '',
            macVendor: '',
            hostname: '',
            interfaceId: -1,
            lastSessionTime: 0,
            tags: {
                javaClass: 'java.util.LinkedList',
                list: []
            },
            username: '',
            javaClass: 'com.untangle.uvm.DeviceTableEntry'
        },

        columns: [{
            header: 'MAC Address'.t(),
            dataIndex: 'macAddress',
            width: 150,
            filter: { type: 'string' }
        }, {
            header: 'MAC Vendor'.t(),
            dataIndex: 'macVendor',
            width: 150,
            filter: { type: 'string' },
            editor: {
                xtype: 'textfield',
                emptyText: '[no MAC Vendor]'.t()
            }
        }, {
            header: 'Interface'.t(),
            dataIndex: 'interfaceId',
            width: 120,
            filter: { type: 'number' },
            renderer: function (value) {
                if (value === null || value < 0) {
                    return '';
                }
                var intf = Util.interfacesListNamesMap()[value];
                if (!intf) {
                    return Ext.String.format('Interface [{0}]'.t(), value);
                }
                return intf + ' [' + value + ']';
            }
        }, {
            header: 'Hostname'.t(),
            dataIndex: 'hostname',
            width: 120,
            editor: {
                xtype: 'textfield',
                emptyText: '[no hostname]'.t()
            }
        }, {
            header: 'Username'.t(),
            dataIndex: 'username',
            width: 150,
            editor: {
                xtype: 'textfield',
                emptyText: '[no device username]'.t()
            }
        }, {
            header: 'HTTP'.t() + ' - ' + 'User Agent'.t(),
            dataIndex: 'httpUserAgent',
            width: 200,
            flex: 1,
            editor: {
                xtype: 'textfield',
                emptyText: '[no HTTP user agent]'.t()
            }
        }, {
            header: 'Last Seen Time'.t(),
            dataIndex: 'lastSessionTime',
            width: 150,
            renderer: function (val, metaData, record) {
                return val === 0 || val === '' ? '' : Util.timestampFormat(val);
            },
            filter: {
                type: 'date'
            }
        }, {
            header: 'Tags',
            dataIndex: 'tagsString',
            width: 150,
            filter: {
                type: 'string'
            }
        }],
        editorFields: [{
            xtype: 'textfield',
            disabled: true,
            bind: {
                value: '{record.macAddress}',
                disabled: '{record.internalId >= 0}'
            },
            fieldLabel: 'MAC Address'.t(),
            emptyText: '[enter MAC address]'.t(),
            allowBlank: false,
            vtype: 'macAddress',
            maskRe: /[a-fA-F0-9:]/
        }, {
            xtype: 'textfield',
            bind: '{record.macVendor}',
            fieldLabel: 'MAC Vendor'.t(),
            emptyText: '[no MAC Vendor]'.t(),
        }, {
            xtype: 'textfield',
            bind: '{record.hostname}',
            fieldLabel: 'Hostname'.t(),
            emptyText: '[no hostname]'.t(),
        }, {
            xtype: 'textfield',
            bind: '{record.username}',
            fieldLabel: 'Username'.t(),
            emptyText: '[no username]'.t(),
        }, {
            xtype: 'textfield',
            bind: '{record.httpUserAgent}',
            fieldLabel: 'HTTP'.t() + ' - ' + 'User Agent'.t(),
            emptyText: '[no HTTP user agent]'.t()
        }],
    },
    // {
    //     region: 'east',
    //     xtype: 'propertygrid',
    //     itemId: 'details',
    //     editable: false,
    //     width: 400,
    //     split: true,
    //     collapsible: false,
    //     resizable: true,
    //     shadow: false,
    //     hidden: true,

    //     cls: 'prop-grid',

    //     viewConfig: {
    //         stripeRows: false,
    //         getRowClass: function(record) {
    //             if (record.get('value') === null || record.get('value') === '') {
    //                 return 'empty';
    //             }
    //             return;
    //         }
    //     },

    //     nameColumnWidth: 200,
    //     bind: {
    //         // title: '{devicesgrid.selection.hostname} ({devicesgrid.selection.address})',
    //         source: '{deviceDetails}',
    //         hidden: '{!devicesgrid.selection}'
    //     },
    //     sourceConfig: {
    //         username:            { displayName: 'Username'.t() },
    //         hostname:            { displayName: 'Hostname'.t() },
    //         hostnameLastKnown:   { displayName: 'HostnameLastKnown'.t() },
    //         httpUserAgent:       { displayName: 'HTTP'.t() + ' - ' + 'User Agent'.t() },
    //         lastSeenInterfaceId: { displayName: 'Interface'.t() },
    //         lastSessionTime:     { displayName: 'Last Seen Time'.t(), renderer: 'timestampRenderer' },
    //         macAddress:          { displayName: 'MAC Address'.t() },
    //         macVendor:           { displayName: 'MAC Vendor'.t() },
    //         tags:                { displayName: 'Tags'.t() },
    //         tagsString:          { displayName: 'Tags String'.t() },
    //     },
    //     listeners: {
    //         beforeedit: function () {
    //             return false;
    //         }
    //     }
    // }
    ],
    tbar: [{
        xtype: 'button',
        text: 'Refresh'.t(),
        iconCls: 'fa fa-repeat',
        handler: 'getDevices',
        bind: {
            disabled: '{autoRefresh}'
        }
    }, {
        xtype: 'button',
        text: 'Reset View'.t(),
        iconCls: 'fa fa-refresh',
        itemId: 'resetBtn',
        handler: 'resetView',
    }, '-', 'Filter:'.t(), {
        xtype: 'textfield',
        checkChangeBuffer: 200
    }, '->', {
        xtype: 'button',
        text: 'View Reports'.t(),
        iconCls: 'fa fa-line-chart',
        href: '#reports/devices',
        hrefTarget: '_self'
    }],
    bbar: ['->', {
        text: 'Apply'.t(),
        iconCls: 'fa fa-floppy-o',
        handler: 'saveDevices'
    }]
});

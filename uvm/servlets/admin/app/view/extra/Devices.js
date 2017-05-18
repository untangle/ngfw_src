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

    items: [{
        xtype: 'ungrid',
        region: 'center',
        itemId: 'devicesgrid',
        reference: 'devicesgrid',
        title: 'Current Devices'.t(),
        store: 'devices',
        stateful: true,

        enableColumnHide: true,

        viewConfig: {
            stripeRows: true,
            enableTextSelection: true
        },

        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'delete'],
        emptyRow: {
            macAddress: '',
            macVendor: '',
            hostname: '',
            hostnameLastKnown: '',
            interfaceId: -1,
            lastSessionTime: 0,
            tags: {
                javaClass: 'java.util.LinkedList',
                list: []
            },
            username: '',
            javaClass: 'com.untangle.uvm.DeviceTableEntry'
        },

        plugins: [
        'gridfilters',
        {
            ptype: 'cellediting',
            clicksToEdit: 1
        }],

        columns: [{
            header: 'MAC'.t(),
            columns:[{
                header: 'Address'.t(),
                dataIndex: 'macAddress',
                filter: { type: 'string' },
                editor: {
                    xtype: 'textfield',
                    emptyText: '[no MAC Address]'.t()
                }
            }, {
                header: 'Vendor'.t(),
                dataIndex: 'macVendor',
                filter: { type: 'string' },
                editor: {
                    xtype: 'textfield',
                    emptyText: '[no MAC Vendor]'.t()
                }
            }]
        }, {
            header: 'Interface'.t(),
            dataIndex: 'interfaceId',
            filter: { type: 'string' },
            rtype: 'interface'
        }, {
            header: 'Last Hostname'.t(),
            dataIndex: 'hostnameLastKnown',
            filter: { type: 'string' },
            editor: {
                xtype: 'textfield',
                emptyText: ''.t()
            }
        }, {
            header: 'Hostname'.t(),
            dataIndex: 'hostname',
            filter: { type: 'string' },
            editor: {
                xtype: 'textfield',
                emptyText: '[no hostname]'.t()
            }
        }, {
            header: 'Username'.t(),
            dataIndex: 'username',
            filter: { type: 'string' },
            editor: {
                xtype: 'textfield',
                emptyText: '[no device username]'.t()
            }
        }, {
            header: 'HTTP'.t() + ' - ' + 'User Agent'.t(),
            dataIndex: 'httpUserAgent',
            flex: 1,
            filter: { type: 'string' },
            editor: {
                xtype: 'textfield',
                emptyText: '[no HTTP user agent]'.t()
            }
        }, {
            header: 'Last Seen Time'.t(),
            dataIndex: 'lastSessionTime',
            filter: { type: 'date' },
            rtype: 'timestamp'
        }, {
            header: 'Tags',
            dataIndex: 'tagsString',
            filter: { type: 'string' },
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
            bind: '{record.hostnameLastKnown}',
            fieldLabel: 'Last Hostname'.t(),
            emptyText: '[no last hostname]'.t(),
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
    }, {
        region: 'east',
        xtype: 'unpropertygrid',
        title: 'Device Details'.t(),
        itemId: 'details',
        collapsed: true,

        bind: {
            source: '{deviceDetails}'
        }
    }],
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
    },{
        xtype: 'ungridstatus'
    }, '->', {
        xtype: 'button',
        text: 'View Reports'.t(),
        iconCls: 'fa fa-line-chart',
        href: '#reports/devices',
        hrefTarget: '_self'
    }],
    bbar: ['->', {
        text: '<strong>' + 'Save'.t() + '</strong>',
        iconCls: 'fa fa-floppy-o',
        handler: 'saveDevices'
    }]
});

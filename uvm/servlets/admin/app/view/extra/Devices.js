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
        defaultSortable: true,

        enableColumnHide: true,

        viewConfig: {
            stripeRows: true,
            enableTextSelection: true,
            listeners: {
                // to avoid some focusing issues
                cellclick: function (view, cell, cellIndex, record, line, rowIndex, e) {
                    if (Ext.Array.contains(cell.getAttribute('class').split(' '), 'tag-cell')) {
                        return false;
                    }
                }
            }
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

        plugins: [ 'gridfilters', {
            ptype: 'cellediting',
            clicksToEdit: 1
        }],

        fields:[{
            name: 'macAddress',
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: 'macVendor',
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: 'interfaceId',
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: 'hostnameLastKnown',
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: 'hostname',
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: 'username',
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: 'httpUserAgent',
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: 'lastSessionTime',
            sortType: 'asTimestamp'
        }, {
            name: 'tags'
        }],

        columns: [{
            header: 'MAC'.t(),
            columns:[{
                header: 'Address'.t(),
                dataIndex: 'macAddress',
                width: Renderer.macWidth,
                filter: Renderer.stringFilter,
                editor: {
                    xtype: 'textfield',
                    emptyText: '[no MAC Address]'.t()
                }
            }, {
                header: 'Vendor'.t(),
                dataIndex: 'macVendor',
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter,
                editor: {
                    xtype: 'textfield',
                    emptyText: '[no MAC Vendor]'.t()
                }
            }]
        }, {
            header: 'Interface'.t(),
            dataIndex: 'interfaceId',
            width: Renderer.messageWidth,
            filter: Renderer.stringFilter,
            renderer: Renderer.interface
        }, {
            header: 'Last Hostname'.t(),
            dataIndex: 'hostnameLastKnown',
            width: Renderer.hostnameWidth,
            filter: Renderer.stringFilter,
            editor: {
                xtype: 'textfield',
                emptyText: ''
            }
        }, {
            header: 'Hostname'.t(),
            dataIndex: 'hostname',
            width: Renderer.hostnameWidth,
            filter: Renderer.stringFilter,
            editor: {
                xtype: 'textfield',
                emptyText: '[no hostname]'.t()
            }
        }, {
            header: 'Username'.t(),
            dataIndex: 'username',
            width: Renderer.usernameidth,
            filter: Renderer.stringFilter,
            editor: {
                xtype: 'textfield',
                emptyText: '[no device username]'.t()
            }
        }, {
            header: 'HTTP'.t() + ' - ' + 'User Agent'.t(),
            flex: 1,
            dataIndex: 'httpUserAgent',
            width: Renderer.messageWidth,
            filter: Renderer.stringFilter,
            editor: {
                xtype: 'textfield',
                emptyText: '[no HTTP user agent]'.t()
            }
        }, {
            header: 'Last Seen Time'.t(),
            dataIndex: 'lastSessionTime',
            width: Renderer.timestampWidth,
            renderer: Renderer.timestamp,
            filter: Renderer.timestampFilter
        }, {
            header: 'Tags'.t(),
            width: Renderer.tagsWidth,
            xtype: 'widgetcolumn',
            tdCls: 'tag-cell',
            widget: {
                xtype: 'tagpicker',
                bind: {
                    tags: '{record.tags}'
                }
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
        reference: 'masterGrid',
        title: 'Device Details'.t(),
        itemId: 'details',
        collapsed: true,
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
    },
    '-',
    {
        xtype: 'ungridfilter',
        store: 'devices'
    }, '->', {
        xtype: 'button',
        text: 'View Reports'.t(),
        iconCls: 'fa fa-line-chart',
        href: '#reports?cat=devices',
        hrefTarget: '_self',
        hidden: true,
        bind: {
            hidden: '{!reportsAppStatus.enabled}'
        }
    }],
    bbar: ['->', {
        text: '<strong>' + 'Save'.t() + '</strong>',
        iconCls: 'fa fa-floppy-o',
        handler: 'saveDevices'
    }]
});

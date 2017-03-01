Ext.define('Ung.view.shd.Devices', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.devices',

    /* requires-start */
    requires: [
        'Ung.view.shd.DevicesController'
    ],
    /* requires-end */
    controller: 'devices',

    layout: 'border',

    defaults: {
        border: false
    },

    viewModel: {
        formulas: {
            deviceDetails: function (get) {
                if (get('devicesgrid.selection')) {
                    var data = get('devicesgrid.selection').getData();
                    console.log(data);
                    delete data._id;
                    delete data.javaClass;
                    return data;
                }
                return;
            }
        }
    },

    items: [{
        xtype: 'grid',
        region: 'center',
        itemId: 'devicesgrid',
        reference: 'devicesgrid',
        title: 'Current Devices'.t(),
        store: 'devices',
        forceFit: true,
        columns: [
            { header: 'MAC Address'.t(), dataIndex: 'macAddress' },
            { header: 'MAC Vendor'.t(), dataIndex: 'macVendor' },
            { header: 'Interface'.t(), dataIndex: 'lastSeenInterfaceId' },
            { header: 'Hostname'.t(), dataIndex: 'hostname' },
            { header: 'Device Username'.t(), dataIndex: 'deviceUsername' },
            { header: 'HTTP'.t() + ' - ' + 'User Agent'.t(), dataIndex: 'httpUserAgent' },
            { header: 'Last Seen Time'.t(), dataIndex: 'lastSeenTimeDate' },
        ]
    }, {
        region: 'east',
        xtype: 'propertygrid',
        itemId: 'details',
        editable: false,
        width: 400,
        split: true,
        collapsible: false,
        resizable: true,
        shadow: false,
        hidden: true,

        cls: 'prop-grid',

        viewConfig: {
            stripeRows: false,
            getRowClass: function(record) {
                if (record.get('value') === null || record.get('value') === '') {
                    return 'empty';
                }
                return;
            }
        },

        nameColumnWidth: 200,
        bind: {
            // title: '{devicesgrid.selection.hostname} ({devicesgrid.selection.address})',
            source: '{deviceDetails}',
            hidden: '{!devicesgrid.selection}'
        },
        sourceConfig: {
            deviceUsername:      { displayName: 'Device Username'.t() },
            hostname:            { displayName: 'Hostname'.t() },
            hostnameKnown:       { displayName: 'Hostname Known'.t(), renderer: 'boolRenderer' },
            httpUserAgent:       { displayName: 'HTTP'.t() + ' - ' + 'User Agent'.t() },
            lastSeenInterfaceId: { displayName: 'Interface'.t() },
            lastSeenTime:        { displayName: 'Last Seen Time'.t(), renderer: 'timestampRenderer' },
            macAddress:          { displayName: 'MAC Address'.t() },
            macVendor:           { displayName: 'MAC Vendor'.t() }
        },
        listeners: {
            beforeedit: function () {
                return false;
            }
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
        text: 'Auto Refresh'.t(),
        iconCls: 'fa fa-refresh',
        enableToggle: true,
        toggleHandler: 'setAutoRefresh'
    }, '-', 'Filter:'.t(), {
        xtype: 'textfield',
        checkChangeBuffer: 200
    }]
});

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
            html: '<strong>' + 'Current Devices'.t() + '</strong>'
        }])
    }],

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
            { header: 'Hostname Last Known'.t(), dataIndex: 'hostnameLastKnown' },
            { header: 'Username'.t(), dataIndex: 'username' },
            { header: 'Tags'.t(), dataIndex: 'tags' },
            { header: 'Tags String'.t(), dataIndex: 'tagsString' },
            { header: 'HTTP'.t() + ' - ' + 'User Agent'.t(), dataIndex: 'httpUserAgent' },
            { header: 'Last Seen Time'.t(), dataIndex: 'lastSessionTimeDate' },
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
            username:            { displayName: 'Username'.t() },
            hostname:            { displayName: 'Hostname'.t() },
            hostnameLastKnown:   { displayName: 'HostnameLastKnown'.t() },
            httpUserAgent:       { displayName: 'HTTP'.t() + ' - ' + 'User Agent'.t() },
            lastSeenInterfaceId: { displayName: 'Interface'.t() },
            lastSessionTime:     { displayName: 'Last Seen Time'.t(), renderer: 'timestampRenderer' },
            macAddress:          { displayName: 'MAC Address'.t() },
            macVendor:           { displayName: 'MAC Vendor'.t() },
            tags:                { displayName: 'Tags'.t() },
            tagsString:          { displayName: 'Tags String'.t() },
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

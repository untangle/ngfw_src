Ext.define('Ung.apps.wanfailover.view.Tests', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-wan-failover-tests',
    itemId: 'tests',
    title: 'Tests'.t(),
    viewModel: true,

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'These tests control how each WAN interface is tested to ensure that it has connectivity to the Internet.'.t()
    }],

    items: [{
        xtype: 'displayfield',
        padding: '10 20 0 20',
        value: '<STRONG>' + 'Note'.t() + '</STRONG>'
    }, {
        xtype: 'displayfield',
        padding: '0 20 0 40',
        value: 'There should be one configured test per WAN interface.'.t() + '<BR>' +
               'These rules require careful configuration. Poorly chosen tests will greatly reduce the effectiveness of WAN Failover.'.t() + '<BR>' +
               'Press Help to see a further discussion about Failure Detection Tests.'.t()
    },{
        xtype: 'app-wan-failover-test-grid',
        title: 'Failure Detection Tests'.t(),
        width: 800,
        height: 400,
        padding: '20 20 20 20',
        border: true,
    }]

});

Ext.define('Ung.apps.wanfailover.view.TestGrid', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-wan-failover-test-grid',
    itemId: 'test-grid',

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add']
    }],

    recordActions: ['edit', 'delete'],
    listProperty: 'settings.tests.list',
    emptyRow: {
        javaClass: 'com.untangle.app.wan_failover.WanTestSettings',
        'enabled': true,
        'description': '',
        'type' :''
        },

    bind: '{tests}',

    columns: [{
        xtype: 'checkcolumn',
        header: 'Enabled'.t(),
        width: 80,
        dataIndex: 'enabled',
        resizable: false
    }, {
        header: 'Interface'.t(),
        width: 100,
        dataIndex: 'interfaceId',
    }, {
        header: 'Test Type'.t(),
        width: 100,
        dataIndex: 'type',
    }, {
        header: 'Description'.t(),
        width: 150,
        flex: 1,
        dataIndex: 'description'
    }],

    editorFields: [{
        xtype: 'checkbox',
        fieldLabel: 'Enabled'.t(),
        bind: '{record.enabled}'
    }, {
        xtype: 'textfield',
        fieldLabel: 'Interface'.t(),
        bind: '{record.interfaceId}'
    }, {
        xtype: 'textfield',
        fieldLabel: 'Description'.t(),
        bind: '{record.description}'
    }, {
        xtype: 'textfield',
        fieldLabel: 'Testing Interval (seconds)'.t(),
        bind: '{record.delayMilliseconds}'
    }, {
        xtype: 'textfield',
        fieldLabel: 'Timeout (seconds)'.t(),
        bind: '{record.timeoutMilliseconds}'
    }, {
        xtype: 'combo',
        fieldLabel: 'Failure Threshold'.t(),
        editable: false,
        bind: '{record.failureThreshold}',
        store: [['1','X1 of 10X'.t()],['2','X2 of 10X'.t()],['3','X3 of 10X'.t()],['4','X4 of 10X'.t()],['5','X5 of 10X'.t()],['6','X6 of 10X'.t()],['7','X7 of 10X'.t()],['8','X8 of 10X'.t()],['9','X9 of 10X'.t()],['10','X10 of 10X'.t()]]
    }, {
        xtype: 'combo',
        fieldLabel: 'Test Type'.t(),
        editable: false,
        bind: '{record.type}',
        store: [['ping','Ping'.t()],['arp','ARP'.t()],['dns','DNS'.t()],['http','HTTP'.t()]]
    }, {
        xtype: 'textfield',
        fieldLabel: 'IP'.t(),
        bind: {
            value: '{record.pingHostname}'
        }
    }, {
        xtype: 'textfield',
        fieldLabel: 'URL'.t(),
        bind: {
            value: '{record.httpUrl}'
        }
    }]

});

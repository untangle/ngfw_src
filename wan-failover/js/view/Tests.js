Ext.define('Ung.apps.wan-failover.view.Tests', {
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

Ext.define('Ung.apps.wan-failover.view.TestGrid', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-wan-failover-test-grid',
    itemId: 'test-grid',
    controller: 'app-wan-failover-special',

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'delete'],
    listProperty: 'settings.tests.list',
    bind: '{tests}',
    emptyRow: {
        javaClass: 'com.untangle.app.wan_failover.WanTestSettings',
        'enabled': true,
        'description': '',
        'type': 'ping',
        'interfaceId': 1,
        'timeoutMilliseconds': 2,
        'delayMilliseconds': 5,
        'failureThreshold': 3,
        'pingHostname': '8.8.8.8',
        'httpUrl': 'http://1.2.3.4/',
        'testHistorySize': 10
    },

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
        renderer: Ung.util.Renderer.interface
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
        xtype: 'combobox',
        fieldLabel: 'Interface'.t(),
        fieldIndex: 'interfaceCombo',
        bind: {
            value: '{record.interfaceId}',
            store: '{wanStatusStore}'
        },
        allowBlank: false,
        editable: false,
        queryMode: 'local',
        displayField: 'interfaceName',
        valueField: 'interfaceId'
    }, {
        xtype: 'textfield',
        fieldLabel: 'Description'.t(),
        bind: '{record.description}'
    }, {
        xtype: 'textfield',
        fieldLabel: 'Testing Interval (seconds)'.t(),
        bind: '{record.delayMilliseconds}',
    }, {
        xtype: 'textfield',
        fieldLabel: 'Timeout (seconds)'.t(),
        bind: '{record.timeoutMilliseconds}',
    }, {
        xtype: 'combobox',
        fieldLabel: 'Failure Threshold'.t(),
        editable: false,
        bind: '{record.failureThreshold}',
        store: [['1','X1 of 10X'.t()],['2','X2 of 10X'.t()],['3','X3 of 10X'.t()],['4','X4 of 10X'.t()],['5','X5 of 10X'.t()],['6','X6 of 10X'.t()],['7','X7 of 10X'.t()],['8','X8 of 10X'.t()],['9','X9 of 10X'.t()],['10','X10 of 10X'.t()]]
    }, {
        xtype: 'combobox',
        fieldLabel: 'Test Type'.t(),
        editable: false,
        bind: '{record.type}',
        store: [['ping','Ping'.t()],['arp','ARP'.t()],['dns','DNS'.t()],['http','HTTP'.t()]]
    }, {
        xtype: 'fieldset',
        layout: 'column',
        border: false,
        anchor: '100%',
        items: [{
            xtype: 'combobox',
            fieldLabel: 'IP'.t(),
            fieldIndex: 'pingCombo',
            labelAlign: 'right',
            labelWidth: 170,
            bind: {
                value: '{record.pingHostname}',
                hidden: '{record.type != "ping"}',
                store: '{pingListStore}'

            },
            allowBlank: false,
            editable: true,
            queryMode: 'local',
            displayField: 'name',
            valueField: 'addr'
        }, {
            xtype: 'button',
            text: 'Generate Suggestions'.t(),
            margin: '0 0 0 10',
            iconCls: 'fa fa-book',
            handler: function(btn) {
                this.up('grid').getController().generateSuggestions(btn);
            }
        }]
    }, {
        xtype: 'textfield',
        fieldLabel: 'URL'.t(),
        bind: {
            value: '{record.httpUrl}',
            hidden: '{record.type != "http"}'
        }
    }, {
        xtype: 'button',
        text: 'Run Test'.t(),
        iconCls: 'fa fa-play',
        anchor: 'left',
        margin: '10 0 0 185',
        width: 140,
        height: 30,
        handler: function(btn) {
            this.up('grid').getController().runWanTest(btn);
        },
    }]

});

Ext.define('Ung.config.network.view.FilterRules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-filter-rules',
    itemId: 'filter-rules',
    viewModel: true,
    scrollable: true,

    title: 'Filter Rules'.t(),

    layout: 'fit',

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: 'Filter Rules control what sessions are passed/blocked. Filter rules process all sessions including bypassed sessions. The rules are evaluated in order.'.t()
    }],

    items: [{
        xtype: 'ungrid',
        region: 'center',
        title: 'Filter Rules'.t(),

        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'delete', 'reorder'],

        emptyText: 'No Filter Rules defined'.t(),

        listProperty: 'settings.filterRules.list',

        emptyRow: {
            ruleId: -1,
            enabled: true,
            ipvsEnabled: false,
            description: '',
            javaClass: 'com.untangle.uvm.network.FilterRule',
            conditions: {
                javaClass: 'java.util.LinkedList',
                list: []
            },
            blocked: false
        },

        bind: '{filterRules}',

        columns: [{
            header: 'Rule Id'.t(),
            width: Renderer.idWidth,
            align: 'right',
            resizable: false,
            dataIndex: 'ruleId',
            renderer: function (value) {
                return value < 0 ? 'new'.t() : value;
            }
        }, {
            xtype: 'checkcolumn',
            header: 'Enable'.t(),
            dataIndex: 'enabled',
            resizable: false,
            width: Renderer.booleanWidth,
        }, {
            xtype: 'checkcolumn',
            header: 'IPv6'.t(),
            dataIndex: 'ipv6Enabled',
            resizable: false,
            width: Renderer.booleanWidth,
        }, {
            header: 'Description',
            width: Renderer.messageWidth,
            flex: 1,
            dataIndex: 'description',
            renderer: function (value) {
                return value || '<em>no description<em>';
            }
        },
        Column.conditions,
        {
            xtype: 'checkcolumn',
            width: Renderer.booleannWidth,
            header: 'Block'.t(),
            dataIndex: 'blocked',
            resizable: false
        }],
        editorFields: [
            Field.enableRule('Enable Filter Rule'.t()),
            Field.enableIpv6,
            Field.description,
            Field.conditions(
                'com.untangle.uvm.network.FilterRuleCondition',[
                "DST_ADDR",
                "DST_PORT",
                "DST_INTF",
                "SRC_MAC" ,
                "SRC_ADDR",
                "SRC_PORT",
                "SRC_INTF",
                "PROTOCOL",
                "CLIENT_TAGGED",
                "SERVER_TAGGED"
            ]),
            Field.blockedCombo
        ]
    }]
});

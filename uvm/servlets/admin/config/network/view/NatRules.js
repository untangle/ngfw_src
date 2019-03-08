Ext.define('Ung.config.network.view.NatRules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-nat-rules',
    itemId: 'nat-rules',
    scrollable: true,

    viewModel: true,

    title: 'NAT Rules'.t(),

    layout: 'fit',

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: 'NAT Rules control the rewriting of the IP source address of traffic (Network Address Translation). The rules are evaluated in order.'.t()
    }],

    items: [{
        xtype: 'ungrid',
        flex: 3,

        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'delete', 'reorder'],

        emptyText: 'No NAT Rules defined'.t(),

        listProperty: 'settings.natRules.list',

        emptyRow: {
            ruleId: -1,
            enabled: true,
            auto: true,
            javaClass: 'com.untangle.uvm.network.NatRule',
            conditions: {
                javaClass: 'java.util.LinkedList',
                list: []
            },
            description: ''
        },

        bind: '{natRules}',

        columns: [{
            header: 'Rule Id'.t(),
            width: Renderer.idWidth,
            align: 'right',
            resizable: false,
            dataIndex: 'ruleId',
            renderer: Renderer.id
        }, {
            xtype: 'checkcolumn',
            header: 'Enable'.t(),
            dataIndex: 'enabled',
            resizable: false,
            width: Renderer.booleanWidth,
        }, {
            header: 'Description',
            width: Renderer.messageWidth,
            dataIndex: 'description'
        },
        Column.conditions,
        {
            header: 'NAT Type'.t(),
            dataIndex: 'auto',
            width: Renderer.idWidth,
            renderer: Ung.config.network.MainController.natTypeRenderer
        }, {
            header: 'New Source'.t(),
            dataIndex: 'newSource',
            width: Renderer.networkWidth,
            renderer: Ung.config.network.MainController.natNewSourceRenderer
        }],
        editorFields: [
            Field.enableRule('Enable NAT Rule'.t()),
            Field.description,
            Field.conditions(
                'com.untangle.uvm.network.NatRuleCondition',[
                "DST_ADDR",
                "DST_PORT",
                "DST_INTF",
                "SRC_ADDR",
                "SRC_PORT",
                "SRC_INTF",
                "PROTOCOL",
                "CLIENT_TAGGED",
                "SERVER_TAGGED"
            ]),
            Field.natType,
            Field.natSource
        ]
    }]
});

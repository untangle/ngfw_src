Ext.define('Ung.config.network.view.PortForwardRules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-port-forward-rules',
    itemId: 'port-forward-rules',
    scrollable: true,

    viewModel: true,

    title: 'Port Forward Rules'.t(),

    layout: { type: 'vbox', align: 'stretch' },

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: 'Port Forward rules forward sessions matching the configured criteria from a public IP to an IP on an internal (NAT\'d) network. The rules are evaluated in order.'.t()
    }],

    items: [{
        xtype: 'ungrid',
        name: 'Port Forward Rules',
        flex: 3,

        simpleEditorAlias: 'config-network-portforwardsimple',

        tbar: ['@addSimple', '->', '@import', '@export'],
        recordActions: ['edit', 'delete', 'reorder'],

        listProperty: 'settings.portForwardRules.list',
        recordJavaClass: 'com.untangle.uvm.network.PortForwardRule',

        emptyText: 'No Port Forward Rules defined'.t(),

        actionText: 'Forward to the following location:'.t(),
        emptyRow: {
            ruleId: -1,
            simple: false, // set simple by default on false, and only when saving simple rules will be set true
            enabled: true,
            description: '',
            javaClass: 'com.untangle.uvm.network.PortForwardRule',
            conditions: {
                javaClass: 'java.util.LinkedList',
                list: [{
                    conditionType: 'DST_LOCAL',
                    invert: false,
                    value: 'true',
                    javaClass: 'com.untangle.uvm.network.PortForwardRuleCondition'
                }, {
                    conditionType: 'PROTOCOL',
                    invert: false,
                    value: 'TCP',
                    javaClass: 'com.untangle.uvm.network.PortForwardRuleCondition'
                }, {
                    conditionType:'DST_PORT',
                    invert: false,
                    value: '80',
                    javaClass: 'com.untangle.uvm.network.PortForwardRuleCondition'
                }]
            },
            newPort: 80
        },

        bind: '{portForwardRules}',

        columns: [
            Column.ruleId,
            Column.enabled,
            Column.description,
            Column.conditions, {
                header: 'New Destination'.t(),
                dataIndex: 'newDestination',
                width: Renderer.messageWidth
            }, {
                header: 'New Port'.t(),
                dataIndex: 'newPort',
                width: Renderer.portWidth
            }],
        editorFields: [
            Field.enableRule('Enable Port Forward Rule'.t()),
            Field.description,
            Field.conditions(
                'com.untangle.uvm.network.PortForwardRuleCondition',[
                "DST_LOCAL",
                "DST_ADDR",
                "DST_PORT",
                "SRC_ADDR",
                "SRC_PORT",
                "SRC_INTF",
                "PROTOCOL",
                "CLIENT_TAGGED",
                "SERVER_TAGGED"
            ]),
            Field.newDestination,
            Field.newPort
        ]
    }, {
        xtype: 'fieldset',
        flex: 2,
        margin: 10,
        padding: 10,
        // border: true,
        collapsible: false,
        collapsed: false,
        autoScroll: true,
        style: {
            lineHeight: 1.4
        },
        title: 'The following ports are currently reserved and can not be forwarded:'.t(),
        items: [{
            xtype: 'component',
            bind: {
                html: '{portForwardWarnings}'
            }
        }]
    }]
});

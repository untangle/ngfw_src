Ext.define('Ung.config.system.view.Shield', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-system-shield',
    itemId: 'shield',
    viewModel: true,
    scrollable: true,

    title: 'Shield'.t(),

    layout: 'fit',

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        padding: '8 5',
        style: { fontSize: '12px' },
        items: [{
            xtype: 'checkbox',
            boxLabel: '<strong>' + 'Enable Shield'.t() + '</strong>',
            bind: '{shieldSettings.shieldEnabled}'
        }]
    }],


    items: [{
        xtype: 'ungrid',
        border: false,
        title: 'Shield Rules'.t(),

        emptyText: 'No Shield Rules defined'.t(),

        disabled: true,
        bind: {
            disabled: '{!shieldSettings.shieldEnabled}',
            store: '{shieldRules}'
        },

        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'delete', 'reorder'],

        listProperty: 'shieldSettings.rules.list',

        emptyRow: {
            ruleId: -1,
            enabled: true,
            description: '',
            action: 'SCAN',
            javaClass: 'com.untangle.app.shield.ShieldRule',
            conditions: {
                javaClass: 'java.util.LinkedList',
                list: []
            }
        },

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
            header: 'Description'.t(),
            width: Renderer.messageWidth,
            dataIndex: 'description',
        },
        Column.conditions,
        {
            header: 'Action'.t(),
            width: Renderer.messageWidth,
            dataIndex: 'action',
            renderer: Ung.config.system.MainController.shieldActionRenderer
        }],
        editorFields: [
            Field.enableRule(),
            Field.description,
            Field.conditions(
                'com.untangle.app.shield.ShieldRuleCondition', [
                "DST_ADDR",
                "DST_PORT",
                "DST_INTF",
                "SRC_ADDR",
                "SRC_PORT",
                "SRC_INTF",
                "PROTOCOL",
                "CLIENT_TAGGED",
                "SERVER_TAGGED"
            ]), {
                xtype: 'combo',
                fieldLabel: 'Action',
                allowBlank: false,
                editable: false,
                bind: '{record.action}',
                store: [
                    ['SCAN', 'Scan'.t()],
                    ['PASS', 'Pass'.t()]
                ],
                queryMode: 'local'
            }
        ]


    }]

});

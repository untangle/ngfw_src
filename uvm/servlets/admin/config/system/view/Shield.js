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
        ruleJavaClass: 'com.untangle.app.shield.ShieldRuleCondition',

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

        conditions: [
            {name:"DST_ADDR",displayName: "Destination Address".t(), type: "textfield", visible: true, vtype:"ipMatcher"},
            {name:"DST_PORT",displayName: "Destination Port".t(), type: "textfield",vtype:"portMatcher", visible: true},
            {name:"DST_INTF",displayName: "Destination Interface".t(), type: "checkboxgroup", values: Util.getInterfaceList(true, true), visible: true},
            {name:"SRC_ADDR",displayName: "Source Address".t(), type: "textfield", visible: true, vtype:"ipMatcher"},
            {name:"SRC_PORT",displayName: "Source Port".t(), type: "textfield",vtype:"portMatcher", visible: Rpc.directData('rpc.isExpertMode')},
            {name:"SRC_INTF",displayName: "Source Interface".t(), type: "checkboxgroup", values: Util.getInterfaceList(true, true), visible: true},
            {name:"PROTOCOL",displayName: "Protocol".t(), type: "checkboxgroup", values: [["TCP","TCP"],["UDP","UDP"]], visible: true},
            {name:"CLIENT_TAGGED",displayName: 'Client Tagged'.t(), type: 'textfield', visible: true},
            {name:"SERVER_TAGGED",displayName: 'Server Tagged'.t(), type: 'textfield', visible: true},
        ],

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
        }, {
            header: 'Conditions'.t(),
            width: Renderer.conditionsWidth,
            flex: 1,
            dataIndex: 'conditions',
            renderer: 'conditionsRenderer'
        }, {
            header: 'Action'.t(),
            width: Renderer.messageWidth,
            dataIndex: 'action',
            renderer: Ung.config.system.MainController.shieldActionRenderer
        }],
        editorFields: [
            Field.enableRule(),
            Field.description,
            Field.conditions, {
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

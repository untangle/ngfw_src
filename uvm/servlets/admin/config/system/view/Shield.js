Ext.define('Ung.config.system.view.Shield', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.system.shield',
    itemId: 'shield',


    viewModel: true,

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

        disabled: true,
        bind: {
            disabled: '{!shieldSettings.shieldEnabled}',
            store: '{shieldRules}'
        },

        tbar: ['@add'],
        recordActions: ['edit', 'delete', 'reorder'],

        listProperty: 'shieldSettings.rules.list',
        ruleJavaClass: 'com.untangle.node.shield.ShieldRuleCondition',

        emptyRow: {
            ruleId: -1,
            enabled: true,
            description: '',
            action: 'SCAN',
            javaClass: 'com.untangle.node.shield.ShieldRule',
            conditions: {
                javaClass: 'java.util.LinkedList',
                list: []
            }
        },

        conditions: [Condition.dstAddr, Condition.dstPort, Condition.dstIntf, Condition.srcAddr, Condition.srcPort, Condition.srcIntf,
            Condition.protocol([['TCP','TCP'],['UDP','UDP']])
        ],

        columns: [{
            header: 'Rule Id'.t(),
            width: 70,
            align: 'right',
            resizable: false,
            dataIndex: 'ruleId',
            renderer: function(value) {
                return value < 0 ? 'new'.t() : value;
            }
        }, {
            xtype: 'checkcolumn',
            header: 'Enable'.t(),
            dataIndex: 'enabled',
            resizable: false,
            width: 70
        }, {
            header: 'Description'.t(),
            width: 200,
            dataIndex: 'description',
            renderer: function (value) {
                return value || '<em>no description<em>';
            }
        }, {
            header: 'Conditions'.t(),
            flex: 1,
            dataIndex: 'conditions',
            renderer: 'conditionsRenderer'
        }, {
            header: 'Action'.t(),
            width: 150,
            dataIndex: 'action',
            renderer: function (value) {
                var action;
                switch (value) {
                case 'SCAN': action = 'Scan'.t(); break;
                case 'PASS': action = 'Pass'.t(); break;
                default: action = 'Unknown Action: ' + value;
                }
                return action;
            }
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

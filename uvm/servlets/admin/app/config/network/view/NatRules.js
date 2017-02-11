Ext.define('Ung.config.network.view.NatRules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.network.natrules',

    viewModel: true,

    requires: [
        // 'Ung.config.network.ConditionWidget',
        // 'Ung.config.network.CondWidget'
    ],

    title: 'NAT Rules'.t(),

    layout: 'fit',

    tbar: [{
        xtype: 'displayfield',
        padding: '0 10',
        value: 'NAT Rules control the rewriting of the IP source address of traffic (Network Address Translation). The rules are evaluated in order.'.t()
    }],

    items: [{
        xtype: 'rules',
        flex: 3,

        tbar: ['@add'],
        recordActions: ['@edit', '@delete', '@reorder'],

        listProperty: 'settings.natRules.list',
        ruleJavaClass: 'com.untangle.uvm.network.NatRuleCondition',

        conditions: [
            { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', visible: true, vtype:'ipall'},
            { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield', vtype:'port', visible: true},
            { name: 'DST_INTF', displayName: 'Destination Interface'.t(), type: 'checkboxgroup', values: [['a', 'a'], ['b', 'b']], visible: true},
            { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', visible: true, vtype:'ipall'},
            { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'numberfield', vtype:'port', visible: rpc.isExpertMode},
            { name: 'SRC_INTF', displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: [['a', 'a'], ['b', 'b']], visible: true},
            { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP','TCP'],['UDP','UDP'],['ICMP','ICMP'],['GRE','GRE'],['ESP','ESP'],['AH','AH'],['SCTP','SCTP']], visible: true}
        ],

        description: "NAT Rules control the rewriting of the IP source address of traffic (Network Address Translation). The rules are evaluated in order.".t(),

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
            width: 70,
            editor: {
                xtype: 'checkbox',
                fieldLabel: 'Enable NAT Rule'.t(),
                bind: '{record.enabled}',
            }
            // renderer: function (val) {
            //     return '<i class="fa + ' + (val ? 'fa-check' : 'fa-check-o') + '"></i>';
            // }
        }, {
            header: 'Description',
            width: 200,
            dataIndex: 'description',
            renderer: function (value) {
                if (value) {
                    return value;
                }
                return '<em>no description<em>';
            },
            editor: {
                xtype: 'textfield',
                fieldLabel: 'Description'.t(),
                bind: '{record.description}',
                emptyText: '[no description]'.t(),
                allowBlank: false
            }
        }, {
            header: 'Conditions'.t(),
            itemId: 'conditions',
            flex: 1,
            dataIndex: 'conditions',
            renderer: 'conditionsRenderer'
        },
        // {
        //     xtype: 'actioncolumn', //
        //     iconCls: 'fa fa-edit',
        //     handler: 'editRuleWin'
        // },
        {
            header: 'NAT Type'.t(),
            dataIndex: 'auto',
            width: 100,
            renderer: function (val) {
                return val ? 'Auto'.t() : 'Custom'.t();
            },
            editor: {
                xtype: 'combo',
                fieldLabel: 'NAT Type'.t(),
                bind: '{record.auto}',
                allowBlank: false,
                editable: false,
                store: [[true, 'Auto'.t()], [false, 'Custom'.t()]],
                queryMode: 'local'
                // vtype: 'ipall'
            }
        }, {
            header: 'New Source'.t(),
            dataIndex: 'newSource',
            // align: 'right',
            width: 120,
            renderer: function (value, metaData, record) {
                return record.get('auto') ? '' : value;
                // if (record.get('auto')) {
                //     return '<span style="color: #999;">' + value + '</span>';
                // }
                // return value;
            },
            editor: {
                xtype: 'textfield',
                fieldLabel: 'New Source'.t(),
                width: 100,
                bind: {
                    value: '{record.newSource}',
                    disabled: '{record.auto}'
                },
                allowBlank: true,
                vtype: 'ipall'
            }
        }],
    }]
});
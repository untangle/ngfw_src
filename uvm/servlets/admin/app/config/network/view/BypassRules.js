Ext.define('Ung.config.network.view.BypassRules', {
    extend: 'Ext.panel.Panel',
    // xtype: 'ung.config.network.bypassrules',
    alias: 'widget.config.network.bypassrules',

    viewModel: true,

    requires: [
        // 'Ung.config.network.ConditionWidget',
        // 'Ung.config.network.CondWidget'
    ],

    title: 'Bypass Rules'.t(),

    layout: 'fit',

    tbar: [{
        xtype: 'displayfield',
        padding: '0 10',
        value: 'Bypass Rules control what traffic is scanned by the applications. Bypassed traffic skips application processing. The rules are evaluated in order. Sessions that meet no rule are not bypassed.'.t()
    }],

    items: [{
        xtype: 'rules',
        flex: 3,
        columnFeatures: ['reorder', 'delete', 'edit'], // which columns to add
        recordActions: ['@edit', '@delete'],

        listProperty: 'settings.bypassRules.list',
        ruleJavaClass: 'com.untangle.uvm.network.BypassRuleCondition',

        conditions: [
            { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', visible: true, vtype:'ipall'},
            { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield', vtype:'port', visible: true},
            { name: 'DST_INTF', displayName: 'Destination Interface'.t(), type: 'checkboxgroup', values: [['a', 'a'], ['b', 'b']], visible: true},
            { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', visible: true, vtype:'ipall'},
            { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'numberfield', vtype:'port', visible: rpc.isExpertMode},
            { name: 'SRC_INTF', displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: [['a', 'a'], ['b', 'b']], visible: true},
            { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP','TCP'],['UDP','UDP'],['ICMP','ICMP'],['GRE','GRE'],['ESP','ESP'],['AH','AH'],['SCTP','SCTP']], visible: true}
        ],

        label: 'Perform the following action(s):'.t(),
        description: "NAT Rules control the rewriting of the IP source address of traffic (Network Address Translation). The rules are evaluated in order.".t(),

        emptyRow: {
            ruleId: -1,
            enabled: true,
            bypass: true,
            javaClass: 'com.untangle.uvm.network.BypassRule',
            conditions: {
                javaClass: 'java.util.LinkedList',
                list: []
            },
            description: ''
        },

        bind: '{bypassRules}',

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
            header: 'Bypass'.t(),
            xtype: 'checkcolumn',
            dataIndex: 'bypass',
            width: 100,
            editor: {
                xtype: 'combo',
                fieldLabel: 'Action'.t(),
                bind: '{record.bypass}',
                editable: false,
                store: [[true, 'Bypass'.t()], [false, 'Process'.t()]],
                queryMode: 'local'
                // vtype: 'ipall'
            }
        }],
    }]
});
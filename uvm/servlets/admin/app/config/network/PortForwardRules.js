Ext.define('Ung.config.network.PortForwardRules', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.config.network.portforwardrules',

    viewModel: true,

    requires: [
        // 'Ung.config.network.ConditionWidget',
        // 'Ung.config.network.CondWidget'
    ],

    title: 'Port Forward Rules'.t(),

    layout: { type: 'vbox', align: 'stretch' },

    tbar: [{
        xtype: 'displayfield',
        value: "Port Forward rules forward sessions matching the configured criteria from a public IP to an IP on an internal (NAT'd) network. The rules are evaluated in order.".t()
    }],

    items: [{
        xtype: 'ung.cmp.rules',
        flex: 3,
        columnFeatures: ['reorder', 'delete', 'edit'], // which columns to add
        recordActions: ['@edit', '@delete'],

        conditions: [
            { name: 'DST_LOCAL', displayName: 'Destined Local'.t(), type: 'boolean', visible: true},
            { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', visible: true, vtype:'ipall'},
            { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield', vtype:'port', visible: true},
            { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', visible: true, vtype:'ipall'},
            { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'textfield', vtype:'port', visible: rpc.isExpertMode},
            { name: 'SRC_INTF', displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: [['a', 'a'], ['b', 'b']], visible: true},
            { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP','TCP'],['UDP','UDP'],['ICMP','ICMP'],['GRE','GRE'],['ESP','ESP'],['AH','AH'],['SCTP','SCTP']], visible: true}
        ],

        emptyRow: {
            ruleId: -1,
            simple: true,
            enabled: true,
            // description: '',
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

        bind: {
            store: '{portforwardrules}'
        },

        modelValidation: true,

        sortableColumns: false,
        enableColumnHide: false,

        columns: [{
            header: 'Rule Id'.t(),
            width: 70,
            align: 'right',
            resizable: false,
            dataIndex: 'ruleId',
            renderer: function(value) {
                if (value < 0) {
                    return 'new'.t();
                } else {
                    return value;
                }
            }
        }, {
            xtype:'checkcolumn',
            header: 'Enable'.t(),
            dataIndex: 'enabled',
            resizable: false,
            width: 70,
            // renderer: function (val) {
            //     return '<i class="fa + ' + (val ? 'fa-check' : 'fa-check-o') + '"></i>';
            // }
        }, {
            header: 'Description',
            width: 200,
            dataIndex: 'description',
            editor: {
                xtype: 'textfield',
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
            header: 'New Destination'.t(),
            dataIndex: 'newDestination',
            width: 150,
            editor: {
                xtype: 'textfield',
                allowBlank: false,
                vtype: 'ipall'
            }
        }, {
            header: 'New Port'.t(),
            dataIndex: 'newPort',
            // align: 'right',
            width: 80,
            editor: {
                xtype: 'numberfield',
                allowBlank: true,
                minValue : 1,
                maxValue : 0xFFFF,
                vtype: 'port'
            }
        }],

        editFields: [{

        }]
    }, {
        xtype: 'fieldset',
        flex: 2,
        margin: 10,
        // border: true,
        collapsible: true,
        collapsed: false,
        autoScroll: true,
        title: 'The following ports are currently reserved and can not be forwarded:'.t(),
        items: [{
            xtype: 'component',
            name: 'portForwardWarnings',
            html: ' '
        }]
    }]
});
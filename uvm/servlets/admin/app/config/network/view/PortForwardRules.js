Ext.define('Ung.config.network.view.PortForwardRules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.network.portforwardrules',

    viewModel: true,

    requires: [
        // 'Ung.config.network.ConditionWidget',
        // 'Ung.config.network.CondWidget'
    ],

    title: 'Port Forward Rules'.t(),

    layout: { type: 'vbox', align: 'stretch' },

    tbar: [{
        xtype: 'displayfield',
        padding: '0 10',
        value: "Port Forward rules forward sessions matching the configured criteria from a public IP to an IP on an internal (NAT'd) network. The rules are evaluated in order.".t()
    }],

    items: [{
        xtype: 'rules',
        flex: 3,

        tbar: ['@add'],
        recordActions: ['@edit', '@delete', '@reorder'],

        listProperty: 'settings.portForwardRules.list',
        ruleJavaClass: 'com.untangle.uvm.network.PortForwardRuleCondition',

        conditions: [
            { name: 'DST_LOCAL', displayName: 'Destined Local'.t(), type: 'boolean' },
            { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', vtype:'ipMatcher' },
            { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield', vtype:'portMatcher' },
            { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', vtype:'ipMatcher' },
            { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'numberfield', vtype:'portMatcher' },
            { name: 'SRC_INTF', displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: Ung.Util.getInterfaceList(true, true) },
            { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP','TCP'],['UDP','UDP'],['ICMP','ICMP'],['GRE','GRE'],['ESP','ESP'],['AH','AH'],['SCTP','SCTP']] }
        ],

        actionDescription: 'Forward to the following location:'.t(),
        description: "Port Forward rules forward sessions matching the configured criteria from a public IP to an IP on an internal (NAT'd) network. The rules are evaluated in order.".t(),

        emptyRow: {
            ruleId: -1,
            simple: true,
            enabled: true,
            // description: '',
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
                fieldLabel: 'Enable Port Forward Rule'.t(),
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
                return value || '<em>no description<em>';
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
            header: 'New Destination'.t(),
            dataIndex: 'newDestination',
            width: 150,
            editor: {
                xtype: 'textfield',
                fieldLabel: 'New Destination'.t(),
                bind: '{record.newDestination}',
                allowBlank: false,
                vtype: 'ipAddress'
            }
        }, {
            header: 'New Port'.t(),
            dataIndex: 'newPort',
            // align: 'right',
            width: 80,
            editor: {
                xtype: 'numberfield',
                fieldLabel: 'New Port'.t(),
                width: 100,
                bind: '{record.newPort}',
                allowBlank: true,
                minValue : 1,
                maxValue : 0xFFFF,
                vtype: 'port'
            }
        }],
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
            // name: 'portForwardWarnings',
            bind: {
                html: '{portForwardWarnings}'
            }
        }]
    }]
});
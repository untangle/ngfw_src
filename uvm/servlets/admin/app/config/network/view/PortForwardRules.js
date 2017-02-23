Ext.define('Ung.config.network.view.PortForwardRules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.network.portforwardrules',
    itemId: 'portForwardRules',

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
        flex: 3,

        tbar: ['@add'],
        recordActions: ['@edit', '@delete', '@reorder'],

        listProperty: 'settings.portForwardRules.list',
        ruleJavaClass: 'com.untangle.uvm.network.PortForwardRuleCondition',

        conditions: [
            Cond.dstLocal,
            Cond.dstAddr,
            Cond.dstPort,
            Cond.srcAddr,
            Cond.srcPort,
            Cond.srcIntf,
            Cond.protocol([['TCP','TCP'],['UDP','UDP'],['ICMP','ICMP'],['GRE','GRE'],['ESP','ESP'],['AH','AH'],['SCTP','SCTP']])
        ],

        actionText: 'Forward to the following location:'.t(),
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
            width: 70
        }, {
            header: 'Description',
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
            header: 'New Destination'.t(),
            dataIndex: 'newDestination',
            width: 150
        }, {
            header: 'New Port'.t(),
            dataIndex: 'newPort',
            width: 80
        }],
        editorFields: [
            Fields.enableRule('Enable Port Forward Rule'.t()),
            Fields.description,
            Fields.conditions,
            Fields.newDestination,
            Fields.newPort
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
            // name: 'portForwardWarnings',
            bind: {
                html: '{portForwardWarnings}'
            }
        }]
    }]
});

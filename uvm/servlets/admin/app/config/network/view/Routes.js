Ext.define('Ung.config.network.view.Routes', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.network.routes',

    viewModel: true,

    title: 'Routes'.t(),

    layout: 'border',

    tbar: [{
        xtype: 'displayfield',
        padding: '0 10',
        value: "Static Routes are global routes that control how traffic is routed by destination address. The most specific Static Route is taken for a particular packet, order is not important.".t()
    }],

    items: [{
        xtype: 'rules',
        region: 'center',

        // cfg: {

            columnFeatures: ['reorder', 'delete', 'edit'], // which columns to add
            recordActions: ['@edit', '@delete'],

            dataProperty: 'settings.staticRoutes.list',

            conditions: [
                { name: 'DST_LOCAL', displayName: 'Destined Local'.t(), type: 'boolean', visible: true},
                { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', visible: true, vtype:'ipall'},
                { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield', vtype:'port', visible: true},
                { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', visible: true, vtype:'ipall'},
                { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'numberfield', vtype:'port', visible: rpc.isExpertMode},
                { name: 'SRC_INTF', displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: [['a', 'a'], ['b', 'b']], visible: true},
                { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP','TCP'],['UDP','UDP'],['ICMP','ICMP'],['GRE','GRE'],['ESP','ESP'],['AH','AH'],['SCTP','SCTP']], visible: true}
            ],

            label: 'Forward to the following location:'.t(),
            description: "Port Forward rules forward sessions matching the configured criteria from a public IP to an IP on an internal (NAT'd) network. The rules are evaluated in order.".t(),

        emptyRow: {
            ruleId: -1,
            network: '',
            prefix: 24,
            nextHop: '4.3.2.1',
            javaClass: 'com.untangle.uvm.network.StaticRoute',
            description: ''
        },
        // },

        bind: {
            store: {
                data: '{settings.staticRoutes.list}'
            }
        },

        columns: [{
            header: 'Description'.t(),
            dataIndex: 'description',
            flex: 1,
            editor: {
                xtype: 'textfield',
                fieldLabel: 'Description'.t(),
                bind: '{record.description}',
                allowBlank: false,
                emptyText: '[enter description]'.t()
            }
        }, {
            header: 'Network'.t(),
            width: 170,
            dataIndex: 'network',
            editor: {
                xtype: 'textfield',
                fieldLabel: 'Network'.t(),
                emptyText: '1.2.3.0'.t(),
                allowBlank: false,
                vtype: 'ipall',
                bind: '{record.network}',
            }
        }, {
            header: 'Netmask/Prefix'.t(),
            width: 170,
            dataIndex: 'prefix',
            editor: {
                xtype: 'combo',
                fieldLabel: 'Netmask/Prefix'.t(),
                bind: '{record.prefix}',
                store: Ung.Util.getV4NetmaskList(false),
                queryMode: 'local',
                editable: false
            }
        }, {
            header: 'Next Hop'.t(),
            width: 300,
            dataIndex: 'nextHop',
            // renderer: function (value) {
            //     if (value) {
            //         return value;
            //     }
            //     return '<em>no description<em>';
            // },
            editor: {
                xtype: 'combo',
                fieldLabel: 'Next Hop'.t(),
                bind: '{record.nextHop}',
                // store: Ung.Util.getV4NetmaskList(false),
                queryMode: 'local',
                allowBlank: false,
                editable: true
            }
        }, {
            hidden: true,
            editor: {
                xtype: 'component',
                margin: '10 0 0 20',
                html: 'If <b>Next Hop</b> is an IP address that network will be routed via the specified IP address.'.t() + '<br/>' +
                    'If <b>Next Hop</b> is an interface that network will be routed <b>locally</b> on that interface.'.t()
            }
        }],
    }, {
        xtype: 'panel',
        title: 'Current Routes'.t(),
        region: 'south',
        height: '50%',
        split: true,
        border: false,
        tbar: [{
            xtype: 'displayfield',
            padding: '0 5',
            value: "Current Routes shows the current routing system's configuration and how all traffic will be routed.".t()
        }, '->', {
            text: 'Refresh Routes'.t(),
            iconCls: 'fa fa-refresh',
            handler: 'refreshRoutes'
        }],
        layout: 'fit',
        items: [{
            xtype: 'textarea',
            itemId: 'currentRoutes',
            border: false,
            fieldStyle: {
                fontFamily: 'monospace'
            },
            // name: "state",
            hideLabel: true,
            labelSeperator: '',
            readOnly: true,
            // autoCreate: { tag: 'textarea', autocomplete: 'off', spellcheck: 'false' },
            // height: 200,
            // width: "100%",
            // isDirty: function() { return false; }
        }]
    }]
});
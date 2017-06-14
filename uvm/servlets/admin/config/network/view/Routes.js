Ext.define('Ung.config.network.view.Routes', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-routes',
    itemId: 'routes',

    viewModel: true,

    title: 'Routes'.t(),

    layout: 'border',

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: 'Static Routes are global routes that control how traffic is routed by destination address. The most specific Static Route is taken for a particular packet, order is not important.'.t()
    }],

    items: [{
        xtype: 'ungrid',
        region: 'center',
        title: 'Static Routes'.t(),

        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'delete', 'reorder'],

        listProperty: 'settings.staticRoutes.list',

        emptyRow: {
            ruleId: -1,
            network: '',
            prefix: 24,
            nextHop: '4.3.2.1',
            javaClass: 'com.untangle.uvm.network.StaticRoute',
            description: ''
        },

        bind: '{staticRoutes}',

        columns: [{
            header: 'Description'.t(),
            dataIndex: 'description',
            flex: 1
        }, {
            header: 'Network'.t(),
            width: 170,
            dataIndex: 'network'
        }, {
            header: 'Netmask/Prefix'.t(),
            width: 170,
            dataIndex: 'prefix'
        }, {
            header: 'Next Hop'.t(),
            width: 170,
            dataIndex: 'nextHop',
            renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store, view) {
                var devMap = Util.getNextHopList(true);
                var intRegex = /^\d+$/;
                if ( intRegex.test( value ) ) {
                    return devMap[value]?devMap[value]:i18n._("Local interface");
                } else {
                    return value;
                }
            }, this)
        }],
        editorFields: [
            Field.description,
            Field.network,
            Field.netMask, {
                xtype: 'combo',
                fieldLabel: 'Next Hop'.t(),
                bind: '{record.nextHop}',
                store: Util.getNextHopList(false),
                queryMode: 'local',
                allowBlank: false,
                editable: true
            }, {
                xtype: 'component',
                margin: '10 0 0 20',
                html: 'If <b>Next Hop</b> is an IP address that network will be routed via the specified IP address.'.t() + '<br/>' +
                    'If <b>Next Hop</b> is an interface that network will be routed <b>locally</b> on that interface.'.t()
            }
        ]
    }, {
        xtype: 'panel',
        itemId: 'currentRoutes',
        title: 'Current Routes'.t(),
        region: 'south',
        height: '50%',
        split: true,
        tbar: [{
            xtype: 'tbtext',
            padding: '8 5',
            // style: { fontSize: '12px' },
            text: 'Current Routes shows the current routing system\'s configuration and how all traffic will be routed.'.t()
        }, '->', {
            text: 'Refresh Routes'.t(),
            iconCls: 'fa fa-refresh',
            handler: 'refreshRoutes'
        }],
        layout: 'fit',
        items: [{
            xtype: 'textarea',
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
        }],
        plugins: 'responsive',
        responsiveConfig: {
            wide: {
                region: 'east',
                width: '50%'
            },
            tall: {
                region: 'south',
                height: '50%',
            }
        }
    }]
});

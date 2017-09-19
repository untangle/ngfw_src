Ext.define('Ung.widget.NetworkLayout', {
    extend: 'Ext.container.Container',
    alias: 'widget.networklayoutwidget',

    /* requires-start */
    requires: [
        'Ung.widget.InterfaceItem'
    ],
    /* requires-end */

    controller: 'widget',

    hidden: true,
    border: false,
    baseCls: 'widget',

    visible: false,

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    bind: {
        hidden: '{!widget.enabled}'
    },

    refreshIntervalSec: 0,

    items: [{
        xtype: 'component',
        cls: 'header',
        height: 40,
        itemId: 'header',
        html: '<h1>' + 'Network Layout'.t() + '</h1>' +
            '<div class="actions"><a class="action-btn"><i class="fa fa-rotate-left fa-lg" data-action="refresh"></i></a></div>'
    }, {
        //xtype: 'container',
        cls: 'net-layout',
        margin: '0 10 10 10',
        layout: {
            type: 'vbox',
            align: 'stretch'
            //pack: 'middle'
        },
        border: false,
        defaults: {
            xtype: 'component'
        },
        items: [{
            html: '<img src="' + '/skins/default/images/admin/icons/interface-cloud.png" style="margin: 0 auto; display: block; height: 30px;"/>'
        }, {
            xtype: 'container',
            cls: 'ifaces',
            height: 75,
            itemId: 'externalInterface'
        }, {
            xtype: 'component',
            cls: 'line'
        }, {
            xtype: 'container',
            cls: 'ifaces',
            height: 120,
            itemId: 'internalInterface'
        }]
    }],

    fetchData: function (cb) {
        var me = this;

        Rpc.asyncData('rpc.networkManager.getNetworkSettings')
            .then(function(result) {
                cb();
                me.down('#externalInterface').removeAll();
                me.down('#internalInterface').removeAll();
                Ext.each(result.interfaces.list, function (iface) {
                    if (iface.configType !== 'DISABLED') {
                        if (iface.isWan) {
                            me.down('#externalInterface').add({
                                xtype: 'interfaceitem',
                                cls: 'iface wan',
                                viewModel: {
                                    data: {
                                        iface: iface,
                                        devicesCount: 0
                                    }
                                }
                            });
                        } else {
                            me.down('#internalInterface').add({
                                xtype: 'interfaceitem',
                                itemId: 'intf_' + iface.interfaceId,
                                cls: 'iface',
                                viewModel: {
                                    data: {
                                        iface: iface,
                                        devicesCount: 0
                                    }
                                }
                            });
                        }
                    }
                });
                me.getController().onStatsUpdate(); // force fetching devices count
            });
    }
});

Ext.define('Ung.widget.NetworkLayout', {
    extend: 'Ext.container.Container',
    alias: 'widget.networklayoutwidget',

    requires: [
        'Ung.widget.InterfaceItem'
    ],

    controller: 'widget',

    hidden: true,
    border: false,
    baseCls: 'widget adding',

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
        itemId: 'header',
        html: '<h1>' + 'Network Layout'.t() + '</h1>' +
            '<button class="action-btn"><i class="fa fa-refresh" data-action="refresh"></i></button>'
    }, {
        //xtype: 'container',
        cls: 'net-layout',
        margin: 10,
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
            html: '<img src="' + resourcesBaseHref + '/skins/default/images/admin/icons/interface-cloud.png" style="margin: 0 auto; display: block; height: 30px;"/>'
        }, {
            xtype: 'container',
            cls: 'ifaces',
            height: 69,
            itemId: 'externalInterface'
        }, {
            xtype: 'component',
            cls: 'line'
        }, {
            xtype: 'container',
            cls: 'ifaces',
            height: 80,
            itemId: 'internalInterface'
        }, {
            xtype: 'component',
            cls: 'devices',
            margin: '5 0 0 0',
            height: 40,
            bind: {
                html: '<img src="' + resourcesBaseHref + '/skins/default/images/admin/icons/interface-devices.png"><br/>{deviceCount}'
            }
        }]
    }],

    fetchData: function () {
        var me = this;
        Rpc.asyncData('rpc.networkManager.getNetworkSettings')
            .then(function(result) {
                me.fireEvent('afterdata');
                me.down('#externalInterface').removeAll();
                me.down('#internalInterface').removeAll();
                Ext.each(result.interfaces.list, function (iface) {
                    if (!iface.disabled) {
                        if (iface.isWan) {
                            me.down('#externalInterface').add({
                                xtype: 'interfaceitem',
                                cls: 'iface wan',
                                viewModel: {
                                    data: {
                                        iface: iface
                                    }
                                }
                            });
                        } else {
                            me.down('#internalInterface').add({
                                xtype: 'interfaceitem',
                                cls: 'iface',
                                viewModel: {
                                    data: {
                                        iface: iface
                                    }
                                }
                            });
                        }
                    }
                });
            });
    }

    // fetchData: function () {
    //     var me = this;
    //     rpc.networkManager.getNetworkSettings(function (result, exception) {
    //         me.fireEvent('afterdata');
    //         //handler.call(this);

    //         // Ext.each(result.interfaces.list, function (iface) {
    //         //     if (!iface.disabled) {
    //         //         if (iface.isWan) {
    //         //             me.data.externalInterfaces.push({
    //         //                 id: iface.interfaceId,
    //         //                 name: iface.name,
    //         //                 rx: 0,
    //         //                 tx: 0
    //         //             });
    //         //         } else {
    //         //             me.data.internalInterfaces.push({
    //         //                 id: iface.interfaceId,
    //         //                 name: iface.name,
    //         //                 rx: 0,
    //         //                 tx: 0
    //         //             });
    //         //         }
    //         //     }
    //         // });
    //         // this.interfacesLoaded = true;
    //         // this.update(me.data);
    //     });
    // }
});

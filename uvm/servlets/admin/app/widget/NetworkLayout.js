Ext.define('Ung.widget.NetworkLayout', {
    extend: 'Ext.container.Container',
    alias: 'widget.networklayoutwidget',

    controller: 'widget',

    hidden: true,
    border: false,
    baseCls: 'widget',

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    bind: {
        hidden: '{!widget.enabled}'
    },

    refreshIntervalSec: 5,

    items: [{
        xtype: 'container',
        layout: {
            type: 'hbox',
            align: 'top'
        },
        cls: 'header',
        style: {
            height: '50px'
        },
        items: [{
            xtype: 'component',
            flex: 1,
            html: '<h1>' + 'Network Layout'.t() + '</h1>'
        }, {
            xtype: 'container',
            margin: '10 5 0 0',
            layout: {
                type: 'hbox',
                align: 'middle'
            },
            items: [{
                xtype: 'button',
                baseCls: 'action',
                text: '<i class="material-icons">refresh</i>',
                listeners: {
                    click: 'fetchData'
                }
            }, {
                xtype: 'button',
                baseCls: 'action',
                text: '<i class="material-icons">call_made</i>',
                //bind: {
                //    href: '#reports/{widget.entryId}'
                //},
                hrefTarget: '_self'
            }]
        }]
    }, {
        xtype: 'container',
        html: 'Under construction'
    }],

    fetchData: function () {
        var me = this;
        rpc.networkManager.getNetworkSettings(function (result, exception) {
            me.fireEvent('afterdata');
            //handler.call(this);

            // Ext.each(result.interfaces.list, function (iface) {
            //     if (!iface.disabled) {
            //         if (iface.isWan) {
            //             me.data.externalInterfaces.push({
            //                 id: iface.interfaceId,
            //                 name: iface.name,
            //                 rx: 0,
            //                 tx: 0
            //             });
            //         } else {
            //             me.data.internalInterfaces.push({
            //                 id: iface.interfaceId,
            //                 name: iface.name,
            //                 rx: 0,
            //                 tx: 0
            //             });
            //         }
            //     }
            // });
            // this.interfacesLoaded = true;
            // this.update(me.data);
        });
    }
});
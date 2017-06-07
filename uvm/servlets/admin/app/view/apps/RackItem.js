Ext.define('Ung.view.apps.RackItem', {
    extend: 'Ext.container.Container',
    alias: 'widget.rackitem',

    baseCls: 'rackitem',


    width: 785,
    height: 93,
    padding: '8 50',

    layout: { type: 'column' },

    // disabled: true,
    hidden: true,
    bind: {
        hidden: '{!instanceId && !installing}',
        userCls: '{(parentPolicy || installing) ? "dsbl" : ""}'
    },

    items: [{
        xtype: 'container',
        width: 290,
        // flex: 1,
        layout: { type: 'vbox', align: 'stretch' },
        items: [{
            xtype: 'container',
            layout: { type: 'hbox' },
            cls: 'ttl',
            items: [{
                xtype: 'component',
                bind: {
                    html: '<img src="/skins/modern-rack/images/admin/apps/{app.name}_80x80.png" width=42 height=42/>'
                }
            }, {
                xtype: 'container',
                layout: { type: 'vbox' },
                items: [{
                    xtype: 'component',
                    cls: 'name',
                    bind: {
                        html: '<p>{app.displayName}</p>'
                    }
                }, {
                    xtype: 'component',
                    cls: 'policy',
                    hidden: true,
                    bind: {
                        hidden: '{!parentPolicy}',
                        html: '<p><i class="fa fa-file-text-o"></i> {parentPolicy}</p>'
                    }
                }, {
                    xtype: 'component',
                    cls: 'license',
                    hidden: true,
                    bind: {
                        hidden: '{!licenseMessage}',
                        html: '<p><i class="fa fa-exclamation-triangle fa-orange"></i> {licenseMessage}</p>'
                    }
                }]
            }]
            // bind: {
            //     html: '<img src="/skins/modern-rack/images/admin/apps/{app.name}_80x80.png" width=42 height=42 style="float: left;"/>' +
            //         '<p>{app.displayName}' +
            //         '<br/><span style="font-size: 12px;"><i class="fa fa-file-text-o"></i> {app.parentPolicy}</span></p>'
            // }
        }, {
            xtype: 'container',
            margin: '5 0',
            layout: { type: 'hbox' },
            items: [{
                xtype: 'button',
                iconCls: 'fa fa-wrench',
                text: 'Settings'.t(),
                hrefTarget: '_self',
                hidden: true,
                disabled: true,
                bind: {
                    href: '{route}',
                    hidden: '{parentPolicy}',
                    disabled: '{installing}'
                }
            }, {
                xtype: 'button',
                margin: '0 0 0 5',
                iconCls: 'fa fa-question-circle',
                hidden: true,
                disabled: true,
                bind: {
                    href: '{helpSource}',
                    hidden: '{parentPolicy}',
                    disabled: '{installing}'
                }
                // text: 'Help'.t()
            }, {
                xtype: 'button',
                margin: '0 0 0 5',
                html: 'Buy Now'.t(),
                iconCls: 'fa fa-shopping-cart',
                hidden: true,
                disabled: true,
                bind: {
                    href: Util.getStoreUrl() + '?action=buy&libitem=untangle-libitem-{app.name}&' + Util.getAbout(),
                    hidden: '{!license || !license.trial || parentPolicy || installing }',
                    disabled: '{installing}'
                }
            }]
        }]
    }, {
        xtype: 'container',
        layout: { type: 'hbox', align: 'stretch' },
        controller: 'rackgraph',
        width: 350,
        height: 75,
        margin: '0 10 0 0',
        // hidden: true,
        // hideMode: 'visibility',
        // bind: {
        //     hidden: '{!metrics || metrics.length === 0}'
        // },
        items: [{
            xtype: 'component',
            itemId: 'rackgraph',
            cls: 'graph',
            width: 125,
            height: 75,
            bind: {
                userCls: '{targetState}',
            }
        }, {
            xtype: 'container',
            itemId: 'metrics',
            flex: 1,
            margin: '0 0 0 10',
            cls: 'metrics',
            layout: { type: 'vbox', align: 'stretch' },
            defaults: {
                xtype: 'component'
            },
            bind: {
                userCls: '{targetState}'
            }
        }]
    }, {
        xtype: 'button',
        width: 30,
        height: 30,
        baseCls: 'power',
        hideMode: 'visibility',
        disabled: true,
        hidden: true,
        bind: {
            userCls: '{targetState}',
            disabled: '{parentPolicy}',
            hidden: '{!app.hasPowerButton }'
        },
        renderTpl: '<i class="fa fa-power-off"></i>',
        handler: 'powerHandler'
    }]

    // initComponent: function () {
    //     me = this;

    // }


});

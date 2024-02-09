Ext.define('Ung.view.apps.RackItem', {
    extend: 'Ext.container.Container',
    alias: 'widget.rackitem',

    baseCls: 'rackitem',

    width: 785,
    height: 93,
    padding: '8 50',

    layout: { type: 'column' },

    disabled: true,
    hidden: true,
    bind: {
        hidden: '{!instanceId && !installing}',
        disabled: '{parentPolicy || installing}',
        userCls: '{parentPolicy ? "from-parent" : (installing ? "installing" : "")}',
    },

    items: [{
        xtype: 'container',
        width: 290,
        layout: { type: 'vbox', align: 'stretch' },
        items: [{
            xtype: 'container',
            layout: { type: 'hbox' },
            cls: 'ttl',
            items: [{
                xtype: 'component',
                bind: {
                    html: '<img src="/icons/apps/{app.name}.svg" width=42 height=42/>'
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
                        html: '<p><i class="fa fa-file-text-o"></i>&nbsp; {parentPolicy}</p>'
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
                bind: {
                    href: '{route}',
                    hidden: '{parentPolicy || installing}'
                }
            }, {
                xtype: 'button',
                margin: '0 0 0 5',
                iconCls: 'fa fa-question-circle',
                hidden: true,
                bind: {
                    href: '{helpSource}',
                    hidden: '{parentPolicy || installing}',
                }
            }, {
                xtype: 'button',
                margin: '0 0 0 5',
                html: 'Buy Now'.t(),
                iconCls: 'fa fa-shopping-cart',
                hidden: true,
                bind: {
                    href: Util.getStoreUrl() + '?action=buy&libitem=untangle-libitem-{app.name}&' + Util.getAbout(),
                    hidden: '{!license || !license.trial || parentPolicy || installing }',
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
        items: [{
            xtype: 'component',
            itemId: 'rackgraph',
            cls: 'graph',
            width: 125,
            height: 75,
            bind: {
                userCls: '{state.on ? "on" : "off"}',
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
                userCls: '{state.on ? "on" : "off"}',
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
            userCls: '{powerCls}',
            disabled: '{parentPolicy}',
            hidden: '{!app.hasPowerButton }'
        },
        renderTpl: '<span id="{id}-btnWrap" data-ref="btnWrap" role="presentation" unselectable="on" style="{btnWrapStyle}" ' + 'class="{btnWrapCls} {btnWrapCls}-{ui} {splitCls}{childElCls}">' +
            '<span id="{id}-btnEl" data-ref="btnEl" role="presentation" unselectable="on" style="{btnElStyle}" ' + 'class="{btnCls} {btnCls}-{ui} {textCls} {noTextCls} {hasIconCls} ' + '{iconAlignCls} {textAlignCls} {btnElAutoHeightCls}{childElCls}">' +
            '<i class="fa fa-power-off"></i>' +
            '</span>' + '</span>',
        handler: 'powerHandler'
    }, {
        xtype: 'component',
        cls: 'loader',
        autoEl: { tag: 'span' },
        hidden: true,
        bind: {
            hidden: '{!installing}'
        }
    }]
});

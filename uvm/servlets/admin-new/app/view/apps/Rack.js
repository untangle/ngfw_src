Ext.define('Ung.view.apps.Rack', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.apps-rack',

    scrollable: 'y',

    border: false,
    bodyBorder: false,

    userCls: 'rack',

    defaults: {
        border: false,
        bodyBorder: false,
        layout: {
            type: 'vbox',
            align: 'middle'
        }
    },

    items: [{
        xtype: 'container',
        items: [{
            xtype: 'component',
            cls: 'apps-separator',
            html: 'Apps'.t()
        }]
    }, {
        xtype: 'container',
        itemId: '_apps'
    }, {
        xtype: 'container',
        items: [{
            xtype: 'component',
            cls: 'apps-separator',
            html: 'Service Apps'.t()
        }]
    }, {
        xtype: 'container',
        itemId: '_services'
    }]
});

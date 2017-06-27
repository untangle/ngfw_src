Ext.define('Ung.view.apps.Simple', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.apps-simple',

    scrollable: 'y',

    border: false,
    bodyBorder: false,

    defaults: {
        border: false,
        bodyBorder: false
    },

    items: [{
        xtype: 'container',
        items: [{
            xtype: 'component',
            cls: 'apps-title',
            html: 'Apps'.t()
        }]
    }, {
        xtype: 'container',
        layout: {
            type: 'column'
        },
        itemId: '_apps'
    }, {
        xtype: 'container',
        items: [{
            xtype: 'component',
            cls: 'apps-title',
            html: 'Service Apps'.t()
        }]
    }, {
        xtype: 'container',
        layout: {
            type: 'column'
        },
        itemId: '_services'
    }]
});

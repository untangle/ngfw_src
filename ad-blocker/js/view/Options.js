Ext.define('Ung.apps.ad-blocker.view.Options', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ad-blocker-options',
    itemId: 'options',
    title: 'Options'.t(),
    scrollable: true,

    bodyPadding: 10,

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    defaults: {
        xtype: 'fieldset',
        cls: 'app-section',
        padding: 10
    },

    items: [{
        title: 'Block'.t(),
        items: [{
            xtype: 'checkbox',
            boxLabel: 'Block Ads'.t(),
            bind: '{settings.scanAds}'
        }, {
            xtype: 'checkbox',
            boxLabel: 'Block Tracking & Ad Cookies',
            bind: '{settings.scanCookies}'
        }]
    }, {
        title: 'Update filters'.t(),
        items: [{
            xtype: 'button',
            text: 'Update'.t(),
            iconCls: 'fa fa-refresh',
            handler: 'updateFilters'
        }, {
            xtype: 'component',
            margin: '10 0 0 0',
            bind: {
                html: Ext.String.format('The current filter list was last modified: {0}. You are free to disable filters and add new ones, however it is not required.'.t(),
                '{lastUpdate}')
            }
        }]
    }]
});

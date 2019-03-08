Ext.define('Ung.view.main.Main', {
    extend: 'Ext.panel.Panel',
    itemId: 'main',
    reference: 'main',

    /* requires-start */
    requires: [
        'Ung.view.main.MainController',
        'Ung.view.main.MainModel',
        'Ung.view.dashboard.Dashboard',
        'Ung.view.apps.Apps',
        'Ung.view.config.Config',
        'Ung.view.reports.Reports',
    ],
    /* requires-end */

    controller: 'main',
    viewModel: {},

    layout: 'card',
    border: false,
    bodyBorder: false,
    bind: {
        activeItem: '{activeItem}'
    },
    publishes: 'activeItem',

    dockedItems: [{
        xtype: 'mainheading'
    }],

    items: [{
        xtype: 'ung-dashboard'
    }, {
        xtype: 'ung.apps'
    }, {
        xtype: 'ung.config'
    }, {
        xtype: 'ung.reports'
    }, {
        xtype: 'invalidroute',
    }]
});

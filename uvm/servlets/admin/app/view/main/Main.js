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
        xtype: 'container',
        html: 'Loading ....'
    }, {
        xtype: 'ung-dashboard'
    }, {
        xtype: 'ung.apps'
    }, {
        xtype: 'ung.config'
    }, {
        xtype: 'ung.reports'
    }, {
        xtype: 'container',
        layout: {
            type: 'vbox',
            align: 'center'
        },
        itemId: 'invalidRoute',
        items: [{
            xtype: 'component',
            style: {
                textAlign: 'center'
            },
            html: '<i class="fa fa-warning fa-5x fa-gray"></i> <h1>Invalid route!</h1>'
        }, {
            xtype: 'button',
            iconCls: 'fa fa-arrow-left',
            scale: 'medium',
            focusable: false,
            text: 'Click to go go back'.t(),
            handler: function() {
                Ext.util.History.back();
            }
        }]
    }]
});

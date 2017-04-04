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
    viewModel: {
        formulas: {
            reportsEnabled: function (get) {
                return (get('reportsInstalled') && get('reportsRunning'));
            }
        }
    },

    layout: 'card',
    border: false,
    bodyBorder: false,
    bind: {
        activeItem: '{activeItem}'
    },
    publishes: 'activeItem',

    items: [{
        xtype: 'ung-dashboard'
    }, {
        xtype: 'ung.apps'
    }, {
        xtype: 'ung.config'
    }, {
        xtype: 'ung.reports'
    }],

    dockedItems: [{
        xtype: 'toolbar',
        ui: 'navigation',
        dock: 'top',
        border: false,
        defaults: {
            xtype: 'button',
            border: false,
            iconAlign: 'top',
            hrefTarget: '_self'
        },
        items: [{
            html: '<img src="' + '/images/BrandingLogo.png" style="height: 40px;"/>',
            cls: 'logo',
            href: '#'
        }, {
            text: 'Dashboard'.t(),
            iconCls: 'fa fa-home',
            cls: 'upper',
            href: '#',
            bind: { userCls: '{activeItem === "dashboard" ? "pressed" : ""}' }
        }, {
            text: 'Apps'.t(),
            iconCls: 'fa fa-th',
            cls: 'upper',
            bind: { href: '#apps', userCls: '{(activeItem === "apps" || activeItem === "appCard") ? "pressed" : ""}' }
        }, {
            text: 'Config'.t(),
            iconCls: 'fa fa-sliders',
            cls: 'upper',
            href: '#config',
            bind: { userCls: '{activeItem === "config" ? "pressed" : ""}' }
        }, {
            text: 'Reports'.t(),
            iconCls: 'fa fa-line-chart',
            cls: 'upper',
            href: '#reports',
            hidden: true,
            bind: {
                userCls: '{activeItem === "reports" ? "pressed" : ""}',
                hidden: '{!reportsInstalled}'
            }
        }, '->', {
            text: 'Help'.t(),
            iconCls: 'fa fa-question-circle'
        }, {
            text: 'Account'.t(),
            iconCls: 'fa fa-user-circle'
        }]
    }]
});

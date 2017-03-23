Ext.define('Ung.view.main.Main', {
    extend: 'Ext.panel.Panel',
    itemId: 'main',
    //xtype: 'ung-main',

    // plugins: [
    //     'viewport'
    // ],
    /* requires-start */
    requires: [
        // 'Ext.plugin.Viewport',
        'Ung.view.main.MainController',
        'Ung.view.main.MainModel',
        'Ung.view.dashboard.Dashboard',
        'Ung.view.apps.Apps',
        'Ung.view.config.Config',
        'Ung.view.reports.Reports',

        // 'Ung.view.shd.Sessions',
        // 'Ung.view.shd.Hosts',
        // 'Ung.view.shd.Devices'
    ],
    /* requires-end */

    controller: 'main',
    // viewModel: true,
    viewModel: {
        type: 'main'
    },

    layout: 'card',
    border: false,

    bind: {
        activeItem: '{activeItem}'
    },

    items: [{
        xtype: 'ung-dashboard'
    }, {
        xtype: 'ung.apps'
    }, {
        xtype: 'ung.config'
    }, {
        xtype: 'ung.reports'
    }, {
        // xtype: 'ung.hosts',
        // itemId: 'hosts'
    }, {
        // xtype: 'ung.devices',
        // itemId: 'devices'
    }],

    dockedItems: [{
        xtype: 'toolbar',
        ui: 'navigation',
        dock: 'top',
        border: false,
        defaults: {
            xtype: 'button',
            border: false,
            enableToggle: true,
            iconAlign: 'top',
            // border: '0 0 5 0',
            hrefTarget: '_self'
        },
        items: [
            { html: '<img src="' + '/images/BrandingLogo.png" style="height: 40px;"/>', cls: 'logo' },
            { text: 'Dashboard'.t(), iconCls: 'fa fa-home', cls: 'upper', href: '#', bind: { pressed: '{selectedNavItem === "dashboard"}' } },
            { text: 'Apps'.t(), iconCls: 'fa fa-th', cls: 'upper', bind: { href: '#apps/{policyId}', pressed: '{selectedNavItem === "apps"}' } },
            { text: 'Config'.t(), iconCls: 'fa fa-sliders', cls: 'upper', href: '#config', bind: { pressed: '{selectedNavItem === "config"}' } },
            { text: 'Reports'.t(), iconCls: 'fa fa-line-chart', cls: 'upper', href: '#reports', bind: { pressed: '{selectedNavItem === "reports"}' } },
            '->',
            { text: 'Sessions'.t(), iconCls: 'fa fa-list', href: '#sessions', bind: { pressed: '{selectedNavItem === "sessions"}' } },
            { text: 'Hosts'.t(), iconCls: 'fa fa-th-list', href: '#hosts', bind: { pressed: '{selectedNavItem === "hosts"}' } },
            { text: 'Devices'.t(), iconCls: 'fa fa-desktop', href: '#devices', bind: { pressed: '{selectedNavItem === "devices"}' } },
            '-',
            { text: 'Help'.t(), iconCls: 'fa fa-question-circle' },
            { text: 'Account'.t(), iconCls: 'fa fa-user-circle' }
        ]
    }]
});

Ext.define('Ung.view.main.Main', {
    extend: 'Ext.panel.Panel',
    itemId: 'main',
    reference: 'main',
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
            // enableToggle: true,
            iconAlign: 'top',
            // border: '0 0 5 0',
            hrefTarget: '_self',
            listeners: {
                // beforetoggle: function () {
                //     console.log('toggle');
                //     return false;
                // }
            }
        },
        items: [
            { html: '<img src="' + '/images/BrandingLogo.png" style="height: 40px;"/>', cls: 'logo', href: '#' },
            { text: 'Dashboard'.t(), iconCls: 'fa fa-home', cls: 'upper', href: '#', bind: { userCls: '{activeItem === "dashboard" ? "pressed" : ""}' } },
            { text: 'Apps'.t(), iconCls: 'fa fa-th', cls: 'upper', bind: { href: '#apps/{policyId}', userCls: '{(activeItem === "apps" || activeItem === "appCard") ? "pressed" : ""}' } },
            { text: 'Config'.t(), iconCls: 'fa fa-sliders', cls: 'upper', href: '#config', bind: { userCls: '{activeItem === "config" ? "pressed" : ""}' } },
            { text: 'Reports'.t(), iconCls: 'fa fa-line-chart', cls: 'upper', href: '#reports', bind: { userCls: '{activeItem === "reports" ? "pressed" : ""}' } },
            '->',
            { text: 'Help'.t(), iconCls: 'fa fa-question-circle' },
            { text: 'Account'.t(), iconCls: 'fa fa-user-circle' }
        ]
    }
    //     {
    //     xtype: 'toolbar',
    //     ui: 'navigation',
    //     dock: 'top',
    //     style: {
    //         background: '#494B50',
    //         zIndex: 9997
    //     },
    //     defaults: {
    //         xtype: 'button',
    //         border: false,
    //         hrefTarget: '_self'
    //     },
    //     layout: { type: 'hbox', align: 'middle' },
    //     items: [
    //         {
    //             text: 'Manage Widgets'.t(),
    //             handler: 'toggleWidgetManager',
    //             iconCls: 'fa fa-cog',
    //             hidden: true,
    //             bind: {
    //                 hidden: '{activeItem !== "dashboard"}',
    //                 iconCls: 'fa {dashboardManagerOpen ? "fa-arrow-down" : "fa-arrow-left"}',
    //                 userCls: '{dashboardManagerOpen ? "pressed" : ""}'
    //             }
    //         }, {
    //             xtype: 'button',
    //             text: 'Policy 1',
    //             iconCls: 'fa fa-file-text-o',
    //             menu: {
    //                 plain: true,
    //                 items: [{
    //                     text: 'Policy 1'
    //                 }]
    //             },
    //             hidden: true,
    //             bind: {
    //                 hidden: '{activeItem !== "apps"}'
    //             }
    //             // xtype: 'combobox',
    //             // editable: false,
    //             // queryMode: 'local',
    //             // hidden: true,
    //             // bind: {
    //             //     value: '{policyId}',
    //             //     store: '{policies}',
    //             //     hidden: '{activeItem !== "apps"}'
    //             // },
    //             // valueField: 'policyId',
    //             // displayField: 'displayName'
    //             // // listeners: {
    //             // //     change: 'setPolicy'
    //             // // }
    //         }, {
    //             xtype: 'button',
    //             html: 'Install Apps'.t(),
    //             iconCls: 'fa fa-download',
    //             hrefTarget: '_self',
    //             hidden: true,
    //             bind: {
    //                 href: '#apps/{policyId}/install',
    //                 hidden: '{activeItem !== "apps"}'
    //             }
    //         }, {
    //             xtype: 'button',
    //             html: 'Back to Apps'.t(),
    //             iconCls: 'fa fa-arrow-circle-left',
    //             hrefTarget: '_self',
    //             hidden: true,
    //             bind: {
    //                 href: '#apps/{policyId}',
    //                 hidden: '{activeItem !== "appCard"}'
    //             }
    //         },
    //         '->',
    //         { text: 'Sessions'.t(), iconCls: 'fa fa-list', href: '#sessions', bind: { userCls: '{activeItem === "sessions" ? "pressed" : ""}' } },
    //         { text: 'Hosts'.t(), iconCls: 'fa fa-th-list', href: '#hosts', bind: { userCls: '{activeItem === "hosts" ? "pressed" : ""}' } },
    //         { text: 'Devices'.t(), iconCls: 'fa fa-desktop', href: '#devices', bind: { userCls: '{activeItem === "devices" ? "pressed" : ""}' } },
    //         { text: 'Users'.t(), iconCls: 'fa fa-users', href: '#', bind: { userCls: '{activeItem === "sessions" ? "users" : ""}' } }
    //     ]
    // }
    ]
});

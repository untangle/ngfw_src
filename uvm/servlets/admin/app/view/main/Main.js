Ext.define('Ung.view.main.Main', {
    extend: 'Ext.container.Viewport',
    //xtype: 'ung-main',

    plugins: [
        'viewport'
    ],

    requires: [
        'Ext.plugin.Viewport',
        'Ung.view.main.MainController',
        'Ung.view.main.MainModel',
        'Ung.view.dashboard.Dashboard',
        'Ung.view.apps.Apps',
        'Ung.view.apps.install.Install',
        'Ung.view.config.Config',
        'Ung.view.reports.Reports',
        'Ung.view.node.Settings'
    ],


    controller: 'main',
    viewModel: {
        type: 'main'
    },

    layout: 'border',
    border: false,

    items: [{
        region: 'north',
        layout: { type: 'hbox', align: 'middle' },
        border: false,
        height: 66,
        ui: 'navigation',
        items: [{
            xtype: 'container',
            layout: { type: 'hbox', align: 'middle' },
            defaults: {
                xtype: 'button',
                baseCls: 'nav-item',
                height: 30,
                hrefTarget: '_self'
            },
            items: [{
                html: '<img src="/images/BrandingLogo.png" style="height: 40px;"/>',
                width: 100,
                height: 40,
                href: '#'
            }, {
                html: Ung.Util.iconTitle('Dashboard'.t(), 'home-16'),
                href: '#',
                bind: {
                    pressed: '{isDashboard}'
                }
            }, {
                html: Ung.Util.iconTitle('Apps'.t(), 'apps-16'),
                bind: {
                    href: '#apps/{policyId}',
                    pressed: '{isApps}'
                }
            }, {
                html: Ung.Util.iconTitle('Config'.t(), 'tune-16'),
                href: '#config',
                bind: {
                    pressed: '{isConfig}'
                }
            }, {
                html: Ung.Util.iconTitle('Reports'.t(), 'show_chart-16'),
                href: '#reports',
                bind: {
                    //html: '{reportsEnabled}',
                    hidden: '{!reportsEnabled}',
                    pressed: '{isReports}'
                }
            }]
        }]
    }, {
        xtype: 'container',
        region: 'center',
        layout: 'card',
        itemId: 'main',
        border: false,
        bind: {
            activeItem: '{activeItem}'
        },
        items: [{
            xtype: 'ung.dashboard',
            itemId: 'dashboard'
        }, {
            xtype: 'ung.apps',
            itemId: 'apps'
        }, {
            xtype: 'ung.config',
            itemId: 'config'
        }, {
            xtype: 'ung.appsinstall',
            itemId: 'appsinstall'
        }, {
            xtype: 'ung.nodesettings',
            itemId: 'settings'
        }]
    }]
});

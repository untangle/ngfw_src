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
        'Ung.view.node.Settings',

        'Ung.view.shd.Sessions',
        'Ung.view.shd.Hosts',
        'Ung.view.shd.Devices'
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
                enableToggle: true,
                baseCls: 'nav-item',
                height: 30,
                hrefTarget: '_self'
            },
            items: [{
                enableToggle: false,
                html: '<img src="' + resourcesBaseHref + '/images/BrandingLogo.png" style="height: 40px;"/>',
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
        },
        {
            xtype: 'ung.apps',
            itemId: 'apps'
        }, {
            xtype: 'ung.config',
            itemId: 'config'
        }, {
            xtype: 'ung.configsettings',
            itemId: 'configsettings'
        }, {
            xtype: 'ung.appsinstall',
            itemId: 'appsinstall'
        }, {
            xtype: 'ung.nodesettings',
            itemId: 'settings'
        },
        {
            layout: 'border',
            itemId: 'shd', // sessions hosts devices
            border: false,
            items: [{
                region: 'north',
                weight: 20,
                border: false,
                height: 44,
                bodyStyle: {
                    background: '#555',
                    padding: '0 5px'
                },
                layout: {
                    type: 'hbox',
                    align: 'middle'
                },
                defaults: {
                    xtype: 'button',
                    enableToggle: true,
                    baseCls: 'heading-btn',
                    hrefTarget: '_self'
                },
                items: [{
                    html: Ung.Util.iconTitle('Back to Dashboard', 'keyboard_arrow_left-16'),
                    enableToggle: false,
                    href: '#',
                    hrefTarget: '_self'
                }, {
                    xtype: 'component',
                    flex: 1
                }, {
                    html: 'Sessions'.t(),
                    href: '#sessions',
                    bind: {
                        pressed: '{isSessions}'
                    }
                }, {
                    html: 'Hosts'.t(),
                    href: '#hosts',
                    bind: {
                        pressed: '{isHosts}'
                    }
                }, {
                    html: 'Devices'.t(),
                    href: '#devices',
                    bind: {
                        pressed: '{isDevices}'
                    }
                }]
            }, {
                region: 'center',
                layout: 'card',
                itemId: 'shdcenter',
                items: [{
                    xtype: 'ung.sessions',
                    itemId: 'sessions'
                }, {
                    xtype: 'ung.hosts',
                    itemId: 'hosts'
                }, {
                    xtype: 'ung.devices',
                    itemId: 'devices'
                }]
            }]
        }
        ]
    }]
});

Ext.define('Ung.view.apps.Apps', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.apps',
    itemId: 'apps',
    layout: 'card',
    /* requires-start */
    requires: [
        'Ung.view.apps.AppsController'
    ],
    /* requires-end */
    controller: 'apps',
    viewModel: {
        data: {
            onInstalledApps: true,
            policyName: '',
            appsCount: 0,
            servicesCount: 0
        },
        stores: {
            apps: {
                // data: '{appsData}',
                fields: ['name', 'displayName', 'url', 'type', 'status'],
                listeners: {
                    datachanged: 'updateCounters'
                }
            },
            installedApps: {
                source: '{apps}',
                filters: [ { property: 'type', value: 'FILTER' }, function (item) { return Ext.Array.contains(['installed', 'progress', 'finish'], item.get('extraCls')); } ],
                sorters: [ { property: 'viewPosition', direction: 'ASC' }]
            },
            installableApps: {
                source: '{apps}',
                filters: [ { property: 'type', value: 'FILTER' }, function (item) { return Ext.Array.contains(['installable', 'progress', 'finish'], item.get('extraCls')); } ],
                sorters: [ { property: 'viewPosition', direction: 'ASC' }]
            },
            installedServices: {
                source: '{apps}',
                filters: [ { property: 'type', value: 'SERVICE' }, function (item) { return Ext.Array.contains(['installed', 'progress', 'finish'], item.get('extraCls')); } ],
                sorters: [ { property: 'viewPosition', direction: 'ASC' }]
            },
            installableServices: {
                source: '{apps}',
                filters: [ { property: 'type', value: 'SERVICE' }, function (item) { return Ext.Array.contains(['installable', 'progress', 'finish'], item.get('extraCls')); } ],
                sorters: [ { property: 'viewPosition', direction: 'ASC' }]
            }
        }
    },

    defaults: {
        border: false
    },

    dockedItems: [{
        xtype: 'toolbar',
        ui: 'navigation',
        dock: 'top',
        border: false,
        style: {
            background: '#333435',
            // zIndex: 9997
        },
        defaults: {
            border: false
        },
        items: Ext.Array.insert(Ext.clone(Util.subNav), 0, [{
            xtype: 'button',
            html: 'Back to Apps',
            iconCls: 'fa fa-arrow-circle-left',
            hrefTarget: '_self',
            hidden: true,
            bind: {
                hidden: '{onInstalledApps}'
            },
            handler: 'backToApps'
        }, {
            xtype: 'button',
            reference: 'policyBtn',
            hidden: true,
            cls: 'policy-menu',
            iconCls: 'fa fa-file-text-o',
            arrowVisible: false,
            bind: {
                text: '{policyName} &nbsp;<i class="fa fa-angle-down fa-lg"></i>',
                hidden: '{!onInstalledApps || !policyManagerInstalled}'
            }
        }, {
            xtype: 'button',
            html: 'Install Apps'.t(),
            iconCls: 'fa fa-download',
            hrefTarget: '_self',
            // hidden: true,
            handler: 'showInstall',
            bind: {
            //     href: '#apps/{policyId}/install',
                hidden: '{!onInstalledApps}'
            }
        }, {
            xtype: 'component',
            margin: '0 10',
            cls: 'install-header',
            reference: 'installHeader',
            html: '',
            hidden: true,
            bind: {
                html: '<i class="fa fa-angle-right fa-lg"></i> &nbsp;&nbsp;&nbsp;&nbsp; Install Apps in &nbsp;<i class="fa fa-file-text-o"></i> <strong>{policyName}</strong> policy',
                hidden: '{onInstalledApps || !policyManagerInstalled}'
            }
        }])
    }],


    items: [{
        scrollable: true,
        itemId: 'installedApps',
        width: '50%',
        layout: { type: 'vbox', align: 'stretch' },
        items: [{
            xtype: 'component',
            cls: 'apps-title',
            hidden: true,
            bind: {
                html: 'Apps'.t() + ' ({appsCount})',
                hidden: '{!policyName}'
            }
        }, {
            xtype: 'dataview',
            bind: '{installedApps}',
            tpl: '<tpl for=".">' +
                    '<tpl if="licenseExpired || parentPolicy"><a class="app-item disabled"><tpl elseif="route"><a href="{route}" class="app-item {extraCls}"><tpl else><a class="app-item {extraCls}"></tpl>' +
                    '<tpl if="hasPowerButton && runState && !licenseExpired"><span class="state {runState}"><i class="fa fa-power-off"></i></span></tpl>' +
                    '<tpl if="licenseMessage"><span class="license">{licenseMessage}</span></tpl>' +
                    '<img src="' + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
                    '<span class="app-name">{displayName}</span>' +
                    '<tpl if="parentPolicy"><span class="parent-policy">[{parentPolicy}]</span></tpl>' +
                    '<span class="new">NEW</span>' +
                    '<span class="loader"></span>' +
                    '</a>' +
                '</tpl>',
            itemSelector: 'a'
        }, {
            xtype: 'component',
            padding: '20 40',
            html: 'No Apps ...',
            // items: [{
            //     xtype: 'button',
            //     scale: 'large',
            //     text: 'Install Apps'.t(),
            //     padding: '0 20 0 0',
            //     iconCls: 'fa fa-download',
            //     handler: 'showInstall'
            // }],
            hidden: true,
            // bind: {
            //     hidden: '{appsCount > 0 || !policyName}'
            // }
        }, {
            xtype: 'component',
            cls: 'apps-title',
            hidden: true,
            bind: {
                html: 'Service Apps'.t() + ' ({servicesCount})',
                hidden: '{!policyName}'
            }
        }, {
            xtype: 'dataview',
            bind: '{installedServices}',
            tpl: '<tpl for=".">' +
                    '<tpl if="licenseExpired"><a class="app-item disabled"><tpl elseif="route"><a href="{route}" class="app-item {extraCls}"><tpl else><a class="app-item {extraCls}"></tpl>' +
                    '<tpl if="hasPowerButton && runState && !licenseExpired"><span class="state {runState}"><i class="fa fa-power-off"></i></span></tpl>' +
                    '<tpl if="licenseMessage"><span class="license">{licenseMessage}</span></tpl>' +
                    '<img src="' + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
                    '<span class="app-name">{displayName}</span>' +
                    '<span class="new">NEW</span>' +
                    '<span class="loader"></span>' +
                    '</a>' +
                '</tpl>',
            itemSelector: 'a'
        }, {
            xtype: 'component',
            padding: '20 40',
            html: 'No Service Apps ...',
            // items: [{
            //     xtype: 'button',
            //     scale: 'large',
            //     text: 'Install Apps'.t(),
            //     padding: '0 20 0 0',
            //     iconCls: 'fa fa-download',
            //     handler: 'showInstall'
            // }],
            hidden: true,
            // bind: {
            //     hidden: '{servicesCount > 0 || !policyName}'
            // }
        }]
    }, {
        scrollable: true,
        width: '50%',
        itemId: 'installableApps',
        layout: { type: 'vbox', align: 'stretch' },
        items: [{
            xtype: 'component',
            cls: 'apps-title',
            html: 'Apps'.t()
        }, {
            xtype: 'dataview',
            bind: '{installableApps}',
            tpl: '<tpl for=".">' +
                    '<div class="app-install-item {extraCls}">' +
                    '<img src="' + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
                    '<i class="fa fa-download fa-3x"></i>' +
                    '<i class="fa fa-check fa-3x"></i>' +
                    '<span class="loader">Loading...</span>' +
                    '<h3>{displayName}</h3>' + '<p>{desc}</p>' +
                    '</div>' +
                '</tpl>',
            itemSelector: 'div'
        }, {
            xtype: 'component',
            cls: 'apps-title',
            html: 'Service Apps'.t()
        }, {
            xtype: 'dataview',
            bind: '{installableServices}',
            tpl: '<tpl for=".">' +
                    '<div class="app-install-item {extraCls}">' +
                    '<img src="' + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
                    '<i class="fa fa-download fa-3x"></i>' +
                    '<i class="fa fa-check fa-3x"></i>' +
                    '<span class="loader">Loading...</span>' +
                    '<h3>{displayName}</h3>' + '<p>{desc}</p>' +
                    '</div>' +
                '</tpl>',
            itemSelector: 'div'
        }]
    }]
});

Ext.define('Ung.view.apps.Apps', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.apps',
    itemId: 'apps',
    layout: 'card',
    requires: [
        'Ung.view.apps.AppsController'
    ],

    controller: 'apps',
    viewModel: {
        data: {
            onInstalledApps: false
        },
        stores: {
            apps: {
                // data: '{appsData}',
                fields: ['name', 'displayName', 'type', 'status'],
                sorters: [{
                    property: 'viewPosition',
                    direction: 'ASC'
                }]
            }
        }
    },

    config: {
        policy: undefined
    },

    defaults: {
        border: false
    },

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        border: false,
        items: [{
            xtype: 'combobox',
            editable: false,
            multiSelect: false,
            queryMode: 'local',
            hidden: true,
            bind: {
                value: '{policyId}',
                store: '{policies}',
                hidden: '{!onInstalledApps}'
            },
            valueField: 'policyId',
            displayField: 'displayName',
            listeners: {
                change: 'setPolicy'
            }
        }, {
            xtype: 'button',
            html: 'Install Apps'.t(),
            iconCls: 'fa fa-download',
            hrefTarget: '_self',
            hidden: true,
            bind: {
                href: '#apps/{policyId}/install',
                hidden: '{!onInstalledApps}'
            }
        }, {
            xtype: 'button',
            html: 'Back to Apps'.t(),
            iconCls: 'fa fa-arrow-circle-left',
            hrefTarget: '_self',
            hidden: true,
            bind: {
                href: '#apps/{policyId}',
                hidden: '{onInstalledApps}'
            }
        }]
    }],

    items: [{
        xtype: 'dataview',
        scrollable: true,
        itemId: 'installedApps',
        bind: '{apps}',
        tpl: '<p class="apps-title">' + 'Apps'.t() + '</p>' +
            '<tpl for=".">' +
                '<tpl if="type === \'FILTER\'">' +
                '<a href="#config" class="app-item">' +
                '<span class="state {targetState}"><i class="fa fa-power-off"></i></span>' +
                '<img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
                '<span class="app-name">{displayName}</span>' +
                '</a>' +
                '</tpl>' +
            '</tpl>' +
            '<p class="apps-title">' + 'Service Apps'.t() + '</p>' +
            '<tpl for=".">' +
                '<tpl if="type === \'SERVICE\'">' +
                '<a href="#config" class="app-item">' +
                '<span class="state {targetState}"><i class="fa fa-power-off"></i></span>' +
                '<img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
                '<span class="app-name">{displayName}</span>' +
                '</a>' +
                '</tpl>' +
            '</tpl>',
        itemSelector: 'a'
    }, {
        xtype: 'dataview',
        scrollable: true,
        itemId: 'installableApps',
        bind: {
            store: '{apps}'
        },
        tpl: '<p class="apps-title">' + 'Apps'.t() + '</p>' +'<tpl for=".">' +
            '<tpl if="type === \'FILTER\'">' +
                '<div class="node-install-item {status}">' +
                '<img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
                '<i class="fa fa-download fa-3x"></i>' +
                '<i class="fa fa-check fa-3x"></i>' +
                '<span class="loader">Loading...</span>' +
                '<h3>{displayName}</h3>' + '<p>{desc}</p>' +
                '</div>' +
                '</tpl>' +
            '</tpl>' +
            '<p class="apps-title">' + 'Service Apps'.t() + '</p>' +
            '<tpl for=".">' +
                '<tpl if="type === \'SERVICE\'">' +
                '<div class="node-install-item {status}">' +
                '<img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
                '<i class="fa fa-download fa-3x"></i>' +
                '<i class="fa fa-check fa-3x"></i>' +
                '<span class="loader">Loading...</span>' +
                '<h3>{displayName}</h3>' + '<p>{desc}</p>' +
                '</div>' +
                '</tpl>' +
            '</tpl>',
        itemSelector: 'div'
    }]
});
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
        itemId: 'installedApps',
        bind: '{apps}',
        tpl: '<tpl for=".">' +
                '<tpl if="type === \'FILTER\'">' +
                '<a href="#config" class="app-item">' +
                '<img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
                '<span class="app-name">{displayName}</span>' +
                '</a>' +
                '</tpl>' +
            '</tpl>' +
            '<p class="apps-title">' + 'Service Apps'.t() + '</p>' +
            '<tpl for=".">' +
                '<tpl if="type === \'SERVICE\'">' +
                '<a href="#config" class="app-item">' +
                '<img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
                '<span class="app-name">{displayName}</span>' +
                '</a>' +
                '</tpl>' +
            '</tpl>',
        itemSelector: 'a'
    }, {
        xtype: 'dataview',
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

    // items: [{
    //     region: 'north',
    //     border: false,
    //     height: 44,
    //     itemId: 'apps-topnav',
    //     bodyStyle: {
    //         background: '#555',
    //         padding: '0 5px'
    //     },
    //     layout: {
    //         type: 'hbox',
    //         align: 'middle'
    //     },
    //     items: [{
    //         xtype: 'combobox',
    //         editable: false,
    //         multiSelect: false,
    //         queryMode: 'local',
    //         bind: {
    //             value: '{policyId}',
    //             store: '{policies}'
    //         },
    //         valueField: 'policyId',
    //         displayField: 'displayName',
    //         listeners: {
    //             change: 'setPolicy'
    //         }
    //     }, {
    //         xtype: 'button',
    //         baseCls: 'heading-btn',
    //         html: Ung.Util.iconTitle('Install Apps'.t(), 'file_download-16'),
    //         hrefTarget: '_self',
    //         bind: {
    //             href: '#apps/{policyId}/install'
    //         }
    //     }, {
    //         xtype: 'component',
    //         flex: 1
    //     }, {
    //         xtype: 'button',
    //         baseCls: 'heading-btn',
    //         html: 'Sessions'.t(),
    //         href: '#sessions',
    //         hrefTarget: '_self'
    //     }, {
    //         xtype: 'button',
    //         baseCls: 'heading-btn',
    //         html: 'Hosts'.t(),
    //         href: '#hosts',
    //         hrefTarget: '_self'
    //     }, {
    //         xtype: 'button',
    //         baseCls: 'heading-btn',
    //         html: 'Devices'.t(),
    //         href: '#devices',
    //         hrefTarget: '_self'
    //     }]
    // }, {
    //     region: 'center',
    //     itemId: 'apps-list',
    //     border: false,
    //     scrollable: true,
    //     bodyStyle: {
    //         background: 'transparent'
    //     },
    //     items: [{
    //         xtype: 'dataview',
    //         itemId: 'filters',
    //         margin: 10,
    //         // tpl: '<tpl for="."><div>{displayName}</div></tpl>',

    //         tpl: '<tpl for=".">' +
    //                 '<tpl if="type == \'FILTER\'">' +
    //                     '<a class="app-item" href="#apps/{policyId}/{name}">' +
    //                         '<span class="app-icon"><img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
    //                         '<span class="app-name">{displayName}</span>' +
    //                         '</span>' +
    //                         '<span class="app-state {state}"><i class="material-icons">power_settings_new</i></span>' +
    //                     '</a>' +
    //                 '</tpl>' +
    //             '</tpl>',
    //         itemSelector: 'a',
    //         bind: {
    //             store: '{nodesStore}'
    //         },
    //         style: {
    //             display: 'inline-block'
    //         }
    //     }, {
    //         xtype: 'component',
    //         cls: 'apps-separator',
    //         html: 'Service Apps'.t()
    //     }, {
    //         xtype: 'dataview',
    //         margin: 10,
    //         // tpl: '<tpl for="."><div>{displayName}</div></tpl>',

    //         tpl: '<tpl for=".">' +
    //                 '<tpl if="type == \'SERVICE\'">' +
    //                     '<a class="app-item" href="#apps/{policyId}/{name}">' +
    //                         '<span class="app-icon"><img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
    //                         '<span class="app-name">{displayName}</span>' +
    //                         '</span>' +
    //                         '<tpl if="hasPowerButton"><span class="app-state {state}"><i class="material-icons">power_settings_new</i></span></tpl>' +
    //                     '</a>' +
    //                 '</tpl>' +
    //             '</tpl>',
    //         itemSelector: 'a',
    //         bind: {
    //             store: '{nodesStore}'
    //         },
    //         style: {
    //             display: 'inline-block'
    //         }
    //     }]
    // }],
    // listeners: {
    //     //beforeRender: 'onBeforeRender'
    //     //onPolicyChange: 'onPolicyChange'
    // }
});
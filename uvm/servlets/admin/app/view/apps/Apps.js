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
            onInstalledApps: false
        },
        stores: {
            apps: {
                // data: '{appsData}',
                fields: ['name', 'displayName', 'url', 'type', 'status'],
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
        items: Ext.Array.insert(Ext.clone(Util.subNav), 0, [
        //     {
        //     xtype: 'combobox',
        //     editable: false,
        //     multiSelect: false,
        //     queryMode: 'local',
        //     hidden: true,
        //     bind: {
        //         value: '{policyId}',
        //         store: '{policies}',
        //         hidden: '{!onInstalledApps}'
        //     },
        //     valueField: 'policyId',
        //     displayField: 'displayName',
        //     // listeners: {
        //     //     change: 'setPolicy'
        //     // }
        // },
        {
            xtype: 'button',
            reference: 'policyBtn',
            // text: 'Policy 1' + ' &nbsp;<i class="fa fa-angle-down fa-lg"></i>',
            iconCls: 'fa fa-file-text-o',
            arrowVisible: false,
            // menu: {
            //     // items: [{

            //     // }];
            //     // items: [{
            //     //     xtype: 'treepanel',
            //     //     border: false,
            //     //     bodyBorder: false,
            //     //     width: 250,
            //     //     height: 200,
            //     //     rootVisible: false,
            //     //     displayField: 'name',
            //     //     store: 'policiestree',
            //     //     useArrows: true,
            //     //     columns: [{
            //     //         xtype: 'treecolumn',
            //     //         flex: 1,
            //     //         dataIndex: 'name',
            //     //         renderer: function (val, meta, rec) {
            //     //             return '<strong>' + rec.get('name') + '</strong>';
            //     //         }
            //     //     }]
            //     // }]
            // },
            bind: {
                hidden: '{!onInstalledApps}'
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
            html: 'Back to Apps',
            iconCls: 'fa fa-arrow-circle-left',
            hrefTarget: '_self',
            hidden: true,
            bind: {
                href: '#apps/{policyId}',
                hidden: '{onInstalledApps}'
            }
        }])
    }],


    items: [{
        xtype: 'dataview',
        scrollable: true,
        itemId: 'installedApps',
        bind: '{apps}',
        tpl: '<p class="apps-title">' + 'Apps'.t() + '</p>' +
            '<tpl for=".">' +
                '<tpl if="type === \'FILTER\'">' +
                '<tpl if="parentPolicy"><a class="app-item disabled"><tpl else><a href="{route}" class="app-item"></tpl>' +
                '<span class="state {targetState}"><i class="fa fa-power-off"></i></span>' +
                '<img src="' + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
                '<span class="app-name">{displayName}</span>' +
                '<tpl if="parentPolicy"><span class="parent-policy">[{parentPolicy}]</span></tpl>' +
                '</a>' +
                '</tpl>' +
            '</tpl>' +
            '<p class="apps-title">' + 'Service Apps'.t() + '</p>' +
            '<tpl for=".">' +
                '<tpl if="type === \'SERVICE\'">' +
                '<a href="{route}" class="app-item">' +
                '<span class="state {targetState}"><i class="fa fa-power-off"></i></span>' +
                '<img src="' + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
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
                '<div class="app-install-item {status}">' +
                '<img src="' + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
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
                '<div class="app-install-item {status}">' +
                '<img src="' + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
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

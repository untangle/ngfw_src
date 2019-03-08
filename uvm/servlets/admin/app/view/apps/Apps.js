Ext.define('Ung.view.apps.Apps', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.apps',
    itemId: 'apps',
    layout: 'card',

    itemType: Rpc.directData('rpc.skinInfo.appsViewType') === 'rack' ? 'rackitem' : 'simpleitem',

    controller: 'apps',
    viewModel: {
        data: {
            onInstalledApps: true,
            policyName: '',
            appsCount: 0,
            servicesCount: 0
        },
        stores: {
            installableApps: {
                sorters: [ { property: 'viewPosition', direction: 'ASC' }],
            },
            installableServices: {
                sorters: [ { property: 'viewPosition', direction: 'ASC' }]
            }
        }
    },

    defaults: {
        border: false
    },

    dockedItems: [{
        xtype: 'toolbar',
        ui: 'footer',
        dock: 'top',
        style: { background: '#D8D8D8' },
        items: [{
            xtype: 'button',
            text: 'Back to Apps',
            iconCls: 'fa fa-arrow-circle-left',
            focusable: false,
            hidden: true,
            bind: {
                text: 'Back to Apps (<strong>{policyName}</strong>)',
                hidden: '{onInstalledApps}'
            },
            handler: 'backToApps'
        }, {
            xtype: 'button',
            reference: 'policyBtn',
            hidden: true,
            iconCls: 'fa fa-file-text-o',
            focusable: false,
            bind: {
                text: '{policyName}',
                hidden: '{!onInstalledApps || !policyManagerInstalled}'
            }
        }, {
            xtype: 'button',
            text: 'Install Apps'.t(),
            iconCls: 'fa fa-download',
            focusable: false,
            handler: 'showInstall',
            hidden: true,
            bind: {
                hidden: '{!onInstalledApps}'
            }
        }, {
            xtype: 'component',
            reference: 'installHeader',
            html: '',
            hidden: true,
            bind: {
                html: 'Available Apps for &nbsp;<i class="fa fa-file-text-o"></i> <strong>{policyName}</strong>',
                hidden: '{onInstalledApps || !policyManagerInstalled}'
            }
        }]
    }],


    items: [{
        itemId: 'installedApps',
        xtype: Rpc.directData('rpc.skinInfo.appsViewType') === 'rack' ? 'apps-rack' : 'apps-simple',
    }, {
        scrollable: true,

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
                    '<img src="' + '/icons/apps/{name}.svg" width=80 height=80/>' +
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
                    '<img src="' + '/icons/apps/{name}.svg" width=80 height=80/>' +
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

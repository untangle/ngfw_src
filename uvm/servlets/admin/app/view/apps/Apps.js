Ext.define('Ung.view.apps.Apps', {
    extend: 'Ext.container.Container',
    xtype: 'ung.apps',
    layout: 'border',
    requires: [
        'Ung.view.apps.AppsController',
        'Ung.view.apps.AppsModel',
        'Ung.view.apps.AppItem'
    ],

    controller: 'apps',
    viewModel: 'apps',

    config: {
        policy: undefined
    },

    defaults: {
        border: false
    },

    items: [{
        region: 'north',
        border: false,
        height: 44,
        itemId: 'apps-topnav',
        bodyStyle: {
            background: '#555',
            padding: '0 5px'
        },
        layout: {
            type: 'hbox',
            align: 'middle'
        },
        items: [{
            xtype: 'combobox',
            editable: false,
            multiSelect: false,
            queryMode: 'local',
            bind: {
                value: '{policyId}',
                store: '{policies}'
            },
            valueField: 'policyId',
            displayField: 'displayName',
            listeners: {
                change: 'setPolicy'
            }
        }, {
            xtype: 'button',
            baseCls: 'heading-btn',
            html: Ung.Util.iconTitle('Install Apps'.t(), 'file_download-16'),
            hrefTarget: '_self',
            bind: {
                href: '#apps/{policyId}/install'
            }
        }, {
            xtype: 'component',
            flex: 1
        }, {
            xtype: 'button',
            baseCls: 'heading-btn',
            html: 'Sessions'.t(),
            href: '#sessions',
            hrefTarget: '_self'
        }, {
            xtype: 'button',
            baseCls: 'heading-btn',
            html: 'Hosts'.t(),
            href: '#hosts',
            hrefTarget: '_self'
        }, {
            xtype: 'button',
            baseCls: 'heading-btn',
            html: 'Devices'.t(),
            href: '#devices',
            hrefTarget: '_self'
        }]
    }, {
        region: 'center',
        itemId: 'apps-list',
        border: false,
        scrollable: true,
        bodyStyle: {
            background: 'transparent'
        },
        items: [{
            xtype: 'dataview',
            itemId: 'filters',
            margin: 10,
            // tpl: '<tpl for="."><div>{displayName}</div></tpl>',

            tpl: '<tpl for=".">' +
                    '<tpl if="type == \'FILTER\'">' +
                        '<a class="app-item" href="#apps/{policyId}/{name}">' +
                            '<span class="app-icon"><img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
                            '<span class="app-name">{displayName}</span>' +
                            '</span>' +
                            '<span class="app-state {state}"><i class="material-icons">power_settings_new</i></span>' +
                        '</a>' +
                    '</tpl>' +
                '</tpl>',
            itemSelector: 'a',
            bind: {
                store: '{nodesStore}'
            },
            style: {
                display: 'inline-block'
            }
        }, {
            xtype: 'component',
            cls: 'apps-separator',
            html: 'Service Apps'.t()
        }, {
            xtype: 'dataview',
            margin: 10,
            // tpl: '<tpl for="."><div>{displayName}</div></tpl>',

            tpl: '<tpl for=".">' +
                    '<tpl if="type == \'SERVICE\'">' +
                        '<a class="app-item" href="#apps/{policyId}/{name}">' +
                            '<span class="app-icon"><img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
                            '<span class="app-name">{displayName}</span>' +
                            '</span>' +
                            '<tpl if="hasPowerButton"><span class="app-state {state}"><i class="material-icons">power_settings_new</i></span></tpl>' +
                        '</a>' +
                    '</tpl>' +
                '</tpl>',
            itemSelector: 'a',
            bind: {
                store: '{nodesStore}'
            },
            style: {
                display: 'inline-block'
            }
        }]
    }],
    listeners: {
        //beforeRender: 'onBeforeRender'
        //onPolicyChange: 'onPolicyChange'
    }
});
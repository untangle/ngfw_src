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
            html: 'Install Apps'.t(),
            hrefTarget: '_self',
            bind: {
                href: '#apps/{policyId}/install'
            }
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
            xtype: 'container',
            margin: 10,
            reference: 'filters',

            style: {
                display: 'inline-block'
            }
        }, {
            xtype: 'component',
            cls: 'apps-separator',
            html: 'Service Apps'.t()
        }, {
            xtype: 'container',
            margin: 10,
            reference: 'services',
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
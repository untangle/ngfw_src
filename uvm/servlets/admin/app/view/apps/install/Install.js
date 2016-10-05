Ext.define('Ung.view.apps.install.Install', {
    extend: 'Ext.container.Container',
    xtype: 'ung.appsinstall',
    layout: 'border',
    requires: [
        'Ung.view.apps.install.InstallController',
        'Ung.view.apps.install.Item'
        //'Ung.view.main.MainModel'
    ],

    controller: 'appsinstall',
    viewModel: true,

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
            xtype: 'component',
            style: {
                color: '#CCC'
            },
            flex: 1,
            html: 'Select Apps and Services to Install'.t()
        }, {
            xtype: 'button',
            html: 'Done'.t(),
            hrefTarget: '_self',
            bind: {
                href: '#apps/{policyId}'
            }
        }]
    }, {
        region: 'center',
        itemId: 'apps-list',
        bodyStyle: {
            background: 'transparent'
        },
        scrollable: true,
        items: [{
            xtype: 'component',
            cls: 'apps-separator',
            html: Ung.Util.iconTitle('Apps'.t(), 'apps')
        }, {
            xtype: 'container',
            margin: 10,
            reference: 'filters',
            style: {
                display: 'inline-block'
            }
        }, {
            xtype: 'component',
            cls: 'apps-separator',
            html: Ung.Util.iconTitle('Service Apps'.t(), 'build')
        }, {
            xtype: 'container',
            margin: 10,
            reference: 'services',
            style: {
                display: 'inline-block'
            }

        }]
    }]
});
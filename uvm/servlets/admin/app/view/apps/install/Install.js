Ext.define('Ung.view.apps.install.Install', {
    extend: 'Ext.container.Container',
    xtype: 'ung.appsinstall',
    layout: 'border',
    requires: [
        'Ung.view.apps.install.InstallController',
        'Ung.view.apps.install.Item'
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
            xtype: 'button',
            baseCls: 'heading-btn',
            html: Ung.Util.iconTitle('Back to Apps', 'arrow_back-16'),
            hrefTarget: '_self',
            bind: {
                href: '#apps/{policyId}'
            }
        }, {
            xtype: 'component',
            hidden: true,
            style: {
                color: '#CCC'
            },
            flex: 1,
            html: 'Select Apps and Services to Install'.t()
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
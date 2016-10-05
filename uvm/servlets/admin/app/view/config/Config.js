Ext.define('Ung.view.config.Config', {
    extend: 'Ext.container.Container',
    xtype: 'ung.config',
    layout: 'fit',
    requires: [
        'Ung.view.config.ConfigController',
        //'Ung.view.apps.AppsModel',

        'Ung.view.config.ConfigItem'
    ],

    controller: 'config',
    //viewModel: 'apps',

    defaults: {
        border: false
    },

    items: [{
        border: false,
        scrollable: true,
        bodyStyle: {
            background: 'transparent'
        },
        items: [{
            xtype: 'component',
            cls: 'apps-separator',
            html: Ung.Util.iconTitle('Configuration'.t(), 'tune')
        }, {
            xtype: 'container',
            margin: 10,
            reference: 'configs',

            style: {
                display: 'inline-block'
            }
        }, {
            xtype: 'component',
            cls: 'apps-separator',
            html: Ung.Util.iconTitle('Tools'.t(), 'build')
        }, {
            xtype: 'container',
            margin: 10,
            reference: 'tools',
            style: {
                display: 'inline-block'
            }

        }]
    }],
    listeners: {
        beforerender: 'onBeforeRender'
        //onPolicyChange: 'onPolicyChange'
    }
});
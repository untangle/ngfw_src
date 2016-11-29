Ext.define('Ung.view.config.Config', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.config',
    requires: [
        'Ung.view.config.ConfigController',
        'Ung.view.config.ConfigItem',
        'Ung.view.config.ConfigSettings'
    ],

    controller: 'config',
    //viewModel: 'apps',

    defaults: {
        border: false
    },

    layout: 'card',

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
        afterrender: 'onBeforeRender'
        //onPolicyChange: 'onPolicyChange'
    }
});
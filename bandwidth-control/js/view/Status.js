Ext.define('Ung.apps.bandwidthcontrol.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-bandwidth-control-status',
    itemId: 'status',
    title: 'Status'.t(),
    scrollable: true,

    viewModel: true,

    layout: 'border',
    items: [{
        region: 'center',
        border: false,
        bodyPadding: 10,
        scrollable: 'y',

        defaults: {
            xtype: 'fieldset',
        },

        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/icons/apps/bandwidth-control.svg" width="80" height="80"/>' +
                '<h3>Bandwidth Control</h3>' +
                '<p>' + 'Bandwidth Control monitors, manages, and shapes bandwidth usage on the network'.t() + '</p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appstate',
            hidden: true,
            bind: {
                hidden: '{!isConfigured}'
            },
        }, {
            title: '<i class="fa fa-cog"></i> ' + 'Configuration'.t(),
            padding: 10,
            margin: '20 0',
            cls: 'app-section',
            items: [{
                xtype: 'component',
                html: 'Bandwidth Control is unconfigured. Use the Wizard to configure Bandwidth Control.'.t(),
                hidden: true,
                bind: {
                    hidden: '{isConfigured}'
                }
            }, {
                xtype: 'component',
                html: 'Bandwidth Control is configured'.t(),
                hidden: true,
                bind: {
                    hidden: '{!isConfigured}'
                }
            }, {
                xtype: 'component',
                html: 'Bandwidth Control is enabled, but QoS is not enabled. Bandwidth Control requires QoS to be enabled.'.t(),
                hidden: true,
                bind: {
                    hidden: '{qosEnabled}'
                }
            }, {
                xtype: 'button',
                margin: '10 0 0 0',
                text: 'Run Bandwidth Control Setup Wizard'.t(),
                iconCls: 'fa fa-magic',
                handler: 'runWizard'
            }]
        }, {
            xtype: 'appreports',
            hidden: true,
            bind: {
                hidden: '{!isConfigured}'
            },
        }]
    }, {
        region: 'west',
        border: false,
        width: Math.ceil(Ext.getBody().getViewSize().width / 4),
        split: true,
        items: [{
            xtype: 'appsessions',
            region: 'north',
            height: 200,
            split: true,
        }, {
            xtype: 'appmetrics',
            region: 'center'
        }],
        bbar: [{
            xtype: 'appremove',
            width: '100%'
        }]
    }]
});

/**
 * Holds the node settings and controls all the nodes
 */
Ext.define('Ung.view.node.Settings', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.nodesettings',
    layout: 'border',
    requires: [
        'Ung.view.node.SettingsController',
        'Ung.view.node.SettingsModel',
        'Ung.view.node.Status',
        'Ung.view.node.Reports',

        'Ung.util.Util',
        //'Ung.node.WebFilter',
        //'Ung.node.BandwidthControl',
        //'Ung.node.VirusBlocker',
        //'Ung.node.SpamBlocker',

        'Ung.model.NodeMetric',
        'Ung.view.grid.Grid',
        'Ung.view.grid.Conditions',
        'Ung.view.grid.ActionColumn',
        'Ung.model.GenericRule'
    ],

    controller: 'nodesettings',
    viewModel: {
        type: 'nodesettings'
    },

    border: false,
    defaults: {
        border: false
    },

    //title: 'test',

    items: [{
        region: 'north',
        border: false,
        height: 44,
        layout: {
            type: 'hbox',
            align: 'middle'
        },
        bodyStyle: {
            background: '#555',
            padding: '0 5px',
            color: '#FFF',
            lineHeight: '44px'
        },
        items: [{
            xtype: 'button',
            text: 'Back to Apps'.t(),
            hrefTarget: '_self',
            bind: {
                href: '#apps/{policyId}'
            }
        }, {
            xtype: 'component',
            margin: '0 0 0 10',
            bind: {
                html: '{nodeProps.displayName}'
            }
        }]
    }],

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'bottom',
        items: [{
            xtype: 'button',
            text: Ung.Util.iconTitle('Remove', 'remove_circle-16'),
            handler: 'removeNode'
        }, {
            xtype: 'button',
            text: Ung.Util.iconTitle('Save', 'save-16'),
            handler: 'saveSettings'
        }]
    }]
});
Ext.define('Ung.apps.intrusionprevention.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-intrusion-prevention-status',
    itemId: 'status',
    title: 'Status'.t(),
    scrollable: true,

    layout: 'border',

    items: [{
        region: 'center',
        border: false,
        bodyPadding: 10,

        layout: {
            type: 'vbox',
            align: 'stretch'
        },

        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/icons/apps/intrusion-prevention.svg" width="80" height="80"/>' +
                '<h3>Intrusion Prevention</h3>' +
                '<p>' + 'Intrusion Prevention scans, detects, and blocks attacks and suspicious traffic using signatures.'.t() + '</p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appstate'
        },{
            xtype: 'fieldset',
            title: '<i class="fa fa-clock-o"></i> ' + "Overview".t(),
            defaults: {
                labelWidth: 200
            },
            padding: 10,
            collapsed: true,
            disabled: true,
            bind: {
                collapsed: '{state.on !== true || state.power === true}',
                disabled: '{state.on !== true || state.power === true}'
            },

            items: [{
                xtype: 'displayfield',
                fieldLabel: "Signatures available".t(),
                bind: '{signatureStatusTotal}'
            },{
                xtype: 'displayfield',
                fieldLabel: "Signatures set to log".t(),
                bind: '{signatureStatusLog}'
            },{
                xtype: 'displayfield',
                fieldLabel: "Signatures set to block".t(),
                bind: '{signatureStatusBlock}'
            },{
                xtype: 'displayfield',
                fieldLabel: "Signatures disabled".t(),
                bind: '{signatureStatusDisable}'
            },{
                xtype: 'displayfield',
                fieldLabel: "Last check for updates".t(),
                bind: '{lastUpdateCheck}'
            }, {
                xtype: 'displayfield',
                fieldLabel: "Last update".t(),
                bind: '{lastUpdate}'
            }, {
                xtype: 'component',
                bind:{
                    html: Ext.String.format("{0}Note:{1} {2} continues to maintain the recommended signature settings through automatic updates. You are free to add signatures, however it is not required.".t(), '<b>', '</b>', '{companyName}')
                }
            }]
        }, {
            xtype: 'appreports'
        }]
    }, {
        region: 'west',
        border: false,
        width: Math.ceil(Ext.getBody().getViewSize().width / 4),
        split: true,
        items: [{
            xtype: 'appmemory',
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

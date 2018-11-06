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
                bind:{
                    value: '{signatureStatusTotal}',
                    hidden: '{signatureStatusTotal == 0}'
                }
            },{
                xtype: 'displayfield',
                fieldLabel: '&nbsp;'.repeat(5) + Ung.apps.intrusionprevention.Main.signatureActions.findRecord('value', 'log').get('description'),
                bind: {
                    value: '{signatureStatusLog}',
                    hidden: '{signatureStatusLog == 0}'
                }
            },{
                xtype: 'displayfield',
                fieldLabel: '&nbsp;'.repeat(5) + Ung.apps.intrusionprevention.Main.signatureActions.findRecord('value', 'block').get('description'),
                bind: {
                    value: '{signatureStatusBlock}',
                    hidden: '{signatureStatusBlock == 0}'
                }
            },{
                xtype: 'displayfield',
                fieldLabel: '&nbsp;'.repeat(5) + Ung.apps.intrusionprevention.Main.signatureActions.findRecord('value', 'disable').get('description'),
                bind: {
                    value: '{signatureStatusDisable}',
                    hidden: '{signatureStatusDisable == 0}'
                }
            }, {
                xtype: 'component',
                style: 'background-color: yellow;',
                padding: '10px 0px 10px 0px',
                bind:{
                    html: Ext.String.format("{0}Warning:{1} No signatures are enabled for Log or Block.".t(), '<b>', '</b>'),
                    hidden: '{signatureStatusTotal == 0 || signatureStatusTotal != signatureStatusDisable}'
                }
            }, {
                xtype: 'displayfield',
                fieldLabel: "Last update".t(),
                bind: {
                    value: '{lastUpdate}',
                    hidden: '{lastUpdate == ""}'
                }
            },{
                xtype: 'displayfield',
                fieldLabel: '&nbsp;'.repeat(5) + "Last check".t(),
                bind: {
                    value: '{lastUpdateCheck}',
                    hidden: '{lastUpdateCheck == ""}'
                }
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

Ext.define('Ung.apps.threatprevention.view.ThreatLookup', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-threat-prevention-threatlookup',
    itemId: 'lookup',
    title: 'Threat Lookup'.t(),
    scrollable: true,
    bodyPadding: 10,
    defaultButton: 'searchButton',
    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: {
            fontSize: '12px',
            fontWeight: 600
        },
        html: 'Lookup the threat information for websites or IP Addresses.'.t()
    }],

    items: [{
        xtype: 'fieldset',
        title: 'IP Address and URL Threats'.t(),
        layout: 'vbox',
        items: [{
            xtype: 'displayfield',
            value: 'Threat Prevention must be enabled to perform lookups'.t(),
            bind: {
                hidden: '{state.on == true}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'Lookup Threat'.t(),
            fieldIndex: 'threatLookupInput',
            margin: '10 0 0 0',
            bind: {
                hidden: '{state.on == false}',
                value: '{threatLookupInfo.inputVal}'
            },
        }, {
            xtype: 'button',
            reference: 'searchButton',
            text: 'Search'.t(),
            iconCls: 'fa fa-search',
            handler: 'handleThreatLookup',
            margin: '10 0 10 0',
            disabled: true,
            bind: {
                hidden: '{state.on == false}',
                disabled: '{threatLookupInfo.inputVal.length === 0}'
            }
        }, {
            xtype: 'fieldset',
            title: 'Threat Results'.t(),
            layout: 'vbox',
            hidden: true,
            bind: {
                hidden: '{!threatLookupInfo.address.length === 0}'
            },
            items: [{
                xtype: 'displayfield',
                labelWidth: 160,
                fieldLabel: 'Address/URL'.t(),
                fieldIndex: 'threatLookupAddress',
                bind: {
                    value: '{threatLookupInfo.address}',
                    hidden: '{threatLookupInfo.address.length === 0}'
                }
            }, {
                xtype: 'displayfield',
                labelWidth: 160,
                fieldLabel: 'Category'.t(),
                fieldIndex: 'threatLookupCategory',
                bind: {
                    value: '{threatLookupInfo.category}',
                    hidden: '{threatLookupInfo.category.length === 0}'
                }
            }, {
                xtype: 'displayfield',
                labelWidth: 160,
                fieldLabel: 'Reputation Score'.t(),
                fieldIndex: 'threatLookupReputationScore',
                bind: {
                    value: '{threatLookupInfo.score}',
                    hidden: '{threatLookupInfo.score.length === 0}'
                }
            }, {
                xtype: 'displayfield',
                labelWidth: 160,
                fieldLabel: 'Reputation Level'.t(),
                fieldIndex: 'threatLookupReputationLevel',
                bind: {
                    value: '{threatLookupInfo.level}',
                    hidden: '{threatLookupInfo.level.length === 0}'
                }
            }, {
                xtype: 'displayfield',
                labelWidth: 160,
                fieldLabel: 'Reputation Level Details'.t(),
                fieldIndex: 'threatLookupReputationLevelDetails',
                width: '100%',
                bind: {
                    value: '{threatLookupInfo.levelDetails}',
                    hidden: '{threatLookupInfo.levelDetails.length === 0}'
                }
            }]
        }]
    }]
});

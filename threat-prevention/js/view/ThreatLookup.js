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
        width: '100%',
        items: [{
            xtype: 'textfield',
            fieldLabel: 'IP Address or URL'.t(),
            labelWidth: 150,
            _neverDirty: true,
            fieldIndex: 'threatLookupInput',
            width: 500,
            vtype: 'ipOrUrl',
            margin: '10 0 0 0',
            bind: {
                hidden: '{state.on == false}',
                value: '{threatLookupInfo.inputVal}'
            }
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
        },{
            xtype: 'displayfield',
            value: 'IP Address is in local network, no lookup performed.'.t(),
            bind: {
                hidden: '{threatLookupInfo.local === false}'
            }
        }, {
            xtype: 'fieldset',
            title: 'Threat Results'.t(),
            hidden: true,
            layout: {
                type :'vbox',
                align : 'stretch'
            },
            bind: {
                hidden: '{threatLookupInfo.resultAddress.length === 0}'
            },
            items: [{
                xtype: 'displayfield',
                labelWidth: 160,
                fieldLabel: 'URL (IP Address)'.t(),
                bind: {
                    value: '{threatLookupInfo.resultAddress}',
                    hidden: '{threatLookupInfo.resultAddress.length === 0}'
                }
            },{
                xtype: 'displayfield',
                labelWidth: 160,
                fieldLabel: 'Server Reputation'.t(),
                bind: {
                    value: '{threatLookupInfo.resultServerReputation}',
                    hidden: '{threatLookupInfo.resultServerReputation.length === 0}'
                }
            },{
                xtype: 'displayfield',
                labelWidth: 160,
                fieldLabel: 'Client Reputation'.t(),
                bind: {
                    value: '{threatLookupInfo.resultClientReputation}',
                    hidden: '{threatLookupInfo.resultClientReputation.length === 0}'
                }
            }]
        }]
    }]
});

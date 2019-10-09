Ext.define('Ung.apps.ipreputation.view.Reputation', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ip-reputation-reputation',
    itemId: 'reputation',
    title: 'Reputation'.t(),

    viewModel: true,
    scrollable: true,

    bodyPadding: 10,

    items: [{
        xtype: 'fieldset',
        title: 'IP Address Threats'.t(),
        layout: 'vbox',
        items:[{
            xtype: 'container',
            layout: 'hbox',
            margin: '0 0 20 0',
            items: [{
                xtype: 'label',
                text: 'Reputation'.t(),
                margin: '0 5 0 0',
                width: 100
            },{
                xtype: 'container',
                layout: 'vbox',
                items:[{
                    xtype: 'component',
                    itemId: 'threatRange'
                },{
                    xtype: 'threatslider',
                    viewLabel: 'threatLabel',
                    rangeLabel: 'threatRange',
                    width: 400,
                    height: 20,
                    maxValue: 100,
                    minValue: 0,
                    increment: 20,
                    bind:{
                        value: '{settings.threatLevel}'
                    },
                    labelTpl: 'Match <i>{0}</i> or worse'.t(),
                    tipTpl: '{0} - {1}'.t(),
                    rangeTpl: '<table style="width:400px; height:75px;border-collapse:collapse; margin-top: -13px;"><tr>' + 
                        '<td>High Risk</td>'+
                        '<td>Suspicious</td>'+
                        '<td>Moderate Risk</td>'+
                        '<td>Low Risk</td>'+
                        '<td>Trustworthy</td>'+
                        '</tr><tr>' + 
                        '<td style="background-color:#{0};"></td>'+
                        '<td style="background-color:#{1};"></td>'+
                        '<td style="background-color:#{2};"></td>'+
                        '<td style="background-color:#{3};"></td>'+
                        '<td style="background-color:#{4};"></td>'+
                        '</tr></table>'
                },{
                    xtype: 'component',
                    itemId: 'threatLabel'
                }]
            }]
        },{
            xtype: 'displayfield',
            fieldLabel: 'Categories'.t(),
            labelWidth: 100,
            bind: {
                value: '{threatList}'
            },
            padding: '0 0 10 0'
        },{
            xtype: 'combo',
            fieldLabel: 'Action'.t(),
            editable: false,
            matchFieldWidth: false,
            queryMode: 'local',
            valueField: 'value',
            displayField: 'description',
            bind:{
                value: '{settings.action}'
            },
            store: Ung.common.ipreputation.references.actions
        },{
            xtype: 'checkbox',
            fieldLabel: 'Flag'.t(),
            bind:{
                value: '{settings.flag}'
            }
        }]
    }]
});

Ext.define('Ung.apps.threatprevention.view.Threats', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-threat-prevention-threats',
    itemId: 'threats',
    title: 'Threats'.t(),

    viewModel: true,
    scrollable: true,

    bodyPadding: 10,

    items: [{
        xtype: 'fieldset',
        title: 'IP Address and URL Threats'.t(),
        layout: 'vbox',
        items:[{
            itemId: 'threatReputation',
            xtype: 'container',
            layout: 'hbox',
            margin: '0 0 20 0',
            items: [{
                xtype: 'label',
                text: 'Reputation Threshold'.t(),
                margin: '0 5 0 0',
                width: 125
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
                    width: 450,
                    height: 20,
                    maxValue: 100,
                    minValue: 0,
                    increment: 20,
                    bind:{
                        value: '{settings.reputationThreshold}'
                    },
                    labelTpl: 'Block traffic assessed as <i>{0}</i>'.t(),
                    labelNoneTpl: '<br><b>' + 'Warning: No traffic will be blocked'.t() + '</b>',
                    labelAllTpl: '<br><b>' + 'Warning: All traffic will be blocked'.t() + '</b>',
                    tipTpl: '{0} - {1}'.t(),
                    rangeTpl: '<table style="width:450px; height:75px;border-collapse:collapse; margin-top: -29px"><tr>' + 
                        '<td style="vertical-align:bottom; width:20%;">Trustworthy</td>'+
                        '<td style="vertical-align:bottom; width:20%;">Low Risk</td>'+
                        '<td style="vertical-align:bottom; width:20%;">Moderate Risk</td>'+
                        '<td style="vertical-align:bottom; width:20%;">Suspicious</td>'+
                        '<td style="vertical-align:bottom; width:20%;">High Risk</td>'+
                        '</tr><tr>' + 
                        '<td style="background-color:#{0}; height:32px;"></td>'+
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
            labelWidth: 125,
            bind: {
                value: '{threatList}'
            },
            padding: '0 10 10 0'
        // },{
        //     xtype: 'displayfield',
        //     value: 'Matching traffic will be blocked'.t(),
        //     padding: '0 10 10 0'
        }]
    }]
});

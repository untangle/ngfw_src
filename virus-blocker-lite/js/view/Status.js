Ext.define('Ung.apps.virusblockerlite.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-virus-blocker-lite-status',
    itemId: 'status',
    title: 'Status'.t(),
    scrollable: true,
    withValidation: false,
    viewModel: true,

    layout: 'border',
    items: [{
        region: 'center',
        border: false,
        bodyPadding: 10,
        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/icons/apps/virus-blocker-lite.svg" width="80" height="80"/>' +
                '<h3>Virus Blocker Lite</h3>' +
                '<p>' + 'Virus Blocker Lite provides basic virus protection and can be resource intensive in some environments.'.t() +
                '<br><br>' + 'Upgrade to Virus Blocker which efficiently leverages signatures from Bitdefender&reg, the leader in speed and efficiency, whose threat lab experts work 24-hours a day, 365-days a year to identify emerging threats.'.t() +
                ' <a target="_blank" href="' + rpc.uriManager.getUriWithPath('https://edge.arista.com/shop/virus-blocker') + '">LEARN MORE<a/></p>'
        }, {
            xtype: 'appstate',
        }, {
            xtype: 'displayfield',
            fieldLabel: '<STRONG>' + 'Signatures were last updated'.t() + '</STRONG>',
            labelWidth: 250,
            bind: '{getSignatureTimestamp}'
        }, {
            xtype: 'appreports'
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

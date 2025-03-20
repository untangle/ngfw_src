Ext.define('Ung.apps.spamblockerlite.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-spam-blocker-lite-status',
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
            html: '<img src="/icons/apps/spam-blocker-lite.svg" width="80" height="80"/>' +
                '<h3>Spam Blocker Lite</h3>' +
                '<p>' + 'Spam Blocker Lite detects, blocks, and quarantines spam before it reaches users\' mailboxes.'.t() +
                '<br><br>' + 'Upgrade to Spam Blocker which includes additional anti-spam engines to provide a catch rate of 99.5% and minimal false positives.  Spam Blocker is updated in real-time, and quarantine is automatic.'.t() +
                ' <a target="_blank" href="' + rpc.uriManager.getUriWithPath('https://edge.arista.com/shop/Spam-Blocker') + '">LEARN MORE<a/></p>'
        }, {
            xtype: 'appstate',
        },{
            xtype: 'fieldset',
            title: '<i class="fa fa-clock-o"></i> ' + "Updates".t(),
            defaults: {
                labelWidth: 200
            },
            padding: 10,
            collapsed: true,
            disabled: true,
            bind: {
                collapsed: '{!state.on}',
                disabled: '{!state.on}',
            },

            items: [{
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
                    html: Ext.String.format("{0}Note:{1} {2} continues to maintain the default signature settings through automatic updates. You are free to modify and add signatures, however it is not required.".t(), '<b>', '</b>', '{companyName}')
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

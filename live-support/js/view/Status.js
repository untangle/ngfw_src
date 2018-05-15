Ext.define('Ung.apps.livesupport.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-live-support-status',
    itemId: 'status',
    title: 'Status'.t(),
    scrollable: true,

    requires: [
        'Ung.cmp.LicenseLoader'
    ],

    items: [{
        title: 'Status'.t(),
        border: false,
        bodyPadding: 10,
        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/icons/apps/live-support.svg" width="80" height="80"/>' +
                '<h3>Live Support</h3>' +
                '<p>' + 'Live Support provides on-demand help for any technical issues.'.t() + '</p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'fieldset',
            title: '<i class="fa fa-life-ring"></i> ' + 'Live Support'.t(),
            padding: 10,
            margin: '20 0',
            cls: 'app-section',
            items: [{
                xtype: 'component',
                bind: {
                    html: Ext.String.format('This {0} Server is entitled to Live Support.'.t(), Rpc.directData('rpc.companyName'))
                },
                html: Ext.String.format('This {0} Server is entitled to Live Support.'.t(), '{companyName}'),
                margin: '0 0 10 0'
            }, {
                xtype: 'button',
                text: 'Get Support!'.t(),
                handler: 'supportHandler'
            }]
        }, {
            xtype: 'fieldset',
            title: '<i class="fa fa-info-circle"></i> ' + 'Support Information'.t(),
            padding: 10,
            margin: '20 0',
            cls: 'app-section',
            defaults: {
                xtype: 'displayfield',
                labelWidth: 50,
                labelAlign: 'right'
            },
            items: [{
                fieldLabel: '<strong>' + 'UID'.t() + '</strong>',
                bind: '{serverUID}'
            }, {
                fieldLabel: '<strong>' + 'Build'.t() + '</strong>',
                bind: '{fullVersionAndRevision}'
            }]
        }, {
            xtype: 'appremove'
        }]
    }]

});

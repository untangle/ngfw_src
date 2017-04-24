Ext.define('Ung.cmp.ConfigPanel', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.configpanel',
    layout: 'fit',

    dockedItems: [{
        xtype: 'toolbar',
        ui: 'navigation',
        dock: 'top',
        border: false,
        style: {
            background: '#333435',
            zIndex: 9997
        },
        defaults: {
            border: false,
        },
        items: Ext.Array.insert(Ext.clone(Util.subNav), 0, [{
            text: 'Back to Config',
            iconCls: 'fa fa-arrow-circle-left fa-lg',
            hrefTarget: '_self',
            href: '#config'
        }, '-', {
            xtype: 'component',
            padding: '0 5',
            style: {
                color: '#CCC'
            },
            bind: { html: '<img src="/skins/modern-rack/images/admin/config/{iconName}.png" style="vertical-align: middle;" width="17" height="17"/> <strong>{title}</strong>' }
        }])
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        // border: false,
        items: ['->', {
            text: '<strong>' + 'Save'.t() + '</strong>',
            // scale: 'large',
            iconCls: 'fa fa-floppy-o fa-lg',
            handler: 'saveSettings'
        }]
    }],

    listeners: {
        // generic listener for all tabs in Apps, redirection
        beforetabchange: function (tabPanel, newCard, oldCard) {
            Ung.app.redirectTo('#config/' + tabPanel.name + '/' + newCard.getItemId());
        },

        // tabchange: function (tabPanel, newCard) {
        //     var helpSource = tabPanel.getViewModel().get('props.name').replace(/-/g, '_');
        //     if (newCard.getItemId() !== 'status') {
        //         helpSource += '_' + newCard.getItemId();
        //     }
        //     tabPanel.down('#helpBtn').setHref(rpc.helpUrl + '?source=' + helpSource + '&' + Util.getAbout());
        // },

        // afterrender: function (tabPanel) {
        //     var helpSource = tabPanel.getViewModel().get('props.name').replace(/-/g, '_');
        //     var currentCard = tabPanel.getActiveTab();
        //     if (currentCard.getItemId() !== 'status') {
        //         helpSource += '_' + currentCard.getItemId();
        //     }
        //     tabPanel.down('#helpBtn').setHref(rpc.helpUrl + '?source=' + helpSource + '&' + Util.getAbout());
        // }
    }

});

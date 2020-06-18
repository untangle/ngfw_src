Ext.define('Ung.apps.threatprevention.view.Advanced', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-threat-prevention-advanced',
    itemId: 'advanced',
    title: 'Advanced'.t(),
    scrollable: true,
    bodyPadding: 10,

    items: [{
        xtype: 'fieldset',
        title: 'Custom block page URL'.t(),
        padding: '10 15',
        layout: 'fit',
        items: [{
            xtype: 'textfield',
            emptyText: 'http://example.com',
            bind: '{settings.customBlockPageUrl}',
            listeners: {
                // add 'http://' prefix if missing
                blur: function (el) {
                    var url = el.getValue();
                    if (url.length) {
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            el.setValue("http://" + url);
                        }
                    }
                }
            }
        }]
    }, {
        xtype: 'fieldset',
        title: 'Block options'.t(),
        padding: '10 15',
        cls: 'app-section',
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'checkbox',
            boxLabel: 'Close connection for blocked HTTPS sessions without redirecting to block page'.t(),
            bind: '{settings.closeHttpsBlockEnabled}'
        }]
    }]
});

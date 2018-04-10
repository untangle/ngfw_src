Ext.define('Ung.config.administration.view.Skins', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-administration-skins',
    itemId: 'skins',
    scrollable: true,

    viewModel: true,
    title: 'Skins'.t(),

    bodyPadding: 10,

    items: [{
        xtype: 'combo',
        width: 300,
        bind: {
            store: '{skins}',
            value: '{skinSettings.skinName}'
        },
        fieldLabel: '<strong>' + 'Administration Skin'.t() + '</strong>',
        labelAlign: 'top',
        displayField: 'displayName',
        valueField: 'name',
        forceSelection: true,
        editable: false,
        queryMode: 'local',
        listeners: {
            change: 'skinChange'
        }
    }, {
        xtype: 'filefield',
        margin: '10 0 0 0',
        fieldLabel: '<strong>' + 'Upload New Skin'.t() + '</strong>',
        labelAlign: 'top',
        width: 300,
        allowBlank: false,
        validateOnBlur: false
    }, {
        xtype: 'button',
        margin: '5 0 0 0',
        text: 'Upload'.t(),
        iconCls: 'fa fa-upload',
        handler: Ext.bind(function() {
            Ext.Msg.alert('Wait...', 'Not implemented yet!');
            // this.panelSkins.onUpload();
        }, this)
    }]
});

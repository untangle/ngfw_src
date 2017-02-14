Ext.define('Ung.config.administration.view.Skins', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.administration.skins',
    itemId: 'skins',

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
        fieldLabel: 'Administration Skin'.t(),
        labelAlign: 'top',
        displayField: 'displayName',
        valueField: 'name',
        forceSelection: true,
        editable: false,
        queryMode: 'local'
    }
    // {
    //     xtype: 'filefield',
    //     fieldLabel: 'Upload New Skin'.t(),
    //     labelAlign: 'top',
    //     width: 300,
    //     allowBlank: false,
    //     validateOnBlur: false
    // }, {
    //     xtype: 'button',
    //     text: 'Upload'.t(),
    //     handler: Ext.bind(function() {
    //         this.panelSkins.onUpload();
    //     }, this)
    // }
    ]


});
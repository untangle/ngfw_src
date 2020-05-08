Ext.define('Ung.cmp.CopyToClipboard', {
    extend: 'Ext.form.FieldContainer',
    alias: 'widget.copytoclipboard',

    layout: {
        type: 'hbox'
    },

    items: [{
        xtype: 'button',
        baseCls: 'fa fa-copy',
        margin: '5 0 0 5',
        tooltip: 'Copy to Clipboard'.t(),
        handler: Util.copyToClipboard
    }],

    constructor: function(config) {
        var me = this;

        if(config.items){
            var buttonExists = false;
            config.items.forEach(function(item){
                if(item.handler == Util.copyToClipboard){
                    buttonExists = true;
                }
            });
            if(buttonExists == false){
                me.items.forEach(function(item){
                    config.items.push(item);
                });    
            }
        }

        me.callParent(arguments);
    }
});
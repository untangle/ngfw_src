Ext.define('Ung.cmp.CopyToClipboard', {
    extend: 'Ext.form.FieldContainer',
    alias: 'widget.copytoclipboard',

    stripPrefix: null,
    stripSuffix: null,
    targetKey: 'value',

    layout: {
        type: 'hbox'
    },

    items: [{
        xtype: 'button',
        itemId: 'copyClipboard',
        baseCls: 'fa fa-copy',
        margin: '5 0 0 5',
        tooltip: 'Copy to Clipboard'.t(),
        handler: null
    }],

    constructor: function(config) {
        var me = this;

        // Attach our local handler to the copy button
        me.items.forEach(function(item){
            if(item.itemId == 'copyClipboard' && 
               item.handler == null){
                item.handler = me.copy;
            }
        });

        if(config.stripPrefix != null){
            me.stripPrefix = new RegExp('^' + config.stripPrefix);
        }
        if(config.stripSuffix != null){
            me.stripSuffix = new RegExp(config.stripSuffix + '$');
        }
        if(config.targetKey){
            me.targetKey = config.targetKey;
        }

        if(config.items){
            var buttonExists = false;
            config.items.forEach(function(item){
                if(item.itemId == 'copyClipboard'){
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
    },

    /**
     * Perform copy to clipboard
     * @param {*} button 
     */
    copy: function(button)
    {
        var me = this,
            copyComponent = button.up(),
            valueElement = copyComponent.down("[xtype!=button]"),
            el = document.createElement('textarea');

        el.value = valueElement[copyComponent.targetKey];

        if(copyComponent.stripPrefix != null){
            el.value = el.value.replace( copyComponent.stripPrefix, '' );
        }
        if(copyComponent.stripSuffix != null){
            el.value = el.value.replace( copyComponent.stripSuffix, '' );
        }

        el.setAttribute('readonly', '');
        el.style.position = 'absolute';
        el.style.left = '-9999px';
        document.body.appendChild(el);
        el.select();
        // this executes the actual copy
        document.execCommand('copy');
        // remove the textarea helper
        document.body.removeChild(el);

    }
});
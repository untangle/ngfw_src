Ext.define('Ung.cmp.CopyToClipboard', {
    extend: 'Ext.form.FieldContainer',
    alias: 'widget.copytoclipboard',

    key: {},
    value: {
        key: 'value'
    },

    layout: {
        type: 'hbox'
    },
    dataType: 'text',

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
    copy: function(button){
        var me = this,
            copyComponent = button.up(),
            sourceElements = copyComponent.query("[xtype!=button]"),
            el = document.createElement('textarea'),
            elementValue = (copyComponent.dataType == 'javascript') ? {} : '';

        sourceElements.forEach( function(sourceEl){
            if(sourceEl.xtype != copyComponent.xtype){
                var value = sourceEl[copyComponent.value.key];
                if(value != undefined){
                    var key = "";
                    if(copyComponent.key.key != null){
                        key = sourceEl[copyComponent.key.key];
                    }
                    if(typeof(value) == "string"){
                        if(copyComponent.value.stripPrefix != null){
                            value = value.replace( copyComponent.value.stripPrefix, '' );
                        }
                        if(copyComponent.value.stripSuffix != null){
                            value = value.replace( copyComponent.value.stripSuffix, '' );
                        }
                    }
                    if(copyComponent.dataType == 'javascript'){
                        elementValue[key] = value;
                    }else{
                        elementValue += (el.value.length ? "\n" : "") + (key ? key + "=" : '') + value;
                    }
                }
            }
        });

        el.value = (copyComponent.dataType == 'javascript') ? JSON.stringify(elementValue) : elementValue;

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
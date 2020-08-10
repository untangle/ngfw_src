Ext.define('Ung.cmp.CopyToClipboard', {
    extend: 'Ext.form.FieldContainer',
    alias: 'widget.copytoclipboard',

    key: {},
    value: {
        key: 'value',
    },

    layout: {
        type: 'hbox'
    },
    dataType: 'text',
    below: 'false', //If the items to copy are embedded a level below the component with copytoclipboard type

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

            //Determine if items to copy are embedded below
            var itemsToLoop = config.items;
            if (config.below == 'true') itemsToLoop = config.items[0].items;

            //Determine if button already exists in configuration
            itemsToLoop.forEach(function(item){
                if(item.itemId == 'copyClipboard'){
                    buttonExists = true;
                }
            });

            //If button does not exist, add the button to the configuration
            if(buttonExists == false){
                me.items.forEach(function(item){
                    itemsToLoop.push(item);
                });    
            }

            //If items to copy are embedded below copytoclipboard component, set the properties appropriately
            if (config.below && config.below == 'true') {
                if (config.dataType) config.items[0].dataType = config.dataType;
                else config.items[0].dataType = me.dataType;

                if (config.layout) config.items[0].layout = config.layout;
                else config.items[0].layout = me.layout; 

                if (config.value) config.items[0].value = config.value;
                else config.items[0].value = me.value;
                
                if (config.key) config.items[0].key = config.key;
                else config.items[0].key = me.key;
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
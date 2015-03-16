/* TODO extjs5
Ext.Loader.loadScriptFileInitial=Ext.Loader.loadScriptFile;
Ext.Loader.loadScriptFile=Ext.bind(function() {
    var args = arguments;
    args[0]=arguments[0]+"?_dc="+Ext.buildStamp;
    Ext.Loader.loadScriptFileInitial.apply(this, args);
}, Ext.Loader);
*/
Ext.override(Ext.form.field.Base, {
    msgTarget: 'side',
    clearDirty: function() {
        if(this.xtype=='radiogroup') {
            this.items.each(function(item) {
                item.clearDirty();
            });
        } else {
            this.originalValue=this.getValue();
        }
    },
    afterRender: Ext.Function.createSequence(Ext.form.Field.prototype.afterRender,function() {
        if (this.tooltip) {
            var target = null;
            try {
                if(this.xtype=='checkbox') {
                    target = this.labelEl;
                } else {
                    target = this.container.dom.parentNode.childNodes[0];
                }
            } catch(exn) {
                //don't bother if there's nothing to target
            }

            if (target) {
                Ext.QuickTips.register({
                    target: target,
                    title: '',
                    text: this.tooltip,
                    enabled: true,
                    showDelay: 20
                });
            }
        }
    })
});

Ext.override( Ext.form.FieldSet, {
    border: 0
});

Ext.override(Ext.grid.column.Column, {
    defaultRenderer: Ext.util.Format.htmlEncode
});
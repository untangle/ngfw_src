if(typeof console === "undefined") {
    //Prevent console.log triggering errors on browsers without console support
    var console = {
        log: function() {},
        error: function() {},
        debug: function() {}
    };
}

if (typeof String.prototype.trim !== "function") {
    // implement trim for browsers like IceWeasel 3.0.6
    String.prototype.trim = function () {
        return this.replace(/^\s\s*/, '').replace(/\s\s*$/, '');
    };
}

Ext.override(Ext.MessageBox, {
    alert: function() {
        this.callParent(arguments);
        //Hack to solve the issue with alert being displayed behind the current settings window after a jabsorb call.
        Ext.defer(this.toFront, 10, this);
    }
});

Ext.override(Ext.Button, {
    listeners: {
        "afterrender": {
            fn: function() {
                if (this.name && this.getEl()) {
                    this.getEl().set({
                        'name': this.name
                    });
                }
            }
        }
    }
});

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
        Ext.QuickTips.init();
        var qt = this.tooltip;
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

        if (qt && target) {
            Ext.QuickTips.register({
                target: target,
                title: '',
                text: qt,
                enabled: true,
                showDelay: 20
            });
        }
    })
});

Ext.override(Ext.panel.Panel, {
    listeners: {
        "afterrender": {
            fn: function() {
                if (this.name && this.getEl()) {
                    this.getEl().set({
                        'name': this.name + " Content"
                    });
                }
            }
        }
    }
});

Ext.override(Ext.Toolbar, {
    nextBlock: function() {
        var td = document.createElement("td");
        if (this.columns && (this.tr.cells.length == this.columns)) {
            this.tr = document.createElement("tr");
            var tbody = this.el.down("tbody", true);
            tbody.appendChild(this.tr);
        }
        this.tr.appendChild(td);
        return td;
    },
    insertButton: Ext.Function.createSequence(function() {
        if (this.columns) {
            throw "This method won't work with multiple rows";
        }
    }, Ext.Toolbar.prototype.insertButton)
});

Ext.override(Ext.PagingToolbar, {
    listeners: {
        "afterrender": {
            fn: function() {
                if (this.getEl()) {
                    this.getEl().set({
                        'name': "Paging Toolbar"
                    });
                }
            }
        }
    }
});

Ext.override( Ext.form.FieldSet, {
    border: 0
});

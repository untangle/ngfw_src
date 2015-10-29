//RuleBuilder
Ext.define('Ung.RuleBuilder', {
    extend: 'Ext.panel.Panel',
    settingsCmp: null,
    layout: { type: 'vbox'},
    dirtyFlag: false,
    alias: 'widget.rulebuilder',
    javaClass: null,
    initComponent: function() {
        if (this.conditions)
            this.conditionsMap=Ung.Util.createRecordsMap(this.conditions, 'name');
        this.visibleFilter = new Ext.util.Filter({
            property: 'visible',
            value: true
        });
        this.conditionTypeStore = Ext.create('Ext.data.Store', {
            filters: [this.visibleFilter],
            fields: ["name", "displayName"],
            data: [{name: "", displayName: ""}].concat(this.conditions)
        });
        this.invertStore = Ext.create('Ext.data.Store', {
            fields: ["name", "displayName"],
            data: [{name: false, displayName: i18n._("is")}, {name: true, displayName: i18n._("is NOT")}]
        });
        this.items = [{
            xtype: 'container',
            layout: 'column',
            width: '100%',
            items: [{
                title: i18n._("Type"),
                width: 412
            }, {
                title: i18n._("Value"),
                columnWidth: 1
            }]
        }];
        this.tbar = [{
            iconCls: 'icon-add-row',
            text: i18n._("Add"),
            handler: this.addHandler,
            scope: this
        }];
        this.callParent(arguments);
    },
    generateRow: function(data, disableInvert) {
        if(!data) {
            data = {conditionType: "", invert: false, value: ""};
        }
        return {
            xtype: 'container',
            layout: 'column',
            name: 'rule',
            width: '100%',
            defaults: {
                margin: 3
            },
            items: [{
                xtype: 'combo',
                width: 300,
                ruleDataIndex: "conditionType",
                editable: false,
                valueField: "name",
                displayField: "displayName",
                queryMode: 'local',
                store: this.conditionTypeStore,
                value: data.conditionType,
                listeners: {
                    change: {
                        fn: function(combo, newValue, oldValue, opts) {
                            this.conditionTypeChangeHandler(combo, newValue);
                        },
                        scope: this
                    }
                }
            }, {
                xtype: 'combo',
                width: 100,
                ruleDataIndex: "invert",
                editable: false,
                valueField: "name",
                displayField: "displayName",
                queryMode: 'local',
                value: data.invert,
                readOnly: disableInvert,
                store: this.invertStore
            }, {
                xtype: 'container',
                ruleDataIndex: "value",
                layout: 'column',
                columnWidth: 1,
                items: this.buildValueItems(data)
            },
            {
                xtype: 'button',
                name: "delete",
                text: i18n._("Delete"),
                handler: Ext.bind(function(button) {
                    this.remove(button.up("container"));
                }, this)
            }]
        };
    },
    conditionTypeChangeHandler: function(combo, newValue) {
        this.dirtyFlag=true;
        var ruleContainer = combo.up("container");
        var invertCombo = ruleContainer.down("[ruleDataIndex=invert]");
        var valueContainer = ruleContainer.down("[ruleDataIndex=value]");
        invertCombo.setReadOnly(false);
        valueContainer.removeAll();
        
        if (Ext.isEmpty(newValue)) {
            return;
        }
        var rule = this.conditionsMap[newValue];
        if (!rule) {
            return;
        }
        if(!rule.allowMultiple) {
            var isDuplicate = false;
            Ext.Array.each(this.query("container[name=rule]"), function(item, index, len) {
                var conditionTypeCombo=item.down("[ruleDataIndex=conditionType]");
                if(conditionTypeCombo!=combo && newValue==conditionTypeCombo.getValue()) {
                    Ext.MessageBox.alert(i18n._("Warning"),i18n._("A condition of this type already exists in this rule."));
                    combo.setValue("");
                    isDuplicate = true;
                    return false;
                }
                
            });
            if(isDuplicate) {
                return;
            }
        }
        if(rule.disableInvert) {
            invertCombo.setValue(false);
            invertCombo.setReadOnly(true);
        }
        valueContainer.add(this.buildValueItems({conditionType: newValue, invert: invertCombo.getValue(), value: ""}));
    },
    buildValueItems: function(data) {
        var items= [], me = this;
        var rule=this.conditionsMap[data.conditionType];
        if (!rule) {
            return items;
        }
        switch (rule.type) {
        case "text":
            items=[{
                xtype : 'textfield',
                width : '100%',
                value : data.value,
                vtype : rule.vtype,
                allowBlank: rule.allowBlank === true 
            }];
            break;
        case "boolean":
            items=[{
                xtype : 'component',
                html : i18n._("True"),
                margin: '3 0 0 0'
            }];
            break;
        case "editor":
            var buildButtonText = function(value) {
                return Ext.isFunction(rule.formatValue) ? Ext.String.htmlEncode(rule.formatValue(value)) : Ext.String.htmlEncode(value);
            };
            items=[{
                xtype : 'button',
                name : "delete",
                width : '100%',
                text : buildButtonText(data.value),
                textAlign: 'left',
                dataValue: data.value,
                getValue: function() {
                    return this.dataValue;
                },
                setValue: function( value) {
                    this.dataValue = value;
                    this.setText(buildButtonText(value));
                    me.dirtyFlag = true;
                },
                handler : Ext.bind(function(button) {
                    if (rule.editor == null) {
                        Ext.MessageBox.alert(i18n._("Warning"), i18n._("Missing Editor"));
                        return;
                    }
    
                    rule.editor.populate(button);
                    rule.editor.show();
                }, this)
            }];
            break;
        case "checkgroup":
            var values_arr = (data.value != null && data.value.length > 0) ? data.value.split(",") : [];
            var checkboxes = [];
            for ( var count = 0; count < rule.values.length; count++) {
                items.push({
                    xtype : 'checkbox',
                    margin : '0 20 0 0',
                    inputValue : rule.values[count][0],
                    boxLabel : rule.values[count][1],
                    checked : values_arr.indexOf(rule.values[count][0]+"") != -1
                });
            }
            break;
        }
        return items;
    },
    getRuleValue: function(item) {
        var value= "";
        var rule=this.conditionsMap[item.down("[ruleDataIndex=conditionType]").getValue()];
        if (!rule) {
            return value;
        }
        var valueContainer = item.down("[ruleDataIndex=value]");
        switch (rule.type) {
        case "text":
            value = valueContainer.down("textfield").getValue();
            break;
        case "boolean":
            value = "true";
            break;
        case "editor":
            value = valueContainer.down("button").getValue();
            break;
        case "checkgroup":
            var checked=[];
            Ext.Array.each(valueContainer.query("checkbox"), function(item, index, len) {
                if(item.getValue()) {
                    checked.push(item.inputValue);
                }
            });
            value = checked.join(",");
            break;
        }
        return value;
    },
    addHandler: function() {
        this.add(this.generateRow());
    },
    setValue: function(value) {
        var me = this, data, rule, record, disableInvert;
        Ext.Array.each(this.query("container[name=rule]"), function(item, index, len) {
            me.remove(item);
        });
        if (value != null && value.list != null) {
            for(var i=0; i<value.list.length; i++) {
                rule=this.conditionsMap[value.list[i].conditionType];
                disableInvert = rule && rule.disableInvert;
                //if the condition is hidden and ther is a value for it make it visible
                if (rule && rule.visible === false) {
                    this.conditionTypeStore.clearFilter();
                    record = this.conditionTypeStore.findRecord("name", value.list[i].conditionType, 0, false, false, true);
                    if(record) {
                        record.set("visible", true);
                    }
                    this.conditionTypeStore.setFilters(this.visibleFilter);
                }
                this.add(this.generateRow(value.list[i], disableInvert));
            }
        }
    },
    getValue: function() {
        var list=[], conditionType, me = this;
        
        Ext.Array.each(this.query("container[name=rule]"), function(item, index, len) {
            conditionType = item.down("[ruleDataIndex=conditionType]").getValue();
            if(!Ext.isEmpty(conditionType)) {
                list.push({
                    javaClass: me.javaClass,
                    conditionType: conditionType,
                    invert: item.down("[ruleDataIndex=invert]").getValue(),
                    value: me.getRuleValue(item)
                });
            }
        });
        
        return {
            javaClass: "java.util.LinkedList",
            list: list,
            //must override toString in order for all objects not to appear the same
            toString: function() {
                return Ext.encode(this);
            }
        };
    },
    getName: function() {
        return "rulebuilder";
    },
    beforeDestroy: function() {
        for (var i = 0; i < this.conditions.length; i++) {
            if (this.conditions[i].editor !=null ) {
                Ext.destroy(this.conditions[i].editor);
            }
        }
        Ext.destroy(this.visibleFilter);
        this.callParent(arguments);
    },
    isValid: function() {
        var valuesList = this.getValue().list;
        var rule, i, val;
        for(i=0; i<valuesList.length; i++) {
            val = valuesList[i];
            rule=this.conditionsMap[val.conditionType];
            if(rule && rule.type=='text') {
                if(Ext.isEmpty(val.value)) {
                    if(rule.allowBlank!==true) {
                        if(rule.vtype =='portMatcher') {
                            return "<b>"+rule.displayName + "</b>: " + Ext.form.field.VTypes.portMatcherText;
                        } else if(rule.vtype =='ipMatcher') {
                            return "<b>"+rule.displayName + "</b>: " + Ext.form.field.VTypes.ipMatcherText;
                        } else {
                            return "<b>"+rule.displayName + "</b>: " + i18n._("Value is required.");
                        }
                    }
                }
            }
        }
        // verify that if DST_PORT or SRC_PORT is specified
        // that if protocol is specified it must be TCP and/or UDP only
        // other protocols do not support ports
        var portRulesFound = false;
        for( i=0 ; i<valuesList.length ; i++ ) {
            val = valuesList[i];
            if ( val.conditionType == "DST_PORT" || val.conditionType == "SRC_PORT" )
                portRulesFound = true;
        }
        if ( portRulesFound ) {
            for( i=0 ; i<valuesList.length ; i++ ) {
                val = valuesList[i];
                if ( val.conditionType == "PROTOCOL" ) {
                    var value = val.value;
                    value = value.replace("TCP","").replace("UDP","").replace("tcp","").replace("udp","");
                    var testvalue = value.replace(",","");
                    // testvalues should "" for valid rules
                    if ( testvalue != "" ) {
                        return i18n._("Can not specify a port condition on port-less protocols: ") + value.split(",").join(" ");
                    }
                }
            }
        }
        return true;
    },
    isDirty: function() {
        return this.dirtyFlag;
    },
    clearDirty: function() {
        this.dirtyFlag = false;
    }
});

//RuleBuilder
Ext.define('Ung.RuleBuilder', {
    extend: 'Ext.grid.Panel',
    settingsCmp: null,
    enableColumnHide: false,
    enableColumnMove: false,
    dirtyFlag: false,
    alias: 'widget.rulebuilder',
    javaClass: null,
    viewConfig: {
        enableTextSelection: true
    },
    initComponent: function() {
        Ext.applyIf(this, {
            height: 220
        });
        this.selModel= Ext.create('Ext.selection.Model',{});
        this.tbar = [{
            iconCls: 'icon-add-row',
            text: this.settingsCmp.i18n._("Add"),
            handler: this.addHandler,
            scope: this
        }];
        this.modelName='Ung.RuleBuilder.Model-' + this.getId();
        Ext.define(this.modelName, {
            extend: 'Ext.data.Model',
            identifier: 'sequential',
            fields: [{name: 'name'},{name: 'invert'},{name: 'value'},{name:'vtype'}]
        });
        this.store = Ext.create('Ext.data.Store', { model:this.modelName});
        this.matchersMap=Ung.Util.createRecordsMap(this.matchers, 'name');
        this.recordDefaults={name:"", value:"", vtype:""};
        var deleteColumn = Ext.create('Ung.grid.DeleteColumn',{});
        this.plugins=[deleteColumn];
        this.columns=[{
            align: "center",
            header: "",
            width: 45,
            resizable: false,
            menuDisabled: true,
            dataIndex: null,
            renderer: Ext.bind(function(value, metadata, record, rowIndex) {
                if (rowIndex == 0) return "";
                return this.settingsCmp.i18n._("and");
            }, this)
        },{
            header: this.settingsCmp.i18n._("Type"),
            width: 305,
            sortable: false,
            menuDisabled: true,
            dataIndex: "name",
            renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store) {
                var out=[];
                out.push('<select class="rule_builder_type" onchange="Ext.getCmp(\''+this.getId()+'\').changeRowType(\''+record.getId()+'\', this)">');
                out.push('<option value=""></option>');

                for (var i = 0; i < this.matchers.length; i++) {
                    var selected = this.matchers[i].name == value;
                    var seleStr=(selected)?"selected":"";
                    // if this select is invisible and not already selected (dont show it)
                    // if it is selected and invisible, show it (we dont have a choice)
                    if (!this.matchers[i].visible && !selected)
                        continue;
                    out.push('<option value="' + this.matchers[i].name + '" ' + seleStr + '>' + this.matchers[i].displayName + '</option>');
                }
                out.push("</select>");
                return out.join("");
            }, this)
        },{
            header: "",
            width: 90,
            sortable: false,
            resizable: false,
            menuDisabled: true,
            dataIndex: "invert",
            renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store) {
                var rule=this.matchersMap[record.get("name")];
                var out=[];
                out.push('<select class="rule_builder_invert" onchange="Ext.getCmp(\''+this.getId()+'\').changeRowInvert(\''+record.getId()+'\', this)">');
                out.push('<option value="false" ' + ((value==false)?"selected":"") + '>' + this.settingsCmp.i18n._("is")     + '</option>');
                if (rule == null || rule.allowInvert == null || rule.allowInvert == true)
                    out.push('<option value="true"  ' + ((value==true) ?"selected":"") + '>' + this.settingsCmp.i18n._("is NOT") + '</option>');
                out.push("</select>");
                return out.join("");
            }, this)
        },{
            header: this.settingsCmp.i18n._("Value"),
            width: 315,
            flex: 1,
            sortable: false,
            resizable: false,
            menuDisabled: true,
            dataIndex: "value",
            renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store, view) {
                var name=record.get("name");
                value=record.get("value");
                var rule=this.matchersMap[name];
                var res="";
                if (!rule) {
                    return "";
                }
                
                switch(rule.type) {
                  case "text":
                    res='<input type="text" class="row-editor-textfield" onchange1="Ext.getCmp(\''+this.getId()+'\').changeRowValue(\''+record.getId()+'\', this)" value="'+value+'"/>';
                    break;
                  case "boolean":
                    res="<div>" + this.settingsCmp.i18n._("True") + "</div>";
                    break;
                  case "editor":
                    var displayValue= Ext.isFunction(rule.formatValue) ? Ext.String.htmlEncode(rule.formatValue(value)) : Ext.String.htmlEncode(value);
                    res='<input type="text" class="row-editor-textfield" onclick="Ext.getCmp(\''+this.getId()+'\').openRowEditor(\''+record.getId()+'\', \''+rule.editor.getId()+'\', this)" onchange="Ext.getCmp(\''+this.getId()+'\').changeRowValue(\''+record.getId()+'\', this)" value="'+displayValue+'"/>';
                    break;
                  case "checkgroup":
                    var values_arr=(value!=null && value.length>0)?value.split(","):[];
                    var out=[];
                    for(var count=0; count<rule.values.length; count++) {
                        var rule_value=rule.values[count][0];
                        var rule_label=rule.values[count][1];
                        var checked_str="";
                        for(var j=0;j<values_arr.length; j++) {
                            if(values_arr[j]==rule_value) {
                                checked_str="checked";
                                break;
                            }
                        }
                        out.push('<div class="checkbox" style="width:100px; float: left; padding:3px 0;">');
                        out.push('<input id="'+rule_value+'[]" class="rule_builder_checkbox" '+checked_str+' onchange="Ext.getCmp(\''+this.getId()+'\').changeRowValue(\''+record.getId()+'\', this)" style="display:inline; float:left;margin:0;" name="'+rule_label+'" value="'+rule_value+'" type="checkbox">');
                            out.push('<label for="'+rule_value+'[]" style="display:inline;float:left;margin:0 0 0 0.6em;padding:0;text-align:left;width:50%;">'+rule_label+'</label>');
                        out.push('</div>');
                    }
                    res=out.join("");
                    break;
                }
                return res;
            }, this)
        }, deleteColumn];
        this.callParent(arguments);
    },
    openRowEditor: function(recordId,editorId,valObj) {
        var record=this.store.getById(recordId);
        var editor=Ext.getCmp(editorId);
        if (editor == null) {
            Ext.MessageBox.alert(i18n._("Warning"),i18n._("Missing Editor"));
            return;
        }

        editor.populate(record, record.get("value"), this);
        editor.show();
    },
    changeRowType: function(recordId,selObj) {
        var record=this.store.getById(recordId);
        var newName=selObj.options[selObj.selectedIndex].value;
        if (newName == "") {
            Ext.MessageBox.alert(i18n._("Warning"),i18n._("A valid type must be selected."));
            return;
        }
        // find the selected matcher
        var rule=this.matchersMap[newName];
        var i;
        // iterate through and make sure there are no other matchers of this type
        if(!rule.allowMultiple) {
            for (i = 0; i < this.store.data.length ; i++) {
                if (this.store.data.items[i].data.id == recordId)
                    continue;
                if (this.store.data.items[i].data.name == newName) {
                    Ext.MessageBox.alert(i18n._("Warning"),i18n._("A matcher of this type already exists in this rule."));
                    record.set("name","");
                    selObj.value = "";
                    return;
                }
            }
        }
        var newValue="";
        if(rule.type=="boolean") {
            newValue="true";
        }
        selObj.value.vtype=rule.vtype;
        record.set({
            "vtype": rule.vtype,
            "value": newValue,
            "name": newName
        });
        this.getView().refresh();
        this.dirtyFlag=true;
    },
    changeRowInvert: function(recordId,selObj) {
        var record=this.store.getById(recordId);
        var newValue=selObj.options[selObj.selectedIndex].value;
        record.set("invert", newValue);
        this.dirtyFlag=true;
    },
    changeRowValue: function(recordId,valObj) {
        var record=this.store.getById(recordId);
        switch(valObj.type) {
          case "checkbox":
            var record_value=record.get("value");
            var values_arr=(record_value!=null && record_value.length>0)?record_value.split(","):[];
            if(valObj.checked) {
                values_arr.push(valObj.value);
            } else {
                for(var i=0;i<values_arr.length;i++) {
                    if(values_arr[i]==valObj.value) {
                        values_arr.splice(i,1);
                        break;
                    }
                }
            }
            record.set("value",values_arr.join(","));
            break;
          case "text":
            var new_value=valObj.value;
            var vtype = record.get('vtype');
            if(!Ext.isEmpty(vtype)) {
                new_value = new_value.replace(/ /g,"");
                if ( !Ext.form.field.VTypes[vtype](new_value)) {
                    valObj.value='';
                    valObj.select();
                    valObj.setAttribute('style','border:1px #C30000 solid');
                } else {
                    valObj.removeAttribute('style');
                    valObj.value=new_value;
                    record.set("value",new_value);
                }
            } else {
                record.set("value",new_value);
            }
            break;
        }
        this.dirtyFlag = true;
    },
    addHandler: function() {
        var record=Ext.create(this.modelName,Ext.decode(Ext.encode(this.recordDefaults)));
        this.getStore().add([record]);
    },
    deleteHandler: function (record) {
        this.store.remove(record);
    },
    setValue: function(value) {
        this.dirtyFlag=false;
        var entries=[];
        var rule;
        if (value != null && value.list != null) {
            for(var i=0; i<value.list.length; i++) {
                if ( value.list[i].vtype == undefined) {
                    // get the vtype for the current value
                    rule=this.matchersMap[value.list[i].matcherType];
                    if(rule) {
                        value.list[i].vtype=rule.vtype;
                    }
                }
                entries.push( [value.list[i].matcherType, value.list[i].invert, value.list[i].value, value.list[i].vtype] );
            }
        }
        this.store.loadData(entries);
    },
    getValue: function() {
        var list=[];
        var records=this.store.getRange();
        for(var i=0; i<records.length;i++) {
            list.push({
                javaClass: this.javaClass,
                matcherType: records[i].get("name"),
                invert: records[i].get("invert"),
                value: records[i].get("value"),
                vtype: records[i].get("vtype")});
        }
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
        for (var i = 0; i < this.matchers.length; i++) {
            if (this.matchers[i].editor !=null ) {
                Ext.destroy(this.matchers[i].editor);
            }
        }
        this.callParent(arguments);
    },
    isValid: function() {
        // check that all the matchers have a selected type and value
        var records=this.store.getRange();
        var rule;
        var record;
        var i;
        for( i=0 ; i<records.length ; i++ ) {
            record = records[i];
            if(Ext.isEmpty(record.get("name"))) {
                return i18n._("A valid type must be selected for all matchers.");
            } else {
                rule=this.matchersMap[record.get("name")];
                if(rule.type=='text') {
                    if(Ext.isEmpty(record.get("value"))) {
                        if(rule.allowBlank!==true) {
                            if(record.get("vtype")=='portMatcher') {
                                return "<b>"+rule.displayName + "</b>: " + Ext.form.field.VTypes.portMatcherText;
                            } else if(record.get("vtype")=='ipMatcher') {
                                return "<b>"+rule.displayName + "</b>: " + Ext.form.field.VTypes.ipMatcherText;
                            } else {
                                return "<b>"+rule.displayName + "</b>: " + i18n._("Value is required.");
                            }
                        }
                    }
                }
            }
        }
        // verify that if DST_PORT or SRC_PORT is specified
        // that if protocol is specified it must be TCP and/or UDP only
        // other protocols do not support ports
        var portRulesFound = false;
        for( i=0 ; i<records.length ; i++ ) {
            record = records[i];
            if ( record.get("name") == "DST_PORT" || record.get("name") == "SRC_PORT" )
                portRulesFound = true;
        }
        if ( portRulesFound ) {
            for( i=0 ; i<records.length ; i++ ) {
                record = records[i];
                if ( record.get("name") == "PROTOCOL" ) {
                    var value = record.get("value");
                    value = value.replace("TCP","").replace("UDP","");
                    value = value.replace("tcp","").replace("udp","");
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
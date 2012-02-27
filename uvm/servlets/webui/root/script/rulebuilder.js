// RuleBuilder
Ext.define('Ung.RuleBuilder', {
    extend:'Ext.grid.Panel',
    settingsCmp: null,
    enableHdMenu : false,
    enableColumnMove: false,
    alias:'widget.rulebuilder',
    javaClass:null,
    

    initComponent: function() {
        Ext.applyIf(this,{
            height:220,
            width:600,
            anchor:"98%"
        });
        this.selModel= Ext.create('Ext.selection.Model',{});;
        this.tbar = [{
            iconCls : 'icon-add-row',
            text : this.settingsCmp.i18n._("Add"),
            handler : this.addHandler,
            scope : this
        }];
        
        this.modelName='Ung.RuleBuilder.Model-' + this.id;
        if ( Ext.ModelManager.get(this.modelName) == null) {
            Ext.define(this.modelName, {
                extend: 'Ext.data.Model',
                requires: ['Ext.data.SequentialIdGenerator'],
                idgen: 'sequential',
                fields: [{name: 'name'},{name: 'invert'},{name: 'value'}]
            });
        }
        this.store = Ext.create('Ext.data.Store', { model:this.modelName});

      
        this.recordDefaults={name:"", value:""};
        var deleteColumn = Ext.create('Ung.grid.DeleteColumn',{});
        this.plugins=[deleteColumn];
        this.columns=[{
            align: "center", 
            header: "",
            width:45,
            fixed: true,
            dataIndex: null,
            renderer: Ext.bind(function(value, metadata, record, rowIndex) {
                if (rowIndex == 0) return "";
                return this.settingsCmp.i18n._("and");
            },this)
        },{
            header : this.settingsCmp.i18n._("Type"),
            width: 320,
            fixed: true,
            dataIndex : "name",
            renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store) {
                var out=[];
                out.push('<select class="rule_builder_type" onchange="Ext.getCmp(\''+this.getId()+'\').changeRowType(\''+record.getId()+'\',this)">');
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
            },this)
        },{
            header : "",
            width: 100,
            fixed: true,
            dataIndex : "invert",
            renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store) {
                var out=[];
                out.push('<select class="rule_builder_invert" onchange="Ext.getCmp(\''+this.getId()+'\').changeRowInvert(\''+record.getId()+'\',this)">');
                out.push('<option value="false" ' + ((value==false)?"selected":"") + '>' + 'is'     + '</option>');
                out.push('<option value="true"  ' + ((value==true) ?"selected":"") + '>' + 'is NOT' + '</option>');
                out.push("</select>");
                return out.join("");
            },this)
        },{
            header : this.settingsCmp.i18n._("Value"),
            width: 315,
            flex:1,
            fixed: true,
            dataIndex : "value",
            renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store) {
                var name=record.get("name");
                value=record.data.value;
                var rule=null;
                for (var i = 0; i < this.matchers.length; i++) {
                    if (this.matchers[i].name == name) {
                        rule=this.matchers[i];
                        break;
                    }
                }
                var res="";
                if ( rule == null ) {
                    return "";
                }
                switch(rule.type) {
                  case "text":
                    res='<input type="text" size="20" class="x-form-text x-form-field rule_builder_value" onchange="Ext.getCmp(\''+this.getId()+'\').changeRowValue(\''+record.getId()+'\',this)" value="'+value+'"/>';
                    break;
                  case "boolean":
                    res="<div>" + this.settingsCmp.i18n._("True") + "</div>";
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
                        out.push('<input id="'+rule_value+'[]" class="rule_builder_checkbox" '+checked_str+' onchange="Ext.getCmp(\''+this.getId()+'\').changeRowValue(\''+record.getId()+'\',this)" style="display:inline; float:left;margin:0;" name="'+rule_label+'" value="'+rule_value+'" type="checkbox">');
                        out.push('<label for="'+rule_value+'[]" style="display:inline;float:left;margin:0 0 0 0.6em;padding:0;text-align:left;width:50%;">'+rule_label+'</label>');
                        out.push('</div>');
                    }
                    res=out.join("");
                    break;
                    
                }
                return res;

            },this)
        },deleteColumn];
        Ung.RuleBuilder.superclass.initComponent.apply( this, arguments );
    },
    changeRowType: function(recordId,selObj) {
        var record=this.store.getById(recordId);
        var newName=selObj.options[selObj.selectedIndex].value;
        var rule=null;
        if (newName == "") {
            Ext.MessageBox.alert(i18n._("Warning"),i18n._("A valid type must be selected."));
            return;
        }
        // iterate through and make sure there are no other matchers of this type
        for (var i = 0; i < this.store.data.length ; i++) {
            if (this.store.data.items[i].id == recordId)
                continue;
            if (this.store.data.items[i].data.name == newName) {
                Ext.MessageBox.alert(i18n._("Warning"),i18n._("A matcher of this type already exists in this rule."));
                record.set("name","");
                selObj.value = "";
                return;
            }
        }
        // find the selected matcher
        for (var i = 0; i < this.matchers.length; i++) {
            if (this.matchers[i].name == newName) {
                rule=this.matchers[i];
                break;
            }
        }
        var newValue="";
        if(rule.type=="boolean") {
            newValue="true";
        }
        record.data.value=newValue;
        record.set("name",newName);
        this.fireEvent("afteredit");
    },
    changeRowInvert: function(recordId,selObj) {
        var record=this.store.getById(recordId);
        var newValue=selObj.options[selObj.selectedIndex].value;
        record.data.invert = newValue;
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
            record.data.value=values_arr.join(",");
            break;
          case "text":
            var new_value=valObj.value;
            if(new_value!=null) {
                new_value.replace("::","");
                new_value.replace("&&","");
            }
            record.data.value=new_value;
            break;
        }
        this.fireEvent("afteredit");
    },
    addHandler: function() {
        var record=Ext.create(this.modelName,Ext.decode(Ext.encode(this.recordDefaults)));
        this.getStore().add([record]);
        this.fireEvent("afteredit");
    },
    deleteHandler: function (record) {
        this.store.remove(record);
        this.fireEvent("afteredit");
    },
    setValue: function(value) {
        var entries=[];
        if (value != null && value.list != null) {
            for(var i=0; i<value.list.length; i++) {
                entries.push( [value.list[i].matcherType, value.list[i].invert, value.list[i].value] );
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
                value: records[i].get("value")});
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
    isValid: function() {
        // check that all the matchers have a selected type and value
        for (var i = 0; i < this.store.data.length ; i++) {
            if (this.store.data.items[i].data.name == null || this.store.data.items[i].data.name == "") {
                Ext.MessageBox.alert(i18n._("Warning"),i18n._("A valid type must be selected for all matchers."));
                return false;
            }
            //if (this.store.data.items[i].data.value == null || this.store.data.items[i].data.value == "") {
            //    Ext.MessageBox.alert(i18n._("Warning"),i18n._("A valid value must be specified for all matchers."));
            //    return false;
            //}
        }
        return true;
    }
});

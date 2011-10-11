if (!Ung.hasResource["Ung.Firewall"]) {
    Ung.hasResource["Ung.Firewall"] = true;
    Ung.NodeWin.registerClassName('untangle-node-firewall', 'Ung.Firewall');

    Ung.FirewallUtil={
        getMatchers : function (settingsCmp) {
            return [
                {name:"DST_ADDR",displayName: settingsCmp.i18n._("Destination Address"), type: "text", visible: true},
                {name:"DST_PORT",displayName: settingsCmp.i18n._("Destination Port"), type: "text",vtype:"port", visible: true},
                {name:"DST_INTF",displayName: settingsCmp.i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
                {name:"SRC_ADDR",displayName: settingsCmp.i18n._("Source Address"), type: "text", visible: true},
                {name:"SRC_PORT",displayName: settingsCmp.i18n._("Source Port"), type: "text",vtype:"port", visible: false},
                {name:"SRC_INTF",displayName: settingsCmp.i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
                {name:"PROTOCOL",displayName: settingsCmp.i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["TCP,UDP","TCP,UDP"],["any","any"]], visible: true},
                {name:"DIRECTORY_CONNECTOR_USERNAME",displayName: settingsCmp.i18n._("Directory Connector: Username"), type: "text", visible: true},
                {name:"DIRECTORY_CONNECTOR_GROUP",displayName: settingsCmp.i18n._("Directory Connector: User in Group"), type: "text", visible: true}
            ];
        }
    };
    // FirewallRuleBuilder
    Ung.FirewallRuleBuilder = Ext.extend(Ext.grid.EditorGridPanel, {
        settingsCmp: null,
        enableHdMenu : false,
        enableColumnMove: false,
        
        clicksToEdit:1,

        initComponent: function() {
            Ext.applyIf(this,{
                height:220,
                width:600,
                anchor:"98%"
            });
            this.xtype="firewallrulebuilder";
            this.selModel= new Ext.grid.RowSelectionModel();
            this.tbar = [{
                iconCls : 'icon-add-row',
                text : this.settingsCmp.i18n._("Add"),
                handler : this.addHandler,
                scope : this
            }];

            this.store = new Ext.data.SimpleStore({
                fields: [
                    {name: 'name'},
                    {name: 'value'}
                ]
            });
            
            this.recordDefaults={name:this.rules[0].name, value:""};
            var deleteColumn = new Ext.grid.DeleteColumn({});
            this.autoExpandColumn = 'displayName',
            this.plugins=[deleteColumn];
            this.columns=[{
                align: "center", 
                header: "",
                width:45,
                fixed: true,
                dataIndex: null,
                renderer: function(value, metadata, record) {
                    return this.settingsCmp.i18n._("and");
                }.createDelegate(this)
            },{
                header : this.settingsCmp.i18n._("Type"),
                width: 300,
                fixed: true,
                dataIndex : "name",
                renderer: function(value, metadata, record, rowIndex, colIndex, store) {
                    var out=[];
                    out.push('<select class="rule_builder_type" onchange="Ext.getCmp(\''+this.getId()+'\').changeRowType(\''+record.id+'\',this)">');
                    for (var i = 0; i < this.rules.length; i++) {
                        var selected = this.rules[i].name == value;
                        var seleStr=(selected)?"selected":"";
                        // if this select is invisible and not already selected (dont show it)
                        // if it is selected and invisible, show it (we dont have a choice)
                        if (!this.rules[i].visible && !selected)
                            continue;
                        out.push('<option value="' + this.rules[i].name + '" ' + seleStr + '>' + this.rules[i].displayName + '</option>');
                    }
                    out.push("</select>");
                    return out.join("");
                }.createDelegate(this)
            },{
                id:'displayName',
                header : this.settingsCmp.i18n._("Value"),
                width: 315,
                fixed: true,
                dataIndex : "value",
                renderer: function(value, metadata, record, rowIndex, colIndex, store) {
                    var name=record.get("name");
                    value=record.data.value;
                    var rule=null;
                    for (var i = 0; i < this.rules.length; i++) {
                        if (this.rules[i].name == name) {
                            rule=this.rules[i];
                            break;
                        }
                    }
                    var res="";
                    if ( rule == null ) {
                        return "";
                    }
                    switch(rule.type) {
                      case "text":
                        res='<input type="text" size="20" class="x-form-text x-form-field rule_builder_value" onchange="Ext.getCmp(\''+this.getId()+'\').changeRowValue(\''+record.id+'\',this)" value="'+value+'"/>';
                        break;
                      case "boolean":
                        res="<div>&nbsp;</div>";
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
                            out.push('<input id="'+rule_value+'[]" class="rule_builder_checkbox" '+checked_str+' onchange="Ext.getCmp(\''+this.getId()+'\').changeRowValue(\''+record.id+'\',this)" style="display:inline; float:left;margin:0;" name="'+rule_label+'" value="'+rule_value+'" type="checkbox">');
                            out.push('<label for="'+rule_value+'[]" style="display:inline;float:left;margin:0 0 0 0.6em;padding:0;text-align:left;width:50%;">'+rule_label+'</label>');
                            out.push('</div>');
                        }
                        res=out.join("");
                        break;
                        
                    }
                    return res;

                }.createDelegate(this)
            },deleteColumn];
            Ung.FirewallRuleBuilder.superclass.initComponent.apply( this, arguments );
        },
        changeRowType: function(recordId,selObj) {
            var record=this.store.getById(recordId);
            var newName=selObj.options[selObj.selectedIndex].value;
            var rule=null;
            for (var i = 0; i < this.rules.length; i++) {
                if (this.rules[i].name == newName) {
                    rule=this.rules[i];
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
            var record=new Ext.data.Record(Ext.decode(Ext.encode(this.recordDefaults)));
            this.getStore().insert(0, [record]);
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
                    entries.push([value.list[i].matcherType,value.list[i].value]);
                }
            }
            this.store.loadData(entries);
        },
        getValue: function() {
            var list=[];
            var records=this.store.getRange();
            for(var i=0; i<records.length;i++) {
                list.push({
                    javaClass: "com.untangle.node.firewall.FirewallRuleMatcher",
                    matcherType: records[i].get("name"),
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
            return "firewallrulebuilder";
        },
        isValid: function() {
            //TODO: implement is valid
            return true;
        }
    });
    Ext.reg('firewallrulebuilder', Ung.FirewallRuleBuilder);
    
    Ung.Firewall = Ext.extend(Ung.NodeWin, {
        panelRules: null,
        gridRules : null,
        gridEventLog : null,
        initComponent : function() {
            //Ung.Util.clearInterfaceStore();
            Ung.Util.generateListIds(this.getSettings().rules.list);
            
            // builds the tabs
            this.buildRules();
            this.buildEventLog();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelRules, this.gridEventLog]);
            
            Ung.Firewall.superclass.initComponent.call(this);
        },
        // Rules Panel
        buildRules : function() {
            // enable is a check column
            var enabledColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("Enable"),
                dataIndex : 'enabled',
                fixed : true
            });
            var blockedColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("Block"),
                dataIndex : 'block',
                fixed : true
            });
            var logColumn = new Ext.grid.CheckColumn({
                header : this.i18n._("Log"),
                dataIndex : 'log',
                fixed : true
            });

            this.panelRules = new Ext.Panel({
                name : 'panelRules',
                helpSource : 'rules',
                // private fields
                gridRulesList : null,
                parentId : this.getId(),
                title : this.i18n._('Rules'),
                layout : 'anchor',
                defaults: {
                    anchor: '98%',
                    autoWidth: true,
                    autoScroll: true
                },
                autoScroll : true,
                border : false,
                cls: 'ung-panel',
                items : [{
                    title : this.i18n._('Note'),
                    cls: 'description',
                    bodyStyle : 'padding:5px 5px 5px; 5px;',
                    html : String.format(this.i18n._(" <b>Firewall</b> is a simple application designed to block and log network traffic based on a set of rules. To learn more click on the <b>Help</b> button below.<br/> Routing and Port Forwarding functionality can be found elsewhere in Config->Networking."),main.getBrandingManager().getCompanyName())
                },
                         this.gridRules= new Ung.EditorGrid({
                             name : 'Rules',
                             settingsCmp : this,
                             height : 500,
                             paginated : false,
                             hasReorder : true,
                             emptyRow : {
                                 "id" : 0,
                                 "enabled" : true,
                                 "block" : false,
                                 "log" : true,
                                 "description" : this.i18n._("[no description]"),
                                 "javaClass" : "com.untangle.node.firewall.FirewallRule"
                             },
                             title : this.i18n._("Rules"),
                             recordJavaClass : "com.untangle.node.firewall.FirewallRule",
                             data:this.getSettings().rules.list,
                             fields : [{
                                 name : 'id'
                             }, {
                                 name : 'enabled'
                             }, {
                                 name : 'block'
                             }, {
                                 name : 'log'
                             }, {
                                 name : 'matchers'
                             },{
                                 name : 'description'
                             }, {
                                 name : 'javaClass'
                             }],
                             columns : [{
                                 id : 'id',
                                 header : this.i18n._("Rule Id"),
                                 width : 50,
                                 dataIndex : 'id'
                             }, enabledColumn, {
                                 id : 'description',
                                 header : this.i18n._("Description"),
                                 width : 200,
                                 dataIndex : 'description'
                             }, logColumn, blockedColumn],
                             columnsDefaultSortable : false,
                             autoExpandColumn : 'description',
                             plugins : [enabledColumn, logColumn, blockedColumn],

                             initComponent : function() {
                                 this.rowEditor = new Ung.RowEditorWindow({
                                     grid : this,
                                     sizeToComponent : this.settingsCmp,
                                     inputLines : this.rowEditorInputLines,
                                     rowEditorLabelWidth : 100,
                                     populate : function(record, addMode) {
                                         return this.populateTree(record, addMode);
                                     },
                                     // updateAction is called to update the record after the edit
                                     updateAction : function() {
                                         return this.updateActionTree();
                                     },
                                     isDirty : function() {
                                         if (this.record !== null) {
                                             if (this.inputLines) {
                                                 for (var i = 0; i < this.inputLines.length; i++) {
                                                     var inputLine = this.inputLines[i];
                                                     if(inputLine.dataIndex!=null) {
                                                         if (this.record.get(inputLine.dataIndex) != inputLine.getValue()) {
                                                             return true;
                                                         }
                                                     }
                                                     /* for fieldsets */
                                                     if(inputLine.items !=null && inputLine.items.dataIndex != null) {
                                                         if (this.record.get(inputLine.items.dataIndex) != inputLine.items.getValue()) {
                                                             return true;
                                                         }
                                                     }
                                                 }
                                             }
                                         }
                                         return false;
                                     },
                                     isFormValid : function() {
                                         for (var i = 0; i < this.inputLines.length; i++) {
                                             var item = null;
                                             if ( this.inputLines.get != null ) {
                                                 item = this.inputLines.get(i);
                                             } else {
                                                 item = this.inputLines[i];
                                             }
                                             if ( item == null ) {
                                                 continue;
                                             }

                                             if ( item.isValid != null) {
                                                 if(!item.isValid()) {
                                                     return false;
                                                 }
                                             } else if(item.items !=null && item.items.getCount()>0) {
                                                 /* for fieldsets */
                                                 for (var j = 0; j < item.items.getCount(); j++) {
                                                     var subitem=item.items.get(j);
                                                     if ( subitem == null ) {
                                                         continue;
                                                     }

                                                     if ( subitem.isValid != null && !subitem.isValid()) {
                                                         return false;
                                                     }
                                                 }                                    
                                             }
                                             
                                         }
                                         return true;
                                     }
                                 });
                                 Ung.EditorGrid.prototype.initComponent.call(this);
                             },

                             rowEditorInputLines : [new Ext.form.Checkbox({
                                 name : "Enable Rule",
                                 dataIndex: "enabled",
                                 fieldLabel : this.i18n._("Enable Rule"),
                                 itemCls:'firewall-spacing-1'
                             }),
                                                    new Ext.form.TextField({
                                                        name : "Description",
                                                        dataIndex: "description",
                                                        fieldLabel : this.i18n._("Description"),
                                                        itemCls:'firewall-spacing-1',
                                                        width : 400
                                                    }),
                                                    new Ext.form.FieldSet({
                                                        title : this.i18n._("Rule") ,
                                                        cls:'firewall-spacing-2',
                                                        autoHeight : true,
                                                        title: "If all of the following conditions are met:",
                                                        items:[{
                                                            xtype:"firewallrulebuilder",
                                                            settingsCmp: this,
                                                            anchor:"98%",
                                                            width: 900,
                                                            dataIndex: "matchers",
                                                            rules : Ung.FirewallUtil.getMatchers(this)
                                                        }]
                                                    }), {
                                                        xtype : 'fieldset',
                                                        autoHeight: true,
                                                        cls:'description',
                                                        title : i18n._('Perform the following action(s):'),
                                                        border: false
                                                    }, new Ext.form.Checkbox({
                                                        name : "Log",
                                                        dataIndex: "log",
                                                        itemCls:'firewall-spacing-1',
                                                        fieldLabel : this.i18n._("Log")
                                                    }), new Ext.form.Checkbox({
                                                        name : "Block",
                                                        dataIndex: "block",
                                                        itemCls:'firewall-spacing-1',
                                                        fieldLabel : this.i18n._("Block")
                                                    })]
                         })]
            });
        },
        // Event Log
        buildEventLog : function() {
            this.gridEventLog = new Ung.GridEventLog({
                settingsCmp : this,
                fields : [{
                    name : 'id'
                }, {
                    name : 'timeStamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'blocked',
                    mapping : 'firewallWasBlocked'
                }, {
                    name : 'firewallRuleIndex'
                }, {
                    name : 'uid'
                }, {
                    name : 'client',
                    mapping : 'CClientAddr'
                }, {
                    name : 'server',
                    mapping : 'CServerAddr'
                }],
                columns : [{
                    header : this.i18n._("timestamp"),
                    width : 130,
                    sortable : true,
                    dataIndex : 'timeStamp',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header : this.i18n._("client"),
                    width : 165,
                    sortable : true,
                    dataIndex : 'client'
                }, {
                    header : this.i18n._("username"),
                    width : 165,
                    sortable : true,
                    dataIndex : 'uid'
                }, {
                    header : this.i18n._("blocked"),
                    width : 100,
                    sortable : true,
                    dataIndex : 'blocked'
                }, {
                    id: 'ruleIndex',
                    header : this.i18n._('rule'),
                    width : 150,
                    sortable : true,
                    dataIndex : 'firewallRuleIndex'
                }, {
                    header : this.i18n._("server"),
                    width : 165,
                    sortable : true,
                    dataIndex : 'server'
                }],
                autoExpandColumn: 'ruleIndex'

            });
        },

        //apply function 
        applyAction : function(){
            this.saveAction(true);
        },         
        // save function
        saveAction : function(keepWindowOpen) {
            Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
            this.gridRules.getGridSaveList(function(saveList) {
                this.getSettings().rules = saveList;
                this.getRpcNode().setSettings(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    // exit settings screen
                    if(!keepWindowOpen) {
                        Ext.MessageBox.hide();                    
                        this.closeWindow();
                    } else {
                        //refresh the settings
                        this.getRpcNode().getSettings(function(result,exception){
                            Ext.MessageBox.hide();
                            this.gridRules.reloadGrid({data:result.rules.list});
                        }.createDelegate(this));                       
                    }
                }.createDelegate(this), this.getSettings());
            }.createDelegate(this));                       
        },
        isDirty : function() {
            return this.gridRules.isDirty();
        }
    });
}

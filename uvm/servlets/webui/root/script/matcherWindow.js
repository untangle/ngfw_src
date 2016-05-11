// Base matcher pop-up editor window
Ext.define('Ung.MatcherEditorWindow', {
    extend:'Ung.EditWindow',
    height: 210,
    width: 120,
    inputLines: null, //override me
    initComponent: function() {
        if (this.title == null) {
            this.title = i18n._('Edit');
        }
        this.items = Ext.create('Ext.panel.Panel',{
            border: false,
            bodyStyle: 'padding:10px 10px 0px 10px;',
            autoScroll: true,
            defaults: {
                msgTarget: 'side'
            },
            items: this.inputLines
        });
        this.callParent(arguments);
    },
    onShow: function() {
        Ung.Window.superclass.onShow.call(this);
        this.setSize({width:this.width,height:this.height});
    },
    populate: function(button) {
        this.button = button;
        this.setValue(button.getValue());
    },
    updateAction: function() {
        this.button.setValue(this.getValue());
        this.hide();
    },
    cancelAction: function() {
        this.hide();
    },
    // set the value of fields (override me)
    setValue: function(value) {
        Ung.Util.todo();
    },
    // set the record based on the value of the fields (override me)
    getValue: function() {
        Ung.Util.todo();
    }
});

// matcher pop-up editor for time ranges
Ext.define('Ung.TimeEditorWindow', {
    extend:'Ung.MatcherEditorWindow',
    height: 250,
    width: 300,
    initComponent: function() {
        this.inputLines = [{
            xtype: 'radio',
            name: 'timeMethod',
            id: 'time_method_range_'+this.getId(),
            boxLabel: i18n._('Specify a Range'),
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, checked) {
                        if (checked) {
                            Ext.getCmp('start_time_hour_'+this.getId()).enable();
                            Ext.getCmp('start_time_minute_'+this.getId()).enable();
                            Ext.getCmp('end_time_hour_'+this.getId()).enable();
                            Ext.getCmp('end_time_minute_'+this.getId()).enable();
                            Ext.getCmp('time_custom_value_'+this.getId()).disable();
                        } else {
                            Ext.getCmp('start_time_hour_'+this.getId()).disable();
                            Ext.getCmp('start_time_minute_'+this.getId()).disable();
                            Ext.getCmp('end_time_hour_'+this.getId()).disable();
                            Ext.getCmp('end_time_minute_'+this.getId()).disable();
                            Ext.getCmp('time_custom_value_'+this.getId()).enable();
                        }
                    }, this)
                }
            }
        }, {
            xtype:'fieldset',
            name: 'Start Time',
            title: i18n._("Start Time"),
            fieldLabel: i18n._("Start Time - End Time"),
            layout: {
                type: 'hbox',
                align: 'middle'
            },
            items: [{
                xtype: 'combo',
                id: 'start_time_hour_'+this.getId(),
                editable: false,
                width: 40,
                allowBlank: false,
                store: [["00","00"], ["01","01"], ["02","02"], ["03","03"], ["04","04"], ["05","05"], ["06","06"], ["07","07"], ["08","08"], ["09","09"],
                        ["10","10"], ["11","11"], ["12","12"], ["13","13"], ["14","14"], ["15","15"], ["16","16"], ["17","17"], ["18","18"], ["19","19"],
                        ["20","20"], ["21","21"], ["22","22"], ["23","23"]]
            }, {
                xtype: 'component',
                margin: '0 3 0 3',
                html: ":"
            }, {
                xtype: 'combo',
                id: 'start_time_minute_'+this.getId(),
                editable: false,
                width: 40,
                allowBlank: false,
                store: [["00","00"], ["01","01"], ["02","02"], ["03","03"], ["04","04"], ["05","05"], ["06","06"], ["07","07"], ["08","08"], ["09","09"],
                        ["10","10"], ["11","11"], ["12","12"], ["13","13"], ["14","14"], ["15","15"], ["16","16"], ["17","17"], ["18","18"], ["19","19"],
                        ["20","20"], ["21","21"], ["22","22"], ["23","23"], ["24","24"], ["25","25"], ["26","26"], ["27","27"], ["28","28"], ["29","29"],
                        ["30","30"], ["31","31"], ["32","32"], ["33","33"], ["34","34"], ["35","35"], ["36","36"], ["37","37"], ["38","38"], ["39","39"],
                        ["40","40"], ["41","41"], ["42","42"], ["43","43"], ["44","44"], ["45","45"], ["46","46"], ["47","47"], ["48","48"], ["49","49"],
                        ["50","50"], ["51","51"], ["52","52"], ["53","53"], ["54","54"], ["55","55"], ["56","56"], ["57","57"], ["58","58"], ["59","59"]]
            }, {
                xtype: 'component',
                margin: '0 10 0 10',
                html: i18n._("to")
            }, {
                xtype: 'combo',
                id: 'end_time_hour_'+this.getId(),
                editable: false,
                width: 40,
                allowBlank: false,
                store: [["00","00"], ["01","01"], ["02","02"], ["03","03"], ["04","04"], ["05","05"], ["06","06"], ["07","07"], ["08","08"], ["09","09"],
                        ["10","10"], ["11","11"], ["12","12"], ["13","13"], ["14","14"], ["15","15"], ["16","16"], ["17","17"], ["18","18"], ["19","19"],
                        ["20","20"], ["21","21"], ["22","22"], ["23","23"]]
            }, {
                xtype: 'component',
                margin: '0 3 0 3',
                html: ":"
            }, {
                xtype: 'combo',
                id: 'end_time_minute_'+this.getId(),
                editable: false,
                width: 40,
                allowBlank: false,
                store: [["00","00"], ["01","01"], ["02","02"], ["03","03"], ["04","04"], ["05","05"], ["06","06"], ["07","07"], ["08","08"], ["09","09"],
                        ["10","10"], ["11","11"], ["12","12"], ["13","13"], ["14","14"], ["15","15"], ["16","16"], ["17","17"], ["18","18"], ["19","19"],
                        ["20","20"], ["21","21"], ["22","22"], ["23","23"], ["24","24"], ["25","25"], ["26","26"], ["27","27"], ["28","28"], ["29","29"],
                        ["30","30"], ["31","31"], ["32","32"], ["33","33"], ["34","34"], ["35","35"], ["36","36"], ["37","37"], ["38","38"], ["39","39"],
                        ["40","40"], ["41","41"], ["42","42"], ["43","43"], ["44","44"], ["45","45"], ["46","46"], ["47","47"], ["48","48"], ["49","49"],
                        ["50","50"], ["51","51"], ["52","52"], ["53","53"], ["54","54"], ["55","55"], ["56","56"], ["57","57"], ["58","58"], ["59","59"]]
            }]
        }, {
            xtype: 'radio',
            name: 'timeMethod',
            id: 'time_method_custom_'+this.getId(),
            boxLabel: i18n._('Specify a Custom Value'),
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, checked) {
                        if (!checked) {
                            Ext.getCmp('start_time_hour_'+this.getId()).enable();
                            Ext.getCmp('start_time_minute_'+this.getId()).enable();
                            Ext.getCmp('end_time_hour_'+this.getId()).enable();
                            Ext.getCmp('end_time_minute_'+this.getId()).enable();
                            Ext.getCmp('time_custom_value_'+this.getId()).disable();
                        } else {
                            Ext.getCmp('start_time_hour_'+this.getId()).disable();
                            Ext.getCmp('start_time_minute_'+this.getId()).disable();
                            Ext.getCmp('end_time_hour_'+this.getId()).disable();
                            Ext.getCmp('end_time_minute_'+this.getId()).disable();
                            Ext.getCmp('time_custom_value_'+this.getId()).enable();
                        }
                    }, this)
                }
            }
        }, {
            xtype:'textfield',
            id: 'time_custom_value_'+this.getId(),
            width: 250,
            allowBlank:false
        }];
        this.callParent(arguments);
    },
    setValue: function(value) {
        var time_method_custom = Ext.getCmp('time_method_custom_'+this.getId());
        var time_method_range = Ext.getCmp('time_method_range_'+this.getId());
        var start_time_hour = Ext.getCmp('start_time_hour_'+this.getId());
        var start_time_minute = Ext.getCmp('start_time_minute_'+this.getId());
        var end_time_hour = Ext.getCmp('end_time_hour_'+this.getId());
        var end_time_minute = Ext.getCmp('end_time_minute_'+this.getId());
        var time_custom_value = Ext.getCmp('time_custom_value_'+this.getId());
        start_time_hour.setValue('12');
        start_time_minute.setValue('00');
        end_time_hour.setValue('13');
        end_time_minute.setValue('30');
        time_method_custom.setValue(true);
        time_custom_value.setValue(value);

        /* if no value is specified - default to range with default range */
        if (value == "") {
            time_method_range.setValue(true);
            return;
        }
        var record_value = value;
        if (record_value == null)
            return;
        if (record_value.indexOf(",") != -1)
            return;
            var splits = record_value.split("-");
        if (splits.length != 2)
            return;

        var start_time = splits[0].split(":");
        var end_time = splits[1].split(":");

        if (start_time.length != 2)
            return;
        if (end_time.length != 2)
            return;

        start_time_hour.setValue(start_time[0]);
        start_time_minute.setValue(start_time[1]);
        end_time_hour.setValue(end_time[0]);
        end_time_minute.setValue(end_time[1]);
        time_method_range.setValue(true);
    },
    getValue: function() {
        var time_method_custom = Ext.getCmp('time_method_custom_'+this.getId());
        if (time_method_custom.getValue()) {
            var time_custom_value = Ext.getCmp('time_custom_value_'+this.getId());
            return time_custom_value.getValue();
        } else {
            var start_time_hour = Ext.getCmp('start_time_hour_'+this.getId());
            var start_time_minute = Ext.getCmp('start_time_minute_'+this.getId());
            var end_time_hour = Ext.getCmp('end_time_hour_'+this.getId());
            var end_time_minute = Ext.getCmp('end_time_minute_'+this.getId());
            return start_time_hour.getValue() + ":" + start_time_minute.getValue() + "-" + end_time_hour.getValue() + ":" + end_time_minute.getValue();
        }
    }
});

// matcher pop-up editor for time users
Ext.define('Ung.UserEditorWindow', {
    extend:'Ung.MatcherEditorWindow',
    height: 450,
    width: 550,
    initComponent: function() {
        var data = [];
        this.gridPanel = Ext.create('Ext.grid.Panel', {
            title: i18n._('Users'),
            id: 'usersGrid_'+this.getId(),
                height: 300,
            width: 400,
            enableColumnHide: false,
            enableColumnMove: false,
            store: new Ext.data.Store({
                data: data,
                sortOnLoad: true,
                sorters: { property: 'uid', direction : 'ASC' },
                fields: [{
                    name: "lastName"
                },{
                    name: "firstName"
                },{
                    name: "checked"
                },{
                    name: "uid"
                },{
                    name: "displayName",
                    convert: function(val, rec) {
                        if (val != null && val != "")
                            return val;
                        var displayName = ( rec.data.firstName == null )  ? "": rec.data.firstName;
                        displayName = displayName + " " + (( rec.data.lastName == null )  ? "": rec.data.lastName);
                        return displayName;
                    }
                }]
            }),
            columns: [ {
                header: i18n._("Selected"),
                width: 60,
                xtype:'checkcolumn',
                menuDisabled: true,
                sortable: false,
                dataIndex: "checked"
            }, {
                header: i18n._("Name"),
                width: 100,
                sortable: true,
                menuDisabled: true,
                dataIndex: "uid"
            },{
                header: i18n._("Full Name"),
                width: 200,
                sortable: true,
                menuDisabled: true,
                dataIndex: "displayName"
            }]
        });

        this.inputLines = [{
            xtype: 'radio',
            name: 'userMethod',
            boxLabel: i18n._('Specify Users'),
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, checked) {
                        if (checked) {
                            Ext.getCmp('usersGrid_'+this.getId()).enable();
                            Ext.getCmp('user_custom_value_'+this.getId()).disable();
                        } else {
                            Ext.getCmp('usersGrid_'+this.getId()).disable();
                            Ext.getCmp('user_custom_value_'+this.getId()).enable();
                        }
                    }, this)
                }
            }
        }, this.gridPanel, {
            xtype: 'radio',
            name: 'userMethod',
            id: 'user_method_custom_'+this.getId(),
            boxLabel: i18n._('Specify a Custom Value'),
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, checked) {
                        if (!checked) {
                            Ext.getCmp('usersGrid_'+this.getId()).enable();
                            Ext.getCmp('user_custom_value_'+this.getId()).disable();
                        } else {
                            Ext.getCmp('usersGrid_'+this.getId()).disable();
                            Ext.getCmp('user_custom_value_'+this.getId()).enable();
                        }
                    }, this)
                }
            }
        }, {
            xtype:'textfield',
            id: 'user_custom_value_'+this.getId(),
            width: 250,
            allowBlank:false
        }];
        this.callParent(arguments);
    },
    populate: function(button) {
        var data = [];
        var node;
        try {
            node = rpc.nodeManager.node("untangle-node-directory-connector");
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        if (node != null) {
            Ext.MessageBox.wait(i18n._("Loading..."), i18n._("Please wait"));
            data = node.getUserEntries().list;
            Ext.MessageBox.hide();
        } else {
            data.push({ firstName: "", lastName: null, uid: "[any]", displayName: "Any User"});
            data.push({ firstName: "", lastName: null, uid: "[authenticated]", displayName: "Any Authenticated User"});
            data.push({ firstName: "", lastName: null, uid: "[unauthenticated]", displayName: "Any Unauthenticated/Unidentified User"});
        }

        this.gridPanel.getStore().getProxy().data = data;
        this.gridPanel.getStore().load();
        this.callParent(arguments);
    },
    setValue: function(value) {
        var user_method_custom = Ext.getCmp('user_method_custom_'+this.getId());
        var user_custom_value = Ext.getCmp('user_custom_value_'+this.getId());

        this.gridPanel.getStore().load();
        user_method_custom.setValue(true);
        user_custom_value.setValue(value);
    },
    getValue: function() {
        var user_method_custom = Ext.getCmp('user_method_custom_'+this.getId());
        if (user_method_custom.getValue()) {
            var user_custom_value = Ext.getCmp('user_custom_value_'+this.getId());
            return user_custom_value.getValue();
        } else{
            var str = "";
            var first = true;
            for ( var i = 0 ; i < this.gridPanel.store.data.items.length ; i++ ) {
                var row = this.gridPanel.store.data.items[i].data;
                if (row.checked) {
                    if (row.uid == "[any]")
                        return "[any]"; /* if any is checked, the rest is irrelevent */
                    if (!first)
                        str = str + ",";
                    else
                            first = false;
                    str = str + row.uid;
                    }
            }
            return str;
        }
    }
});

// matcher pop-up editor for directory connector groups
Ext.define('Ung.GroupEditorWindow', {
    extend:'Ung.MatcherEditorWindow',
    height: 450,
    width: 550,
    initComponent: function() {
        this.gridPanel = Ext.create('Ext.grid.Panel', {
            title: i18n._('Groups'),
            height: 300,
            width: 400,
            enableColumnHide: false,
            enableColumnMove: false,
            store: Ext.create('Ext.data.Store', {
                data: [],
                sortOnLoad: true,
                sorters: { property: 'SAMAccountName', direction : 'ASC' },
                fields: [{
                    name: "checked"
                },{
                    name: "CN"
                },{
                    name: "SAMAccountName"
                },{
                    name: "displayName",
                    convert: function(val, rec) {
                        if (val != null && val != "")
                            return val;
                        return rec.data.CN;
                    }
                }]
            }),
            columns: [ {
                header: i18n._("Selected"),
                width: 60,
                menuDisabled: true,
                sortable: false,
                xtype:'checkcolumn',
                dataIndex: "checked"
            }, {
                header: i18n._("Name"),
                width: 100,
                menuDisabled: true,
                sortable: true,
                dataIndex: "SAMAccountName"
            },{
                header: i18n._("Full Name"),
                width: 200,
                menuDisabled: true,
                sortable: true,
                dataIndex: "displayName"
            }]
        });

        this.inputLines = [{
            xtype: 'radio',
            name: 'groupMethod',
            boxLabel: i18n._('Specify Groups'),
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, checked) {
                        this.gridPanel.setDisabled(!checked);
                        this.groupCustomValue.setDisabled(checked);
                    }, this)
                }
            }
        }, this.gridPanel, {
            xtype: 'radio',
            name: 'groupMethod',
            groupMethodCustom: true,
            boxLabel: i18n._('Specify a Custom Value'),
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, checked) {
                        this.gridPanel.setDisabled(checked);
                        this.groupCustomValue.setDisabled(!checked);
                    }, this)
                }
            }
        }, {
            xtype:'textfield',
            name: 'groupCustomValue',
            width: 250,
            allowBlank:false
        }];
        this.callParent(arguments);
        
        this.groupMethodCustom = this.down("radio[groupMethodCustom]");
        this.groupCustomValue = this.down("textfield[name=groupCustomValue]");
    },
    populate: function(button) {
        var data = [];
        var node;
        try {
            node = rpc.nodeManager.node("untangle-node-directory-connector");
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        if (node != null) {
            Ext.MessageBox.wait(i18n._("Loading..."), i18n._("Please wait"));
            data = node.getGroupEntries().list;
            Ext.MessageBox.hide();
        } else {
            data.push({ SAMAccountName: "*", displayName: "Any Group"});
        }

        this.gridPanel.getStore().getProxy().data = data;
        this.gridPanel.getStore().load();
        this.callParent(arguments);
    },
    setValue: function(value) {
        this.gridPanel.getStore().load();
        this.groupMethodCustom.setValue(true);
        this.groupCustomValue.setValue(value);
    },
    getValue: function() {
        if (this.groupMethodCustom.getValue()) {
            return this.groupCustomValue.getValue();
        } else{
            var str = "";
            var first = true;
            for ( var i = 0 ; i < this.gridPanel.store.data.items.length ; i++ ) {
                var row = this.gridPanel.store.data.items[i].data;
                if (row.checked) {
                    if (row.SAMAccountName == "*")
                        return "*"; /* if any is checked, the rest is irrelevent */
                    if (!first)
                        str = str + ",";
                    else
                        first = false;
                    str = str + row.SAMAccountName;
                }
            }
            return str;
        }
    }
});

//matcher pop-up editor for time groups
Ext.define('Ung.FieldConditionWindow', {
    extend:'Ung.MatcherEditorWindow',
    height: 150,
    width: 500,
    initComponent: function() {
        this.inputLines = [{
            xtype: 'panel',
            border: false,
            layout: {
                type: 'table',
                columns: 3
            },
            defaults: {
                padding: 3
            },
            items: [{
                xtype: 'label',
                html: i18n._("Field Name"),
                columnWidth: 0.3
            },{
                xtype: 'label',
                html: i18n._("Comparator"),
                columnWidth: 0.2
            },{
                xtype: 'label',
                html: i18n._("Value")
            },{
                xtype: 'textfield',
                name: "field",
                allowBlank: false
            },{
                xtype: 'combo',
                name: "comparator",
                editable: false,
                allowBlank: false,
                store: [["=","="], ["!=","!="], ["<","<"], ["<=","<="], [">",">"], [">=",">="]]
            },{
                xtype: 'textfield',
                name: "value"
            }]
        }];
        this.callParent(arguments);
    },
    setValue: function(value) {
        var field = "";
        var comparator = "";
        var val = "";
        if(value) {
            var jsonobj = value;
            field = jsonobj.field;
            comparator = jsonobj.comparator;
            val = jsonobj.value;
        }
        this.down('textfield[name="field"]').setValue(field);
        this.down('combo[name="comparator"]').setValue(comparator);
        this.down('textfield[name="value"]').setValue(val);
    },
    getValue: function() {
        var jsonobj = {
            field: this.down('textfield[name="field"]').getValue(),
            comparator: this.down('combo[name="comparator"]').getValue(),
            value: this.down('textfield[name="value"]').getValue(),
            javaClass: "com.untangle.node.reports.AlertRuleConditionField"
        };
        return jsonobj;
    }
});

// matcher pop-up editor for countries
Ext.define('Ung.CountryEditorWindow', {
    extend:'Ung.MatcherEditorWindow',
    height: 450,
    width: 550,
    initComponent: function() {
        this.gridPanel = Ext.create('Ext.grid.Panel', {
            title: i18n._('Countries'),
            height: 300,
            width: 400,
            enableColumnHide: false,
            enableColumnMove: false,
            store: Ext.create('Ext.data.Store', {
                data: [],
                sortOnLoad: true,
                sorters: { property: 'CountryName', direction : 'ASC' },
                fields: [{
                    name: "checked"
                },{
                    name: "CountryCode"
                },{
                    name: "CountryName"
                }]
            }),
            columns: [ {
                header: i18n._("Selected"),
                width: 80,
                menuDisabled: true,
                sortable: false,
                xtype:'checkcolumn',
                dataIndex: "checked"
            }, {
                header: i18n._("ISO Code"),
                width: 80,
                menuDisabled: true,
                sortable: true,
                dataIndex: "CountryCode"
            },{
                header: i18n._("Country Name"),
                width: 200,
                menuDisabled: true,
                sortable: true,
                flex: true,
                dataIndex: "CountryName"
            }]
        });

        this.inputLines = [{
            xtype: 'radio',
            name: 'countryMethod',
            boxLabel: i18n._('Specify Countries'),
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, checked) {
                        this.gridPanel.setDisabled(!checked);
                        this.countryCustomValue.setDisabled(checked);
                    }, this)
                }
            }
        }, this.gridPanel, {
            xtype: 'radio',
            name: 'countryMethod',
            countryMethodCustom: true,
            boxLabel: i18n._('Specify a Custom Value'),
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, checked) {
                        this.gridPanel.setDisabled(checked);
                        this.countryCustomValue.setDisabled(!checked);
                    }, this)
                }
            }
        }, {
            xtype:'textfield',
            name: 'countryCustomValue',
            width: 250,
            allowBlank:false
        }];
        this.callParent(arguments);
        
        this.countryMethodCustom = this.down("radio[countryMethodCustom]");
        this.countryCustomValue = this.down("textfield[name=countryCustomValue]");
    },
    populate: function(button) {
        var data = [];
        data.push({ CountryCode:"*", CountryName:"Any Country" });
        data.push({ CountryCode:"AF", CountryName:"Afghanistan" });
        data.push({ CountryCode:"AX", CountryName:"Aland Islands" });
        data.push({ CountryCode:"AL", CountryName:"Albania" });
        data.push({ CountryCode:"DZ", CountryName:"Algeria" });
        data.push({ CountryCode:"AS", CountryName:"American Samoa" });
        data.push({ CountryCode:"AD", CountryName:"Andorra" });
        data.push({ CountryCode:"AO", CountryName:"Angola" });
        data.push({ CountryCode:"AI", CountryName:"Anguilla" });
        data.push({ CountryCode:"AQ", CountryName:"Antarctica" });
        data.push({ CountryCode:"AG", CountryName:"Antigua and Barbuda" });
        data.push({ CountryCode:"AR", CountryName:"Argentina" });
        data.push({ CountryCode:"AM", CountryName:"Armenia" });
        data.push({ CountryCode:"AW", CountryName:"Aruba" });
        data.push({ CountryCode:"AU", CountryName:"Australia" });
        data.push({ CountryCode:"AT", CountryName:"Austria" });
        data.push({ CountryCode:"AZ", CountryName:"Azerbaijan" });
        data.push({ CountryCode:"BS", CountryName:"Bahamas" });
        data.push({ CountryCode:"BH", CountryName:"Bahrain" });
        data.push({ CountryCode:"BD", CountryName:"Bangladesh" });
        data.push({ CountryCode:"BB", CountryName:"Barbados" });
        data.push({ CountryCode:"BY", CountryName:"Belarus" });
        data.push({ CountryCode:"BE", CountryName:"Belgium" });
        data.push({ CountryCode:"BZ", CountryName:"Belize" });
        data.push({ CountryCode:"BJ", CountryName:"Benin" });
        data.push({ CountryCode:"BM", CountryName:"Bermuda" });
        data.push({ CountryCode:"BT", CountryName:"Bhutan" });
        data.push({ CountryCode:"BO", CountryName:"Bolivia, Plurinational State of" });
        data.push({ CountryCode:"BQ", CountryName:"Bonaire, Sint Eustatius and Saba" });
        data.push({ CountryCode:"BA", CountryName:"Bosnia and Herzegovina" });
        data.push({ CountryCode:"BW", CountryName:"Botswana" });
        data.push({ CountryCode:"BV", CountryName:"Bouvet Island" });
        data.push({ CountryCode:"BR", CountryName:"Brazil" });
        data.push({ CountryCode:"IO", CountryName:"British Indian Ocean Territory" });
        data.push({ CountryCode:"BN", CountryName:"Brunei Darussalam" });
        data.push({ CountryCode:"BG", CountryName:"Bulgaria" });
        data.push({ CountryCode:"BF", CountryName:"Burkina Faso" });
        data.push({ CountryCode:"BI", CountryName:"Burundi" });
        data.push({ CountryCode:"KH", CountryName:"Cambodia" });
        data.push({ CountryCode:"CM", CountryName:"Cameroon" });
        data.push({ CountryCode:"CA", CountryName:"Canada" });
        data.push({ CountryCode:"CV", CountryName:"Cape Verde" });
        data.push({ CountryCode:"KY", CountryName:"Cayman Islands" });
        data.push({ CountryCode:"CF", CountryName:"Central African Republic" });
        data.push({ CountryCode:"TD", CountryName:"Chad" });
        data.push({ CountryCode:"CL", CountryName:"Chile" });
        data.push({ CountryCode:"CN", CountryName:"China" });
        data.push({ CountryCode:"CX", CountryName:"Christmas Island" });
        data.push({ CountryCode:"CC", CountryName:"Cocos (Keeling) Islands" });
        data.push({ CountryCode:"CO", CountryName:"Colombia" });
        data.push({ CountryCode:"KM", CountryName:"Comoros" });
        data.push({ CountryCode:"CG", CountryName:"Congo" });
        data.push({ CountryCode:"CD", CountryName:"Congo, the Democratic Republic of the" });
        data.push({ CountryCode:"CK", CountryName:"Cook Islands" });
        data.push({ CountryCode:"CR", CountryName:"Costa Rica" });
        data.push({ CountryCode:"CI", CountryName:"Cote d'Ivoire" });
        data.push({ CountryCode:"HR", CountryName:"Croatia" });
        data.push({ CountryCode:"CU", CountryName:"Cuba" });
        data.push({ CountryCode:"CW", CountryName:"Curacao" });
        data.push({ CountryCode:"CY", CountryName:"Cyprus" });
        data.push({ CountryCode:"CZ", CountryName:"Czech Republic" });
        data.push({ CountryCode:"DK", CountryName:"Denmark" });
        data.push({ CountryCode:"DJ", CountryName:"Djibouti" });
        data.push({ CountryCode:"DM", CountryName:"Dominica" });
        data.push({ CountryCode:"DO", CountryName:"Dominican Republic" });
        data.push({ CountryCode:"EC", CountryName:"Ecuador" });
        data.push({ CountryCode:"EG", CountryName:"Egypt" });
        data.push({ CountryCode:"SV", CountryName:"El Salvador" });
        data.push({ CountryCode:"GQ", CountryName:"Equatorial Guinea" });
        data.push({ CountryCode:"ER", CountryName:"Eritrea" });
        data.push({ CountryCode:"EE", CountryName:"Estonia" });
        data.push({ CountryCode:"ET", CountryName:"Ethiopia" });
        data.push({ CountryCode:"FK", CountryName:"Falkland Islands (Malvinas)" });
        data.push({ CountryCode:"FO", CountryName:"Faroe Islands" });
        data.push({ CountryCode:"FJ", CountryName:"Fiji" });
        data.push({ CountryCode:"FI", CountryName:"Finland" });
        data.push({ CountryCode:"FR", CountryName:"France" });
        data.push({ CountryCode:"GF", CountryName:"French Guiana" });
        data.push({ CountryCode:"PF", CountryName:"French Polynesia" });
        data.push({ CountryCode:"TF", CountryName:"French Southern Territories" });
        data.push({ CountryCode:"GA", CountryName:"Gabon" });
        data.push({ CountryCode:"GM", CountryName:"Gambia" });
        data.push({ CountryCode:"GE", CountryName:"Georgia" });
        data.push({ CountryCode:"DE", CountryName:"Germany" });
        data.push({ CountryCode:"GH", CountryName:"Ghana" });
        data.push({ CountryCode:"GI", CountryName:"Gibraltar" });
        data.push({ CountryCode:"GR", CountryName:"Greece" });
        data.push({ CountryCode:"GL", CountryName:"Greenland" });
        data.push({ CountryCode:"GD", CountryName:"Grenada" });
        data.push({ CountryCode:"GP", CountryName:"Guadeloupe" });
        data.push({ CountryCode:"GU", CountryName:"Guam" });
        data.push({ CountryCode:"GT", CountryName:"Guatemala" });
        data.push({ CountryCode:"GG", CountryName:"Guernsey" });
        data.push({ CountryCode:"GN", CountryName:"Guinea" });
        data.push({ CountryCode:"GW", CountryName:"Guinea-Bissau" });
        data.push({ CountryCode:"GY", CountryName:"Guyana" });
        data.push({ CountryCode:"HT", CountryName:"Haiti" });
        data.push({ CountryCode:"HM", CountryName:"Heard Island and McDonald Islands" });
        data.push({ CountryCode:"VA", CountryName:"Holy See (Vatican City State)" });
        data.push({ CountryCode:"HN", CountryName:"Honduras" });
        data.push({ CountryCode:"HK", CountryName:"Hong Kong" });
        data.push({ CountryCode:"HU", CountryName:"Hungary" });
        data.push({ CountryCode:"IS", CountryName:"Iceland" });
        data.push({ CountryCode:"IN", CountryName:"India" });
        data.push({ CountryCode:"ID", CountryName:"Indonesia" });
        data.push({ CountryCode:"IR", CountryName:"Iran, Islamic Republic of" });
        data.push({ CountryCode:"IQ", CountryName:"Iraq" });
        data.push({ CountryCode:"IE", CountryName:"Ireland" });
        data.push({ CountryCode:"IM", CountryName:"Isle of Man" });
        data.push({ CountryCode:"IL", CountryName:"Israel" });
        data.push({ CountryCode:"IT", CountryName:"Italy" });
        data.push({ CountryCode:"JM", CountryName:"Jamaica" });
        data.push({ CountryCode:"JP", CountryName:"Japan" });
        data.push({ CountryCode:"JE", CountryName:"Jersey" });
        data.push({ CountryCode:"JO", CountryName:"Jordan" });
        data.push({ CountryCode:"KZ", CountryName:"Kazakhstan" });
        data.push({ CountryCode:"KE", CountryName:"Kenya" });
        data.push({ CountryCode:"KI", CountryName:"Kiribati" });
        data.push({ CountryCode:"KP", CountryName:"Korea, Democratic People's Republic of" });
        data.push({ CountryCode:"KR", CountryName:"Korea, Republic of" });
        data.push({ CountryCode:"KW", CountryName:"Kuwait" });
        data.push({ CountryCode:"KG", CountryName:"Kyrgyzstan" });
        data.push({ CountryCode:"LA", CountryName:"Lao People's Democratic Republic" });
        data.push({ CountryCode:"LV", CountryName:"Latvia" });
        data.push({ CountryCode:"LB", CountryName:"Lebanon" });
        data.push({ CountryCode:"LS", CountryName:"Lesotho" });
        data.push({ CountryCode:"LR", CountryName:"Liberia" });
        data.push({ CountryCode:"LY", CountryName:"Libya" });
        data.push({ CountryCode:"LI", CountryName:"Liechtenstein" });
        data.push({ CountryCode:"LT", CountryName:"Lithuania" });
        data.push({ CountryCode:"LU", CountryName:"Luxembourg" });
        data.push({ CountryCode:"MO", CountryName:"Macao" });
        data.push({ CountryCode:"MK", CountryName:"Macedonia, the Former Yugoslav Republic of" });
        data.push({ CountryCode:"MG", CountryName:"Madagascar" });
        data.push({ CountryCode:"MW", CountryName:"Malawi" });
        data.push({ CountryCode:"MY", CountryName:"Malaysia" });
        data.push({ CountryCode:"MV", CountryName:"Maldives" });
        data.push({ CountryCode:"ML", CountryName:"Mali" });
        data.push({ CountryCode:"MT", CountryName:"Malta" });
        data.push({ CountryCode:"MH", CountryName:"Marshall Islands" });
        data.push({ CountryCode:"MQ", CountryName:"Martinique" });
        data.push({ CountryCode:"MR", CountryName:"Mauritania" });
        data.push({ CountryCode:"MU", CountryName:"Mauritius" });
        data.push({ CountryCode:"YT", CountryName:"Mayotte" });
        data.push({ CountryCode:"MX", CountryName:"Mexico" });
        data.push({ CountryCode:"FM", CountryName:"Micronesia, Federated States of" });
        data.push({ CountryCode:"MD", CountryName:"Moldova, Republic of" });
        data.push({ CountryCode:"MC", CountryName:"Monaco" });
        data.push({ CountryCode:"MN", CountryName:"Mongolia" });
        data.push({ CountryCode:"ME", CountryName:"Montenegro" });
        data.push({ CountryCode:"MS", CountryName:"Montserrat" });
        data.push({ CountryCode:"MA", CountryName:"Morocco" });
        data.push({ CountryCode:"MZ", CountryName:"Mozambique" });
        data.push({ CountryCode:"MM", CountryName:"Myanmar" });
        data.push({ CountryCode:"NA", CountryName:"Namibia" });
        data.push({ CountryCode:"NR", CountryName:"Nauru" });
        data.push({ CountryCode:"NP", CountryName:"Nepal" });
        data.push({ CountryCode:"NL", CountryName:"Netherlands" });
        data.push({ CountryCode:"NC", CountryName:"New Caledonia" });
        data.push({ CountryCode:"NZ", CountryName:"New Zealand" });
        data.push({ CountryCode:"NI", CountryName:"Nicaragua" });
        data.push({ CountryCode:"NE", CountryName:"Niger" });
        data.push({ CountryCode:"NG", CountryName:"Nigeria" });
        data.push({ CountryCode:"NU", CountryName:"Niue" });
        data.push({ CountryCode:"NF", CountryName:"Norfolk Island" });
        data.push({ CountryCode:"MP", CountryName:"Northern Mariana Islands" });
        data.push({ CountryCode:"NO", CountryName:"Norway" });
        data.push({ CountryCode:"OM", CountryName:"Oman" });
        data.push({ CountryCode:"PK", CountryName:"Pakistan" });
        data.push({ CountryCode:"PW", CountryName:"Palau" });
        data.push({ CountryCode:"PS", CountryName:"Palestine, State of" });
        data.push({ CountryCode:"PA", CountryName:"Panama" });
        data.push({ CountryCode:"PG", CountryName:"Papua New Guinea" });
        data.push({ CountryCode:"PY", CountryName:"Paraguay" });
        data.push({ CountryCode:"PE", CountryName:"Peru" });
        data.push({ CountryCode:"PH", CountryName:"Philippines" });
        data.push({ CountryCode:"PN", CountryName:"Pitcairn" });
        data.push({ CountryCode:"PL", CountryName:"Poland" });
        data.push({ CountryCode:"PT", CountryName:"Portugal" });
        data.push({ CountryCode:"PR", CountryName:"Puerto Rico" });
        data.push({ CountryCode:"QA", CountryName:"Qatar" });
        data.push({ CountryCode:"RE", CountryName:"Reunion" });
        data.push({ CountryCode:"RO", CountryName:"Romania" });
        data.push({ CountryCode:"RU", CountryName:"Russian Federation" });
        data.push({ CountryCode:"RW", CountryName:"Rwanda" });
        data.push({ CountryCode:"BL", CountryName:"Saint Barthelemy" });
        data.push({ CountryCode:"SH", CountryName:"Saint Helena, Ascension and Tristan da Cunha" });
        data.push({ CountryCode:"KN", CountryName:"Saint Kitts and Nevis" });
        data.push({ CountryCode:"LC", CountryName:"Saint Lucia" });
        data.push({ CountryCode:"MF", CountryName:"Saint Martin (French part)" });
        data.push({ CountryCode:"PM", CountryName:"Saint Pierre and Miquelon" });
        data.push({ CountryCode:"VC", CountryName:"Saint Vincent and the Grenadines" });
        data.push({ CountryCode:"WS", CountryName:"Samoa" });
        data.push({ CountryCode:"SM", CountryName:"San Marino" });
        data.push({ CountryCode:"ST", CountryName:"Sao Tome and Principe" });
        data.push({ CountryCode:"SA", CountryName:"Saudi Arabia" });
        data.push({ CountryCode:"SN", CountryName:"Senegal" });
        data.push({ CountryCode:"RS", CountryName:"Serbia" });
        data.push({ CountryCode:"SC", CountryName:"Seychelles" });
        data.push({ CountryCode:"SL", CountryName:"Sierra Leone" });
        data.push({ CountryCode:"SG", CountryName:"Singapore" });
        data.push({ CountryCode:"SX", CountryName:"Sint Maarten (Dutch part)" });
        data.push({ CountryCode:"SK", CountryName:"Slovakia" });
        data.push({ CountryCode:"SI", CountryName:"Slovenia" });
        data.push({ CountryCode:"SB", CountryName:"Solomon Islands" });
        data.push({ CountryCode:"SO", CountryName:"Somalia" });
        data.push({ CountryCode:"ZA", CountryName:"South Africa" });
        data.push({ CountryCode:"GS", CountryName:"South Georgia and the South Sandwich Islands" });
        data.push({ CountryCode:"SS", CountryName:"South Sudan" });
        data.push({ CountryCode:"ES", CountryName:"Spain" });
        data.push({ CountryCode:"LK", CountryName:"Sri Lanka" });
        data.push({ CountryCode:"SD", CountryName:"Sudan" });
        data.push({ CountryCode:"SR", CountryName:"Suriname" });
        data.push({ CountryCode:"SJ", CountryName:"Svalbard and Jan Mayen" });
        data.push({ CountryCode:"SZ", CountryName:"Swaziland" });
        data.push({ CountryCode:"SE", CountryName:"Sweden" });
        data.push({ CountryCode:"CH", CountryName:"Switzerland" });
        data.push({ CountryCode:"SY", CountryName:"Syrian Arab Republic" });
        data.push({ CountryCode:"TW", CountryName:"Taiwan, Province of China" });
        data.push({ CountryCode:"TJ", CountryName:"Tajikistan" });
        data.push({ CountryCode:"TZ", CountryName:"Tanzania, United Republic of" });
        data.push({ CountryCode:"TH", CountryName:"Thailand" });
        data.push({ CountryCode:"TL", CountryName:"Timor-Leste" });
        data.push({ CountryCode:"TG", CountryName:"Togo" });
        data.push({ CountryCode:"TK", CountryName:"Tokelau" });
        data.push({ CountryCode:"TO", CountryName:"Tonga" });
        data.push({ CountryCode:"TT", CountryName:"Trinidad and Tobago" });
        data.push({ CountryCode:"TN", CountryName:"Tunisia" });
        data.push({ CountryCode:"TR", CountryName:"Turkey" });
        data.push({ CountryCode:"TM", CountryName:"Turkmenistan" });
        data.push({ CountryCode:"TC", CountryName:"Turks and Caicos Islands" });
        data.push({ CountryCode:"TV", CountryName:"Tuvalu" });
        data.push({ CountryCode:"UG", CountryName:"Uganda" });
        data.push({ CountryCode:"UA", CountryName:"Ukraine" });
        data.push({ CountryCode:"AE", CountryName:"United Arab Emirates" });
        data.push({ CountryCode:"GB", CountryName:"United Kingdom" });
        data.push({ CountryCode:"US", CountryName:"United States" });
        data.push({ CountryCode:"UM", CountryName:"United States Minor Outlying Islands" });
        data.push({ CountryCode:"UY", CountryName:"Uruguay" });
        data.push({ CountryCode:"UZ", CountryName:"Uzbekistan" });
        data.push({ CountryCode:"VU", CountryName:"Vanuatu" });
        data.push({ CountryCode:"VE", CountryName:"Venezuela, Bolivarian Republic of" });
        data.push({ CountryCode:"VN", CountryName:"Viet Nam" });
        data.push({ CountryCode:"VG", CountryName:"Virgin Islands, British" });
        data.push({ CountryCode:"VI", CountryName:"Virgin Islands, U.S." });
        data.push({ CountryCode:"WF", CountryName:"Wallis and Futuna" });
        data.push({ CountryCode:"EH", CountryName:"Western Sahara" });
        data.push({ CountryCode:"YE", CountryName:"Yemen" });
        data.push({ CountryCode:"ZM", CountryName:"Zambia" });
        data.push({ CountryCode:"ZW", CountryName:"Zimbabwe" });

        this.gridPanel.getStore().getProxy().data = data;
        this.gridPanel.getStore().load();
        this.callParent(arguments);
    },
    setValue: function(value) {
        this.gridPanel.getStore().load();
        this.countryMethodCustom.setValue(true);
        this.countryCustomValue.setValue(value);
    },
    getValue: function() {
        if (this.countryMethodCustom.getValue()) {
            return this.countryCustomValue.getValue();
        } else{
            var str = "";
            var first = true;
            for ( var i = 0 ; i < this.gridPanel.store.data.items.length ; i++ ) {
                var row = this.gridPanel.store.data.items[i].data;
                if (row.checked) {
                    if (row.CountryName == "*")
                        return "*"; /* if any is checked, the rest is irrelevent */
                    if (!first)
                        str = str + ",";
                    else
                        first = false;
                    str = str + row.CountryCode;
                }
            }
            return str;
        }
    }
});


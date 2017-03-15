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
            node = rpc.nodeManager.node("directory-connector");
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
            node = rpc.nodeManager.node("directory-connector");
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
    width: 530,
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
    height: 480,
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
                sorters: { property: 'code', direction : 'ASC' },
                fields: [{
                    name: "checked"
                },{
                    name: "code"
                },{
                    name: "name"
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
                dataIndex: "code"
            },{
                header: i18n._("Country Name"),
                width: 200,
                menuDisabled: true,
                sortable: true,
                flex: true,
                dataIndex: "name"
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
        this.gridPanel.getStore().getProxy().data = Ung.Main.getCountryList();
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
                    if (!first)
                        str = str + ",";
                    else
                        first = false;
                    str = str + row.code;
                }
            }
            return str;
        }
    }
});

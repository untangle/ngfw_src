if (!Ung.hasResource["Ung.Reporting"]) {
    Ung.hasResource["Ung.Reporting"] = true;
    Ung.Settings.registerClassName('untangle-node-reporting', 'Ung.Reporting');

    Ung.Reporting = Ext.extend(Ung.Settings, {
        panelStatus : null,
        panelGeneration : null,
        gridRecipients : null,
        gridIpMap : null,
        // called when the component is rendered
        onRender : function(container, position) {
            // call superclass renderer first
            Ung.Reporting.superclass.onRender.call(this, container, position);
            // builds the 3 tabs
            this.buildStatus();
            this.buildGeneration();
            this.buildIpMap();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelStatus, this.panelGeneration, this.gridIpMap]);
            this.tabs.activate(this.panelStatus);
        },
        getReportingSettings : function(forceReload) {
            if (forceReload || this.rpc.reportingSettings === undefined) {
                this.rpc.reportingSettings = this.getRpcNode().getReportingSettings();
            }
            return this.rpc.reportingSettings;
        },
        // get mail settings
        getMailSettings : function(forceReload) {
            if (forceReload || this.rpc.mailSettings === undefined) {
                this.rpc.mailSettings = rpc.adminManager.getMailSettings();
            }
            return this.rpc.mailSettings;
        },
        // Status Panel
        buildStatus : function() {
            this.panelStatus = new Ext.Panel({
                title : this.i18n._('Status'),
                name : 'Status',
                layout : "form",
                autoScroll : true,
                bodyStyle : 'padding:5px 5px 0px 5px;',
                items: [{
                    title : this.i18n._('Status'),
                    xtype : 'fieldset',
                    autoHeight : true,
                    items : [{
                        buttonAlign : 'center',
                        footer : false,
                        border : false,
                        buttons: [{
                            xtype : 'button',
                            text : this.i18n._('View Reports'),
                            name : 'View Reports',
                            iconCls : 'actionIcon',
                            handler : function() {
                                var viewReportsUrl = "../reports/";
                                var breadcrumbs = [{
                                    title : i18n._(rpc.currentPolicy.name),
                                    action : function() {
                                        main.iframeWin.closeActionFn();
                                        this.cancelAction();
                                    }.createDelegate(this)
                                }, {
                                    title : this.node.md.displayName,
                                    action : function() {
                                        main.iframeWin.closeActionFn();
                                    }.createDelegate(this)
                                }, {
                                    title : this.i18n._('View Reports')
                                }];
                                main.openInRightFrame(breadcrumbs, viewReportsUrl);
                            }.createDelegate(this)
                        }]
                    }, {
                        buttonAlign : 'center',
                        footer : false,
                        border : false,
                        buttons: [{
                            xtype : 'button',
                            text : this.i18n._('Archived Reports'),
                            name : 'Archived Reports',
                            iconCls : 'actionIcon',
                            handler : function() {
                                var viewReportsUrl = "../reports/archive";
                                var breadcrumbs = [{
                                    title : i18n._(rpc.currentPolicy.name),
                                    action : function() {
                                        main.iframeWin.closeActionFn();
                                        this.cancelAction();
                                    }.createDelegate(this)
                                }, {
                                    title : this.node.md.displayName,
                                    action : function() {
                                        main.iframeWin.closeActionFn();
                                    }.createDelegate(this)
                                }, {
                                    title : this.i18n._('Archived Reports')
                                }];
                                main.openInRightFrame(breadcrumbs, viewReportsUrl);
                            }.createDelegate(this)
                        }]
                    }]
                }]
            });
        },
        // Generation panel
        buildGeneration : function() {
            var storeData = [];
            var reportEmail = this.getMailSettings().reportEmail;
            if (reportEmail != null && reportEmail != "") {
                var values = this.getMailSettings().reportEmail.split(',');
                for(var i=0; i<values.length; i++) {
                    storeData.push({emailAddress: values[i].replace(' ','')});
                }
            }
            
            // WEEKLY SCHEDULE 
            var weeklySundayCurrent = false;
            var weeklyMondayCurrent = false;
            var weeklyTuesdayCurrent = false;
            var weeklyWednesdayCurrent = false;
            var weeklyThursdayCurrent = false;
            var weeklyFridayCurrent = false;
            var weeklySaturdayCurrent = false;
            var weeklySched = this.getReportingSettings().schedule.weeklySched.list;
            for(var i=0; i<weeklySched.length;i++) {
                switch (weeklySched[i].day)
                    {
                    case 1: //Schedule.SUNDAY
                        weeklySundayCurrent = true;
                        break;
                    case 2: //Schedule.MONDAY:
                        weeklyMondayCurrent = true;
                        break;
                    case 3: //Schedule.TUESDAY:
                        weeklyTuesdayCurrent = true;
                        break;
                    case 4: //Schedule.WEDNESDAY:
                        weeklyWednesdayCurrent = true;
                        break;
                    case 5: //Schedule.THURSDAY:
                        weeklyThursdayCurrent = true;
                        break;
                    case 6: //Schedule.FRIDAY:
                        weeklyFridayCurrent = true;
                        break;
                    case 7: //Schedule.SATURDAY:
                        weeklySaturdayCurrent = true;
                        break;
                    }
            }
            
            // MONTHLY SCHEDULE 
            var schedule = this.getReportingSettings().schedule;
            var monthlyFirstCurrent = schedule.monthlyNFirst;
            var monthlyEverydayCurrent = schedule.monthlyNDaily;
            var monthlyOnceCurrent = ( schedule.monthlyNDayOfWk != -1 /*Schedule.NONE*/ );
            var monthlyOnceDayCurrent = schedule.monthlyNDayOfWk;
            var monthlyNoneCurrent = !( monthlyFirstCurrent || monthlyEverydayCurrent || monthlyOnceCurrent );
            var monthlyOnceComboCurrent = monthlyOnceCurrent ? schedule.monthlyNDayOfWk : 1 /*SUNDAY*/ ;
            
            this.panelGeneration = new Ext.Panel({
                // private fields
                name : 'Generation',
                parentId : this.getId(),
                title : this.i18n._('Generation'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true
                },
                items : [{
                    title : this.i18n._('Email'),
                    layout:'column',
                    height : 350,
                    items: [{
                        border: false,
                        columnWidth:.5,
                        items: [ this.gridRecipients = new Ung.EditorGrid({
                            name : 'Recipients',
                            title : this.i18n._("Recipients"),
                            hasEdit : false,
                            settingsCmp : this,
                            paginated : false,
                            height : 300,
                            emptyRow : {
                                "emailAddress" : "reportrecipient@example.com"
                            },
                            autoExpandColumn : 'emailAddress',
                            data : storeData,
                            dataRoot: null,
                            autoGenerateId: true,
                            fields : [{
                                name : 'emailAddress'
                            }],
                            columns : [{
                                id : 'emailAddress',
                                header : this.i18n._("email address"),
                                width : 200,
                                dataIndex : 'emailAddress',
                                editor : new Ext.form.TextField({
                                    vtype: 'email',
                                    allowBlank : false,
                                    blankText : this.i18n._("The email address cannot be blank.")
                                })
                            }],
                            sortField : 'emailAddress',
                            columnsDefaultSortable : true
                        })]
                    },{
                        border: false,
                        columnWidth:.5,
                        layout: 'form',
                        items: [{
                            xtype : 'fieldset',
                            border: false,
                            height : 300,
                            items : [{
                                xtype : 'checkbox',
                                name : 'Include Incident Reports in emailed reports',
                                boxLabel : String.format(this.i18n._('Include {0}Incident Reports{1} in emailed reports'),'<b>','</b>'),
                                hideLabel : true,
                                checked : this.getReportingSettings().emailDetail,
                                listeners : {
                                    "check" : {
                                        fn : function(elem, newValue) {
                                            this.getReportingSettings().emailDetail = newValue;
                                        }.createDelegate(this)
                                    }
                                }
                            }, {
                            	border : false,
                            	html : this.i18n._('This makes emailed reports larger, but includes information about each incident/violation.')
                            }]
                        }]
                    }]
                },{
                	title : this.i18n._("Daily Reports"),
                    labelWidth: 150,
                	items : [{
                		border : false,
                		html : this.i18n._('Daily Reports are generated at midnight and covers events from the previous 24 hours, up to, but not including the day of generation.')
                	},  {
                        xtype : 'checkbox',
                        name : 'Generate Daily Reports',
                        fieldLabel : this.i18n._('Generate Daily Reports'),
                        boxLabel : this.i18n._('Every Day'),
                        checked : this.getReportingSettings().schedule.daily,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getReportingSettings().schedule.daily = newValue;
                                }.createDelegate(this)
                            }
                        }
                	}]
                },{
                    title : this.i18n._("Weekly Reports"),
                    labelWidth: 150,
                    items : [{
                        border : false,
                        html : this.i18n._('Weekly Reports are generated at midnight and covers events from the previous 7 days, up to, but not including the day of generation.')
                    },  {
                        xtype : 'checkbox',
                        name : 'Sunday',
                        id : 'reporting_weeklySunday', 
                        fieldLabel : this.i18n._('Generate Weekly Reports'),
                        boxLabel : this.i18n._('Sunday'),
                        checked : weeklySundayCurrent
                    },  {
                        xtype : 'checkbox',
                        name : 'Monday',
                        id : 'reporting_weeklyMonday', 
                        boxLabel : this.i18n._('Monday'),
                        hasLabel : false,
                        labelSeparator : '',
                        checked : weeklyMondayCurrent
                    },  {
                        xtype : 'checkbox',
                        name : 'Tuesday',
                        id : 'reporting_weeklyTuesday', 
                        boxLabel : this.i18n._('Tuesday'),
                        hasLabel : false,
                        labelSeparator : '',
                        checked : weeklyTuesdayCurrent
                    },  {
                        xtype : 'checkbox',
                        name : 'Wednesday',
                        id : 'reporting_weeklyWednesday', 
                        boxLabel : this.i18n._('Wednesday'),
                        hasLabel : false,
                        labelSeparator : '',
                        checked : weeklyWednesdayCurrent
                    },  {
                        xtype : 'checkbox',
                        name : 'Thursday',
                        id : 'reporting_weeklyThursday', 
                        boxLabel : this.i18n._('Thursday'),
                        hasLabel : false,
                        labelSeparator : '',
                        checked : weeklyThursdayCurrent
                    },  {
                        xtype : 'checkbox',
                        name : 'Friday',
                        id : 'reporting_weeklyFriday', 
                        boxLabel : this.i18n._('Friday'),
                        hasLabel : false,
                        labelSeparator : '',
                        checked : weeklyFridayCurrent
                    },  {
                        xtype : 'checkbox',
                        name : 'Saturday',
                        id : 'reporting_weeklySaturday', 
                        boxLabel : this.i18n._('Saturday'),
                        hasLabel : false,
                        labelSeparator : '',
                        checked : weeklySaturdayCurrent
                    }]
                },{
                    title : this.i18n._("Monthly Reports"),
                    labelWidth: 150,
                    items : [{
                        border : false,
                        html : this.i18n._('Monthly Reports are generated at midnight and covers events from the previous 30 days, up to, but not including the day of generation.')
                    },  {
                        xtype : 'radiogroup',
                        name : 'Generate Monthly Reports',
                        fieldLabel : 'Generate Monthly Reports',
                        itemCls: 'x-check-group-alt',
                        columns: 1,
                        items : [{
                            boxLabel : this.i18n._('Never'),
                            name: 'rb-col',
                            id : 'reporting_monthlyNone',
                            checked : monthlyNoneCurrent
                        },{
                            boxLabel : this.i18n._('First Day of Month'),
                            name: 'rb-col',
                            id : 'reporting_monthlyFirst',
                            checked : monthlyFirstCurrent
                        },{
                            boxLabel : this.i18n._('Everyday'),
                            name: 'rb-col',
                            id : 'reporting_monthlyEveryday',
                            checked : monthlyEverydayCurrent
                        },{
                            boxLabel : this.i18n._('Once Per Week'),
                            name: 'rb-col',
                            id : 'reporting_monthlyOnce',
                            checked : monthlyOnceCurrent,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                            Ext.getCmp('reporting_monthlyOnceCombo').setDisabled(!checked);
                                    }
                                }
                            }
                        }]
                    }, {
                        xtype : 'combo',
                        editable : false,
                        mode : 'local',
                        fieldLabel : '',
                        labelSeparator : '',
                        name : "Once Per Week combo",
                        id : 'reporting_monthlyOnceCombo', 
                        store : new Ext.data.SimpleStore({
                            fields : ['monthlyOnceValue', 'monthlyOnceName'],
                            data : [[1, this.i18n._("Sunday")], 
                                    [2, this.i18n._("Monday")],
                                    [3, this.i18n._("Tuesday")],
                                    [4, this.i18n._("Wednesday")],
                                    [5, this.i18n._("Thursday")],
                                    [6, this.i18n._("Friday")],
                                    [7, this.i18n._("Saturday")]]
                        }),
                        displayField : 'monthlyOnceName',
                        valueField : 'monthlyOnceValue',
                        value : monthlyOnceComboCurrent,
                        disabled : !monthlyOnceCurrent,
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small'
                    }]
                },{
                    title : this.i18n._("Data Retention"),
                    labelWidth: 150,
                    items : [{
                        border : false,
                        html : this.i18n._("Limits data retention to one week, this allows reports to run faster on high traffic sites.")
                    },  {
                        xtype : 'checkbox',
                        name : 'Limit data retention',
                        fieldLabel : this.i18n._('Limit data retention'),
                        boxLabel : this.i18n._("Keep One Week's Data"),
                        checked : this.getReportingSettings().daysToKeep == 8,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getReportingSettings().daysToKeep = newValue ? 8 : 33;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }]
            });
        },  
        // IP Map grid
        buildIpMap : function() {
            this.gridIpMap = new Ung.EditorGrid({
                settingsCmp : this,
                name : 'IP addresses',
                title : this.i18n._("IP addresses"),
                emptyRow : {
                    "ipMaddr" : "0.0.0.0/32",
                    "name" : this.i18n._("[no name]"),
                    "description" : this.i18n._("[no description]")
                },
                // the column is autoexpanded if the grid width permits
                autoExpandColumn : 'name',
                recordJavaClass : "com.untangle.uvm.node.IPMaddrRule",
                
                data : this.getReportingSettings().networkDirectory.entries,
                dataRoot: 'list',
                
                // the list of fields
                fields : [{
                    name : 'id'
                }, {
                    name : 'ipMaddr'
                }, {
                    name : 'name'
                }, {
                    name : 'description'
                }],
                // the list of columns for the column model
                columns : [{
                    id : 'ipMaddr',
                    header : this.i18n._("IP address"),
                    width : 200,
                    dataIndex : 'ipMaddr',
                    editor : new Ext.form.TextField({})                    
                }, {
                    id : 'name',
                    header : this.i18n._("name"),
                    width : 200,
                    dataIndex : 'name',
                    editor : new Ext.form.TextField({})                    
                }],
                columnsDefaultSortable : true,
                // the row input lines used by the row editor window
                rowEditorInputLines : [new Ext.form.TextField({
                    name : "Subnet",
                    dataIndex : "ipMaddr",
                    fieldLabel : this.i18n._("IP Address"),
                    allowBlank : false,
                    width : 200
                }), new Ext.form.TextField({
                    name : "Name",
                    dataIndex : "name",
                    fieldLabel : this.i18n._("Name"),
                    allowBlank : false,
                    width : 200
                })]
            });
        },
        // validation
        validateServer : function() {
            // ipMaddr list must be validated server side
            var ipMapList = this.gridIpMap.getSaveList();
            var ipMaddrList = [];
            // added
            for (var i = 0; i < ipMapList[0].list.length; i++) {
                ipMaddrList.push(ipMapList[0].list[i]["ipMaddr"]);
            }
            // modified
            for (var i = 0; i < ipMapList[2].list.length; i++) {
                ipMaddrList.push(ipMapList[2].list[i]["ipMaddr"]);
            }
            if (ipMaddrList.length > 0) {
                try {
                    var result = this.getValidator().validate({
                        list : ipMaddrList,
                        "javaClass" : "java.util.ArrayList"
                    });
                    if (!result.valid) {
                        var errorMsg = "";
                        switch (result.errorCode) {
                            case 'INVALID_IPMADDR' : 
                                errorMsg = this.i18n._("Invalid \"IP address\" specified") + ": " + result.cause;
                            break;
                            default :
                                errorMsg = this.i18n._(result.errorCode) + ": " + result.cause;
                        }
                        
                        this.tabs.activate(this.gridIpMap);
                        this.gridIpMap.focusFirstChangedDataByFieldValue("ipMaddr", result.cause);
                        Ext.MessageBox.alert(this.i18n._("Validation failed"), errorMsg);
                        return false;
                    }
                } catch (e) {
                    Ext.MessageBox.alert(i18n._("Failed"), e.message);
                    return false;
                }
            }
                
            return true;
        },
        // save function
        saveAction : function() {
            if (this.validate()) {
                this.saveSemaphore = 2;
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                
                // set weekly schedule
                var weeklySched = [];
                if (Ext.getCmp('reporting_weeklySunday').getValue())  weeklySched.push({javaClass:"com.untangle.node.reporting.WeeklyScheduleRule", day:1});
                if (Ext.getCmp('reporting_weeklyMonday').getValue())  weeklySched.push({javaClass:"com.untangle.node.reporting.WeeklyScheduleRule", day:2});
                if (Ext.getCmp('reporting_weeklyTuesday').getValue())  weeklySched.push({javaClass:"com.untangle.node.reporting.WeeklyScheduleRule", day:3});
                if (Ext.getCmp('reporting_weeklyWednesday').getValue())  weeklySched.push({javaClass:"com.untangle.node.reporting.WeeklyScheduleRule", day:4});
                if (Ext.getCmp('reporting_weeklyThursday').getValue())  weeklySched.push({javaClass:"com.untangle.node.reporting.WeeklyScheduleRule", day:5});
                if (Ext.getCmp('reporting_weeklyFriday').getValue())  weeklySched.push({javaClass:"com.untangle.node.reporting.WeeklyScheduleRule", day:6});
                if (Ext.getCmp('reporting_weeklySaturday').getValue())  weeklySched.push({javaClass:"com.untangle.node.reporting.WeeklyScheduleRule", day:7});
                this.getReportingSettings().schedule.weeklySched.list = weeklySched;
                
                // set monthly schedule
                var schedule = this.getReportingSettings().schedule;
                schedule.monthlyNFirst = Ext.getCmp('reporting_monthlyFirst').getValue();
                schedule.monthlyNDaily = Ext.getCmp('reporting_monthlyEveryday').getValue();
                var monthlyOnce = Ext.getCmp('reporting_monthlyOnce').getValue();
                schedule.monthlyNDayOfWk = monthlyOnce ? Ext.getCmp('reporting_monthlyOnceCombo').getValue() : -1 //NONE ;
                
                // set Ip Map list
                this.getReportingSettings().networkDirectory.entries.list = this.gridIpMap.getFullSaveList();
                this.getRpcNode().setReportingSettings(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getReportingSettings());
                
                // save email recipients
                var gridRecipientsValues = this.gridRecipients.getFullSaveList();
                var recipientsList = [];
                for(var i=0; i<gridRecipientsValues.length; i++) {
                    recipientsList.push(gridRecipientsValues[i].emailAddress);
                }
                this.getMailSettings().reportEmail = recipientsList.length == 0 ? "" : recipientsList.join(",");
                // do the save
                rpc.adminManager.setMailSettings(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getMailSettings());
                
            }
        },
        afterSave : function() {
            this.saveSemaphore--;
            if (this.saveSemaphore == 0) {
                Ext.MessageBox.hide();
                this.cancelAction();
            }
        }        
    });
}
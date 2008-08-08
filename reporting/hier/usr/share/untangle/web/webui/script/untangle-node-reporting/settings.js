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
            this.buildTabPanel([this.panelStatus, /*this.panelGeneration,*/ this.gridIpMap]);
        },
        getReportingSettings : function(forceReload) {
            if (forceReload || this.rpc.reportingSettings === undefined) {
                this.rpc.reportingSettings = this.getRpcNode().getReportingSettings();
            }
            return this.rpc.reportingSettings;
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
                                rpc.adminManager.generateAuthNonce(function (result, exception) {
                                    if(exception) { 
                                        Ext.MessageBox.alert(i18n._("Failed"),exception.message); 
                                        return;
                                    }
                                    var viewReportsUrl = "../reports/?" + result;
                                    window.open(viewReportsUrl);
                                }.createDelegate(this));
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
                                rpc.adminManager.generateAuthNonce(function (result, exception) {
                                    if(exception) { 
                                        Ext.MessageBox.alert(i18n._("Failed"),exception.message); 
                                        return;
                                    }
                                    var viewReportsUrl = "../reports/archive?" + result;
                                    window.open(viewReportsUrl);
                                }.createDelegate(this));
                            }.createDelegate(this)
                        }]
                    }]
                }]
            });
        },
        // Generation panel
        buildGeneration : function() {
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
        save : function() {
            if (this.validate()) {
                this.saveSemaphore = 1;
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                
                this.getReportingSettings().networkDirectory.entries.list = this.gridIpMap.getFullSaveList();
                this.getRpcNode().setReportingSettings(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getReportingSettings());
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
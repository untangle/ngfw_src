if (!Ung.hasResource["Ung.Reporting"]) {
  Ung.hasResource["Ung.Reporting"] = true;
  Ung.NodeWin.registerClassName('untangle-node-reporting', 'Ung.Reporting');

  Ung.Reporting = Ext.extend(Ung.NodeWin, {
      panelStatus: null,
      panelGeneration: null,
      gridRecipients: null,
      gridIpMap: null,
      initComponent: function(container, position) {
          // builds the 3 tabs
          this.buildStatus();
          this.buildGeneration();
          this.buildIpMap();
          // builds the tab panel with the tabs
          this.buildTabPanel([this.panelStatus, this.panelGeneration, this.gridIpMap]);
          this.tabs.activate(this.panelStatus);
          Ung.Reporting.superclass.initComponent.call(this);
      },
      getReportingSettings: function(forceReload) {
          if (forceReload || this.rpc.reportingSettings === undefined) {
              try {
                  this.rpc.reportingSettings = this.getRpcNode().getReportingSettings();
              } catch (e) {
                  Ung.Util.rpcExHandler(e);
              }
          }
          return this.rpc.reportingSettings;
      },
      // get mail settings
      getMailSettings: function(forceReload) {
          if (forceReload || this.rpc.mailSettings === undefined) {
              try {
                  this.rpc.mailSettings = rpc.adminManager.getMailSettings();
              } catch (e) {
                  Ung.Util.rpcExHandler(e);
              }
          }
          return this.rpc.mailSettings;
      },
      // Status Panel
      buildStatus: function() {
          this.panelStatus = new Ext.Panel({ 
              title: this.i18n._('Status'),
              name: 'Status',
              helpSource: 'status',
              layout: "form",
              autoScroll: true,
              cls: 'ung-panel',
              items: [{ 
                  title: this.i18n._('Status'),
                  xtype: 'fieldset',
                  autoHeight: true,
                  items: [{ 
                      buttonAlign: 'center',
                      footer: false,
                      border: false,
                      buttons: [{ 
                          xtype: 'button',
                          text: this.i18n._('View Reports'),
                          name: 'View Reports',
                          iconCls: 'action-icon',
                          handler: function() {
                              var viewReportsUrl = "../reports/";
                              var breadcrumbs = [{ 
                                  title: i18n._(rpc.currentPolicy.name),
                                  action: function() {
                                      main.iframeWin.closeActionFn();
                                      this.cancelAction();
                                  }.createDelegate(this)
                              }, { 
                                  title: this.node.md.displayName,
                                  action: function() {
                                      main.iframeWin.closeActionFn();
                                  }.createDelegate(this)
                              }, {
                                  title: this.i18n._('View Reports') 
                              }];
                              window.open(viewReportsUrl);
                          }.createDelegate(this)
                      }]
                  }]
              }]
          });
      },
      // Generation panel
      buildGeneration: function() {
          var storeData = [];
          var reportEmail = this.getMailSettings().reportEmail;
          if (reportEmail != null && reportEmail != "") {
              var values = this.getMailSettings().reportEmail.split(',');
              for(var i=0; i<values.length; i++) {
                  storeData.push({emailAddress: values[i].replace(' ','')});
              }
          }

          this.panelGeneration = new Ext.Panel({
              // private fields
              name: 'Generation',
              helpSource: 'generation',
              parentId: this.getId(),
              title: this.i18n._('Generation'),
              layout: "anchor",
              cls: 'ung-panel',
              autoScroll: true,
              defaults: {
                  anchor: "98%",
                  xtype: 'fieldset',
                  autoHeight: true
              },
              items: [{
                  title: this.i18n._('Email'),
                  layout:'column',
                  height: 350,
                  items: [{
                      border: false,
                      columnWidth:.5,
                      items: [ this.gridRecipients = new Ung.EditorGrid({
                          name: 'Recipients',
                          title: this.i18n._("Recipients"),
                          hasEdit: false,
                          settingsCmp: this,
                          paginated: false,
                          height: 300,
                          emptyRow: {
                              "emailAddress": "reportrecipient@example.com"
                          },
                          autoExpandColumn: 'emailAddress',
                          data: storeData,
                          dataRoot: null,
                          autoGenerateId: true,
                          fields: [{
                              name: 'emailAddress'
                          }],
                          columns: [{
                              id: 'emailAddress',
                              header: this.i18n._("email address"),
                              width: 200,
                              dataIndex: 'emailAddress',
                              editor: new Ext.form.TextField({
                                  vtype: 'email',
                                  allowBlank: false,
                                  blankText: this.i18n._("The email address cannot be blank.")
                              })
                          }],
                          sortField: 'emailAddress',
                          columnsDefaultSortable: true
                      })]
                  }]
              },{
                  title: this.i18n._("Data Retention"),
                  labelWidth: 150,
                  items: [{
                      border: false,
                      cls: 'description',
                      html: this.i18n._("Limit Data Retention to a number of days. The smaller the number the lower the disk space requirements and resource usage during report generation.")
                  },{ 
                      xtype : 'numberfield',
                      fieldLabel : this.i18n._('Limit Data Retention'),
                      name : 'Limit Data Retention',
                      id: 'reporting_daysToKeep',
                      value : this.getReportingSettings().daysToKeep,
                      width: 25,
                      allowDecimals: false,
                      allowNegative: false,
                      minValue: 1,
                      maxValue: 30,
                      listeners : {
                          "change" : {
                              fn : function(elem, newValue) {
                                  this.getReportingSettings().daysToKeep = newValue;
                              }.createDelegate(this)
                          }
                      }
                  }]
              }]
          });
      },
      // IP Map grid
      buildIpMap: function() {
          this.gridIpMap = new Ung.EditorGrid({
              settingsCmp: this,
              name: 'Name Map',
              helpSource: 'ip_addresses',
              title: this.i18n._("Name Map"),
              emptyRow: {
                  "ipMaddr": "0.0.0.0/32",
                  "name": this.i18n._("[no name]"),
                  "description": this.i18n._("[no description]")
              },
              // the column is autoexpanded if the grid width permits
              autoExpandColumn: 'name',
              recordJavaClass: "com.untangle.uvm.node.IPMaddrRule",

              data: this.getReportingSettings().networkDirectory.entries,
              dataRoot: 'list',

              // the list of fields
              fields: [{
                  name: 'id'
              }, {
                  name: 'ipMaddr'
              }, {
                  name: 'name'
              }, {
                  name: 'description'
              }],
              // the list of columns for the column model
              columns: [{
                  id: 'ipMaddr',
                  header: this.i18n._("Name Map"),
                  width: 200,
                  dataIndex: 'ipMaddr',
                  editor: new Ext.form.TextField({})
              }, {
                  id: 'name',
                  header: this.i18n._("name"),
                  width: 200,
                  dataIndex: 'name',
                  editor: new Ext.form.TextField({})
              }],
              columnsDefaultSortable: true,
              // the row input lines used by the row editor window
              rowEditorInputLines: [new Ext.form.TextField({
                  name: "Subnet",
                  dataIndex: "ipMaddr",
                  fieldLabel: this.i18n._("Name Map"),
                  allowBlank: false,
                  width: 200
              }), new Ext.form.TextField({
                  name: "Name",
                  dataIndex: "name",
                  fieldLabel: this.i18n._("Name"),
                  allowBlank: false,
                  width: 200
              })]
          });
      },
      // validation
      validateServer: function() {
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
                  var result=null;
                  try {
                      result = this.getValidator().validate({
                          list: ipMaddrList,
                          "javaClass": "java.util.ArrayList"
                      });
                  } catch (e) {
                      Ung.Util.rpcExHandler(e);
                  }
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
                  var message = ( e == null ) ? "Unknown" : e.message;
                  if (message == "Unknown") {
                      message = i18n._("Please Try Again");
                  }
                  Ext.MessageBox.alert(i18n._("Failed"), message);
                  return false;
              }
          }

          return true;
      },
      // save function
      saveAction: function() {
          if (this.validate()) {
              this.saveSemaphore = 2;
              Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
              if(!this.panelGeneration.rendered) {
                  var activeTab=this.tabs.getActiveTab();
                  this.tabs.activate(this.panelGeneration);
                  this.tabs.activate(activeTab);
              }

              // set Ip Map list
              this.getReportingSettings().networkDirectory.entries.list = this.gridIpMap.getFullSaveList();
              this.getRpcNode().setReportingSettings(function(result, exception) {
                  if(Ung.Util.handleException(exception)) return;
                  this.afterSave();
              }.createDelegate(this), this.getReportingSettings());

              // save email recipients
              var gridRecipientsValues = this.gridRecipients.getFullSaveList();
              var recipientsList = [];
              for(var i=0; i<gridRecipientsValues.length; i++) {
                  recipientsList.push(gridRecipientsValues[i].emailAddress);
              }
              this.getMailSettings().reportEmail = recipientsList.length == 0 ? "": recipientsList.join(",");
              // do the save
              rpc.adminManager.setMailSettings(function(result, exception) {
                  if(Ung.Util.handleException(exception)) return;
                  this.afterSave();
              }.createDelegate(this), this.getMailSettings());

          }
      },
      afterSave: function() {
          this.saveSemaphore--;
          if (this.saveSemaphore == 0) {
              Ext.MessageBox.hide();
              this.closeWindow();
          }
      },
      isDirty: function() {
          if(this.panelGeneration.rendered) {
              var cmpIds = [ 'reporting_daysToKeep'];
              for (var i = 0; i < cmpIds.length; i++) {
                  if (Ext.getCmp(cmpIds[i]).isDirty()){
                      return true;
                  }
              }
              if (this.gridRecipients.isDirty()){
                  return true;
              }
          }
          return this.gridIpMap.isDirty();
      }
  });
}

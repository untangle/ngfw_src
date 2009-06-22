Ext.namespace('Ung');

var rpc = null;
var reports = null;
Ext.onReady(function() {
    reports = new Ung.Reports({});
});
// Main object class
Ung.Reports = Ext.extend(Object, {
    //The selected reports date
    reportsDate:null,
    // the table of contents data for the left side
    tableOfContents:null,
    //the selected node from the left side tree
    selectedNode: null,
    //report details object
    reportDetails:null,
    // breadcrumbs object for the report details
    breadcrumbs: null,
    constructor : function(config) {
        Ext.apply(this, config);
        this.init();
    },
    init : function() {
        this.initSemaphore = 3;
        this.treeNodes =[];
        rpc = {};
        // get JSONRpcClient
        rpc.jsonrpc = new JSONRpcClient("/webui/JSON-RPC");
        // get language manager
        rpc.jsonrpc.RemoteUvmContext.languageManager(function(result, exception) {
            if (exception) {
                Ext.MessageBox.alert("Failed", exception.message);
                return;
            };
            rpc.languageManager = result;
            // get translations for main module
            rpc.languageManager.getTranslations(function(result, exception) {
                if (exception) {
                    Ext.MessageBox.alert("Failed", exception.message);
                    return
                };
                i18n = new Ung.I18N({
                    "map" : result.map
                });
                this.postinit();// 1
            }.createDelegate(this), "untangle-libuvm");
                // get language settings

            }.createDelegate(this));
        rpc.jsonrpc.RemoteUvmContext.skinManager(function(result, exception) {
            if (exception) {
              Ext.MessageBox.alert("Failed", exception.message);
              return;
            };
            rpc.skinManager = result;
            // Load Current Skin
            rpc.skinManager.getSkinSettings(function(result, exception) {
                if (exception) {
                    Ext.MessageBox.alert("Failed", exception.message);
                    return;
                };
                rpc.skinSettings = result;
                Ung.Util.loadCss("/skins/" + rpc.skinSettings.userPagesSkin + "/css/ext-skin.css");
                Ung.Util.loadCss("/skins/"+rpc.skinSettings.userPagesSkin+"/css/reports.css");
                this.postinit();// 2
            }.createDelegate(this));
        }.createDelegate(this));
        // get node manager
        rpc.jsonrpc.RemoteUvmContext.reportingManager(function(result, exception) {
            if (exception) {
                Ext.MessageBox.alert("Failed", exception.message);
                return;
            };
            rpc.reportingManager = result;
            rpc.reportingManager.getDates(function(result, exception) {
                if (exception) {
                    Ext.MessageBox.alert("Failed", exception.message);
                    return;
                };
                rpc.dates = result;
                this.postinit();// 3
            }.createDelegate(this));
        }.createDelegate(this));
    },
    postinit : function() {
      this.initSemaphore--;
      if (this.initSemaphore != 0) {
        return;
      }
      this.startApplication();
    },

    startApplication : function() {
      this.reportDatesItems = [];
        for (var i = 0; i < rpc.dates.list.length; i++) {
            this.reportDatesItems.push({
                text : i18n.dateFormat(rpc.dates.list[i]),
                dt : rpc.dates.list[i],
                handler : function() {
                  reports.changeDate(this.dt);
                }
            });
        }

        var panel = new Ext.Panel({
            renderTo : Ext.getBody(),
            cls : "base-container",
            layout : 'border',
            height : window.innerHeight-80,
            defaults : {
                border : false,
                bodyStyle : 'background-color: transparent;'
            },
            items : [{
                region : 'north',
                layout : 'border',
                style : 'padding: 7px 5px 7px 7px;',
                height : 65,
                defaults : {
                    border : false,
                    bodyStyle : 'background-color: transparent;'

                },
                items : [{
                    html: '<img src="/images/BrandingLogo.gif?'+(new Date()).getTime()+'" border="0" height="50"/>',
                    region : 'west',
                    width : 100

                }, {
                    html : '<h1>'+i18n._('Untangle Reports')+'</h1>',
                    region : 'center'
                }, {
                    region : 'east',
                    width : 200,
                    layout : 'fit',
                    items : [{
                        xtype : "fieldset",
                        align : 'right',
                        border : false,
                        items : [{
                            xtype : 'splitbutton',
                            id : 'report-day-menu',
                            text : this.reportDatesItems[0].text,
                            menu : new Ext.menu.Menu({
                                items : this.reportDatesItems
                            })
                        }]
                    }]
                }]
            }, {
                region : "center",
                layout : 'border',
                width : 960,
                height : window.innerHeight-30,
                items : [{
                    xtype : 'treepanel',
                    id : 'tree-panel',
                    region : 'center',
                    margins : '2 2 0 2',
                    autoScroll : true,
                    rootVisible : false,
                    title : i18n._('Reports'),
                    enableDD: false,
                    enableDrag: false,
                    root : new Ext.tree.AsyncTreeNode({
                        draggable : false,
                        //id : 'source',
                        children : []
                    }),
                    loader : new Ext.tree.TreeLoader(),

                    listeners : {
                        'load' : function(node) {
                            // Select the firs element form the tableOfContent tree to load it's report details
                            Ext.getCmp('tree-panel').getSelectionModel().select(Ext.getCmp('tree-panel').getRootNode().firstChild);
                        },
                        'render' : function(tp) {
                            tp.getSelectionModel().on('selectionchange', function(tree, node) {
                                reports.selectedNode=node;
                                reports.breadcrumbs=[];
                                if(node!=null) {
                                    reports.getApplicationData(node.attributes.name)
                                }
                            });

                          p = Ext.urlDecode(window.location.search.substring(1));
                          qsDate = p.date;
                          if (qsDate) {
                            dp = qsDate.split('-');
                            d = new Date(parseInt(dp[0]), parseInt(dp[1]) - 1, parseInt(dp[2]));

                            reports.changeDate({javaClass: 'java.util.Date',
                                                time: d.getTime()});
                          } else if (rpc.dates && rpc.dates.list.length > 0) {
                                reports.changeDate(rpc.dates.list[0])
                            }
                        }
                    }
                }, {
                    region : 'east',
                    title : 'Report Details&nbsp;<span id="breadcrumbs" class="breadcrumbs"></span>',
                    id : 'report-details',
                    layout:"anchor",
                    autoScroll : true,
                    collapsible : false,
                    split : true,
                    margins : '2 2 0 2',
                    cmargins : '2 2 2 2',
                    width : "80%",
                    defaults: {
                        border: false
                    },
                    items : [{
                        html:""
                    }]
                }]
            }]
        });
    },
    getTreeNodesFromTableOfContent : function(tableOfContents) {
        var treeNodes = [];
        if(tableOfContents.platform!=null) {
            treeNodes.push({
                text : i18n._(tableOfContents.platform.title),
                name : tableOfContents.platform.name,
                leaf: true
            });
        }
        if(tableOfContents.applications!=null) {
            var tn = {
                text : i18n._("Applications"),
                name : "applications"
            };
            var tc = tableOfContents.applications;
            if (tc.list != null && tc.list.length > 0) {
                tn.leaf = false;
                tn.children = [];
                for (var i = 0; i < tc.list.length; i++) {
                    tn.children.push({
                        text : i18n._(tc.list[i].title),
                        name : tc.list[i].name,
                        leaf : true,
                        iconCls : tc.list[i].name
                    });
                    tn.expanded = true;
                }
            } else {
                tn.leaf = true;
            }
            treeNodes.push(tn);
        }
        if(tableOfContents.users!=null) {
            treeNodes.push({
                text : i18n._("Users"),
                name : "users",
                leaf: true
            });
        }
        if(tableOfContents.hosts!=null) {
            treeNodes.push({
                text : i18n._("Hosts"),
                name : "hosts",
                leaf: true
            });
        }
        return treeNodes;
    },
    changeDate : function(date) {
        this.reportsDate=date;

        for (var i = 0; i < this.reportDatesItems.length; i++) {
          item = this.reportDatesItems[i];
          if (item.dt.time == date.time) {
            Ext.getCmp('report-day-menu').setText(item.text);
            found = true;
            break;
          }
        }

        rpc.reportingManager.getTableOfContents(function(result, exception) {
            if (exception) {
                Ext.MessageBox.alert("Failed", exception.message);
                return;
            };
            this.tableOfContents = result;
            var treeNodes = this.getTreeNodesFromTableOfContent(this.tableOfContents);
            Ext.getCmp('tree-panel').getSelectionModel().clearSelections();
            var root= Ext.getCmp('tree-panel').getRootNode();
            root.collapse(true);
            root.attributes.children=treeNodes;
            Ext.getCmp('tree-panel').getLoader().load(root);
        }.createDelegate(this), this.reportsDate);
    },
    getApplicationData: function(nodeName) {
        rpc.reportingManager.getApplicationData(function (result, exception) {
            if(exception) {Ext.MessageBox.alert("Failed",exception.message);return};
            rpc.applicationData=result;
            reports.breadcrumbs.push({
                text: this.selectedNode.attributes.text,
                handler: this.getApplicationData.createDelegate(this,[nodeName])
            });

            Ung.Util.loadModuleTranslations( nodeName, i18n,
                function(){
                    reports.reportDetails = new Ung.ReportDetails();
                }
            );
        }.createDelegate(this), reports.reportsDate,nodeName);
    },
    openBreadcrumb: function(breadcrumbIndex) {
        if(this.breadcrumbs.length>breadcrumbIndex) {
            var breadcrumb=this.breadcrumbs[breadcrumbIndex];
            reports.breadcrumbs.splice(breadcrumbIndex,this.breadcrumbs.length-breadcrumbIndex);
            breadcrumb.handler.call(this);
        }
    }
});

// Right section object class
Ung.ReportDetails = Ext.extend(Object, {
    constructor : function(config) {
        Ext.apply(this, config);
        // this.i18n should be used in ReportDetails to have i18n context based
        this.i18n = Ung.i18nModuleInstances[reports.selectedNode.attributes.name];
        this.buildReportDetails();
    },
    buildReportDetails: function() {
        var reportDetails=Ext.getCmp("report-details");
        while(reportDetails.items.length!=0) {
            reportDetails.remove(reportDetails.items.get(0));
        }
        var itemsArray=[];
        //TODO rpc.applicationData should never be null
        if (rpc.applicationData != null) {
            for(var i=0;i<rpc.applicationData.sections.list.length ;i++) {
                var section=rpc.applicationData.sections.list[i];
                var sectionPanel=this.buildSection(section);
                itemsArray.push(sectionPanel);
            }
        }
        //create breadcrums item
        var breadcrumbArr=[];
        for(var i=0;i<reports.breadcrumbs.length;i++) {
            if(i+1==reports.breadcrumbs.length) {
                breadcrumbArr.push(reports.breadcrumbs[i].text);
            } else {
                breadcrumbArr.push('<a href="javascript:reports.openBreadcrumb('+i+')">'+reports.breadcrumbs[i].text+'</a>');
            }
        }
        document.getElementById("breadcrumbs").innerHTML='<span class="icon-breadcrumbs-separator">&nbsp;&nbsp;&nbsp;&nbsp;</span>'+breadcrumbArr.join('<span class="icon-breadcrumbs-separator">&nbsp;&nbsp;&nbsp;&nbsp;</span>');
        if (itemsArray && itemsArray.length > 0) {
          this.tabPanel=new Ext.TabPanel({
              anchor: '100% 100%',
              autoWidth : true,
              defaults: {
                  anchor: '100% 100%',
                  autoWidth : true,
                  autoScroll: true
              },

              height : 400,
              activeTab : 0,
              frame : true,
              items : itemsArray,
              layoutOnTabChange : true
          });
          reportDetails.add(this.tabPanel);
        }
        reportDetails.doLayout();
    },
    buildSection: function(section) {
        var sectionPanel=null;
        if(section.javaClass=="com.untangle.uvm.reports.SummarySection") {
            sectionPanel=this.buildSummarySection(section);
        } else if(section.javaClass=="com.untangle.uvm.reports.DetailSection") {
            sectionPanel=this.buildDetailSection(section);
        } else {
            //For test
          sectionPanel=new Ext.Panel({
            title : this.i18n._(section.title),
            layout : "form",
            items : [{border:false, html:"TODO: "+section.title+" for "+"HIPPIES" +" on date "+ "DIPPIE"}]
          //items : [{border:false, html:"TODO: "+section.title+" for "+this.selectedNode.attributes.text +" on date "+i18n.dateFormat(this.reportsDate)}]
          });
        }
        return sectionPanel;

    },
    buildSummarySection: function (section) {
        var items = [];
        for (var i = 0; i < section.summaryItems.list.length; i++) {
            var summaryItem = section.summaryItems.list[i];
            // graph
            items.push({html:'<img src="'+summaryItem.imageUrl+'"/>', bodyStyle:'padding:20px'});
            // key statistics
            var data = [];
            for(var j=0;j<summaryItem.keyStatistics.list.length;j++) {
              var keyStatistic = summaryItem.keyStatistics.list[j];
              data.push([keyStatistic.label, keyStatistic.value, keyStatistic.unit]);
            }
            items.push( new Ext.grid.GridPanel({
                    store: new Ext.data.SimpleStore({
                        fields: [
                           {name: 'label'},
                           {name: 'value'},
                           {name: 'unit'}
                        ],
                        data: data
                    }),
                    columns: [{
                        id:'label',
                        header: "Label",
                        width: 150,
                        sortable: false,
                        dataIndex: 'label',
                        renderer: function(value) {
                            return this.i18n._(value);
                        }.createDelegate(this)
                    },{
                        header: "Value",
                        width: 150,
                        sortable: false,
                        dataIndex: 'value',
                        renderer: function (value, medata, record) {
                            return record.data.unit == null ? value :  (value + " " + this.i18n._(record.data.unit));
                        }.createDelegate(this)
                    }],
                    // inline toolbars
                    tbar:[{
                        tooltip:this.i18n._('Export Excel'),
                        iconCls:'export-excel',
                        handler : function () {
                          window.open(summaryItem.csvUrl);
                        }
                    }, '-', {
                        tooltip:this.i18n._('Export Printer'),
                        iconCls:'export-printer',
                        handler : function () {
                          window.open(summaryItem.printerUrl);
                        }
                    }],
                    title:this.i18n._('Key Statistics'),
                    stripeRows: true,
                    hideHeaders: true,
                    enableHdMenu : false,
                    enableColumnMove: false
                })
            );
        }
        return new Ext.Panel({
            title : section.title,
            layout:'table',
            defaults: {
                border: false,
                columnWidth: 0.5
            },
            layoutConfig: {
                columns: 2
            },
            items : items
        });
    },

    buildDetailSection: function (section) {
        var columns = [];
        var fields = [];
        var c = null;
        for (var i = 0; i < section.columns.list.length; i++) {
          c = section.columns.list[i];
          //TODO this case should not occur
          if (c == null || c == undefined) { break; }
          var col = { header:this.i18n._(c.title), dataIndex:c.name };

          if (c.type == "Date") {
            col.renderer = function(value) {
              return i18n.timestampFormat(value);
            };
            col.width = 140;
          } else if (c.type == "URL") {
            col.renderer = function(value) {
              return '<a href="' + value + '" target="_new">' + value + '</a>';
            };
            col.width = 160;
          } else if (c.type == "UserLink") {
            col.renderer = function(value) {
              return '<a href="javascript:reports.reportDetails.getApplicationDataForUser(\'' + value + '\')">' + value + '</a>';
            };
            col.width = 100;
          } else if (c.type == "HostLink") {
            col.renderer = function(value) {
              return '<a href="javascript:reports.reportDetails.getApplicationDataForHost(\'' + value + '\')">' + value + '</a>';
            };
            col.width = 100;
          } else if (c.type == "EmailLink") {
            col.renderer = function(value) {
              return '<a href="javascript:reports.reportDetails.getApplicationDataForEmail(\'' + value + '\')">' + value + '</a>';
            };
            col.width = 180;
          }
          columns.push(col);
          fields.push({ name: c.name });
        }

        var store = new Ext.data.SimpleStore({fields: fields, data: [] });

        var detailSection=new Ext.grid.GridPanel({
            title : section.title,
            enableHdMenu : false,
            enableColumnMove: false,
            store: store,
            columns: columns
        });

        rpc.reportingManager.getDetailData(function(result, exception) {
          if (exception) {
            Ext.MessageBox.alert("Failed", exception.message);
            return;
          };

          var data = [];

          for (var i = 0; i < result.list.length; i++) {
            data.push(result.list[i].list);
          }

          store.loadData(data);
        }.createDelegate(this), reports.reportsDate, reports.selectedNode.attributes.name, section.name);

        return detailSection;
    },

    getApplicationDataForUser: function(value) {
        rpc.reportingManager.getApplicationDataForUser(function (result, exception) {
            if(exception) {Ext.MessageBox.alert(this.i18n._("Failed"),exception.message);return};
            rpc.applicationData=result;
            reports.breadcrumbs.push({
                text: value +" "+i18n._("Reports"),
                handler: this.getApplicationDataForUser.createDelegate(this,[value])
            });
            this.buildReportDetails();
        }.createDelegate(this),reports.reportsDate, reports.selectedNode.attributes.name, value);
    },
    getApplicationDataForHost: function(value) {
        rpc.reportingManager.getApplicationDataForHost(function (result, exception) {
            if(exception) {Ext.MessageBox.alert(this.i18n._("Failed"),exception.message);return};
            rpc.applicationData=result;
            reports.breadcrumbs.push({
                text: value +" "+i18n._("Reports"),
                handler: this.getApplicationDataForHost.createDelegate(this,[value])
            });
            this.buildReportDetails();
        }.createDelegate(this),reports.reportsDate, reports.selectedNode.attributes.name, value);
    },
    getApplicationDataForEmail: function(value) {
        rpc.reportingManager.getApplicationDataForEmail(function (result, exception) {
            if(exception) {Ext.MessageBox.alert(this.i18n._("Failed"),exception.message);return};
            rpc.applicationData=result;
            reports.breadcrumbs.push({
                text: value +" "+i18n._("Reports"),
                handler: this.getApplicationDataForEmail.createDelegate(this,[value])
            });
            this.buildReportDetails();
        }.createDelegate(this),reports.reportsDate, reports.selectedNode.attributes.name, value);
    }
});

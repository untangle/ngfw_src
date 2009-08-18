Ext.namespace('Ung');

var rpc = null;
var reports = null;
function getWinHeight(){
    if(!window.innerHeight){
        return window.screen.height - 190;
    }
    return window.innerHeight;
}

Ext.onReady(function()
            {
              reports = new Ung.Reports({});
            });

JSONRpcClient.toplevel_ex_handler = function (ex) {
    if (ex instanceof JSONRpcClient.Exception) {
        // assuming this is because of a session timeout.  navigate to current location,
        // and the auth filter should let them log in and get back to whatever page we're on.
        if (ex.code == 550) {
          setTimeout(function ()
                     {
                       navigate(document.location);
                     }, 300);
          return;
        }
    }

    if (ex) {
        throw ex.message.substring(ex.name.length + 2); //ideally this would throw a complex exception object
    } else {
        throw "Error making rpc request to server";
    }
};

// Main object class
Ung.Reports = Ext.extend(Object,
                         {
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
                           //progress bar for various actions
                           progressBar : null,

                           appNames: { },

                           constructor : function(config) {
                             Ext.apply(this, config);
                             this.init();
                           },
                           init : function() {
                             this.initSemaphore = 3;
                             this.progressBar = Ext.MessageBox;
                             this.treeNodes =[];
                             rpc = {};
                             rpc.jsonrpc = new JSONRpcClient("/webui/JSON-RPC");

                             rpc.jsonrpc.RemoteUvmContext.languageManager(function(result, exception)
                                                                          {
                                                                            if (exception) {
                                                                              Ext.MessageBox.alert("Failed", exception.message);
                                                                            };
                                                                            rpc.languageManager = result;
                                                                            // get translations for main module
                                                                            rpc.languageManager.getTranslations(function(result, exception)
                                                                                                                {
                                                                                                                  if (exception) {
                                                                                                                    Ext.MessageBox.alert("Failed", exception.message);
                                                                                                                    return;
                                                                                                                  }
                                                                                                                  i18n = new Ung.I18N({ "map" : result.map });
                                                                                                                  this.postinit();
                                                                                                                }.createDelegate(this), "untangle-libuvm");
                                                                          }.createDelegate(this));

                             rpc.jsonrpc.RemoteUvmContext.skinManager(function(result, exception)
                                                                      {
                                                                        if (exception) {
                                                                          Ext.MessageBox.alert("Failed", exception.message);
                                                                        };
                                                                        rpc.skinManager = result;
                                                                        // Load Current Skin
                                                                        rpc.skinManager.getSkinSettings(function(result, exception)
                                                                                                        {
                                                                                                          if (exception) {
                                                                                                            Ext.MessageBox.alert("Failed", exception.message);
                                                                                                            return;
                                                                                                          }
                                                                                                          rpc.skinSettings = result;
                                                                                                          Ung.Util.loadCss("/skins/" + rpc.skinSettings.userPagesSkin + "/css/ext-skin.css");
                                                                                                          Ung.Util.loadCss("/skins/"+rpc.skinSettings.userPagesSkin+"/css/reports.css");
                                                                                                          this.postinit();
                                                                                                        }.createDelegate(this));
                                                                      }.createDelegate(this));

                             rpc.jsonrpc.RemoteUvmContext.reportingManager(function(result, exception) {
                                                                             if (exception) {
                                                                               Ext.MessageBox.alert("Failed", exception.message);
                                                                             }
                                                                             rpc.reportingManager = result;
                                                                             rpc.reportingManager.getDates(function(result, exception)
                                                                                                           {
                                                                                                             if (exception) {
                                                                                                               Ext.MessageBox.alert("Failed", exception.message);
                                                                                                               return;
                                                                                                             };
                                                                                                             rpc.dates = result;
                                                                                                             this.postinit();
                                                                                                           }.createDelegate(this));
                                                                           }.createDelegate(this));
                           },

                           postinit : function()
                           {
                             this.initSemaphore--;
                             if (this.initSemaphore != 0) {
                               return;
                             }
                             this.startApplication();
                           },
                           startApplication : function()
                           {
                             this.reportDatesItems = [];
                             for (var i = 0; i < rpc.dates.list.length; i++) {
                               this.reportDatesItems.push({ text : i18n.dateFormat(rpc.dates.list[i]),
                                                            dt : rpc.dates.list[i],
                                                            handler : function()
                                                            {
                                                              reports.changeDate(this.dt);
                                                            }
                                                          });
                             }

                             var panel = new Ext.Panel({
                                                         renderTo : 'base',
                                                         cls : "base-container",
                                                         layout : 'border',
                                                         height : getWinHeight()-80,
                                                         defaults : { border : false,
                                                                      bodyStyle : 'background-color: transparent;'
                                                                    },
                                                         items : [{ region : 'north',
                                                                    layout : 'border',
                                                                    style : 'padding: 7px 5px 7px 7px;',
                                                                    height : 65,
                                                                    defaults : { border : false,
                                                                                 bodyStyle : 'background-color: transparent;'
                                                                               },
                                                                    items : [{ html: '<img src="/images/BrandingLogo.gif?'+(new Date()).getTime()+'" border="0" height="50"/>',
                                                                               region : 'west',
                                                                               width : 100
                                                                             }, { html : '<h1>'+i18n._('Reports')+'</h1>',
                                                                                  region : 'center'
                                                                             }, { region : 'east',
                                                                                  width : 200,
                                                                                  layout : 'fit',
                                                                                  items : [{ xtype : "fieldset",
                                                                                             align : 'right',
                                                                                             border : false,
                                                                                             items : [{ xtype : 'splitbutton',
                                                                                                        id : 'report-day-menu',
                                                                                                        text : this.reportDatesItems[0].text,
                                                                                                        menu : new Ext.menu.Menu({ items : this.reportDatesItems })
                                                                                                      }]
                                                                                           }]
                                                                                }]
                                                                  }, { region : "center",
                                                                       layout : 'border',
                                                                       width : 960,
                                                                       height : getWinHeight() - 30,//'auto',//window.innerHeight-30,
                                                                       items : [{ xtype : 'treepanel',
                                                                                  id : 'tree-panel',
                                                                                  region : 'center',
                                                                                  margins : '2 2 0 2',
                                                                                  autoScroll : true,
                                                                                  rootVisible : false,
                                                                                  title : i18n._('Reports'),
                                                                                  enableDD: false,
                                                                                  enableDrag: false,
                                                                                  root : new Ext.tree.AsyncTreeNode({ draggable : false,
                                                                                                                      //id : 'source',
                                                                                                                      children : []
                                                                                                                    }),
                                                                                  loader : new Ext.tree.TreeLoader(),
                                                                                  listeners : { 'load' : function(node)
                                                                                                {
                                                                                                  // Select the firs element form the tableOfContent tree to load it's report details
                                                                                                  Ext.getCmp('tree-panel').getSelectionModel().select(Ext.getCmp('tree-panel').getRootNode().firstChild);
                                                                                                },
                                                                                                'render' : function(tp)
                                                                                                {
                                                                                                  tp.getSelectionModel().on('selectionchange',
                                                                                                                            function(tree, node)
                                                                                                                            {
                                                                                                                              if(node!=null) {
                                                                                                                                  if (node.attributes.name == 'applications') {
                                                                                                                                    return;
                                                                                                                                  }

                                                                                                                                  reports.selectedNode=node;
                                                                                                                                  reports.breadcrumbs=[];
                                                                                                                                  reports.getApplicationData(node.attributes.name);
                                                                                                                              }
                                                                                                                            });

                                                                                                  p = Ext.urlDecode(window.location.search.substring(1));
                                                                                                  qsDate = p.date;
                                                                                                  if (qsDate) {
                                                                                                    dp = qsDate.split('-');
                                                                                                    d = new Date(parseInt(dp[0]), parseInt(dp[1]) - 1, parseInt(dp[2]));

                                                                                                    reports.changeDate({ javaClass: 'java.util.Date',
                                                                                                                         ime: d.getTime()
                                                                                                                       });
                                                                                                  } else if (rpc.dates && rpc.dates.list.length > 0) {
                                                                                                    reports.changeDate(rpc.dates.list[0]);
                                                                                                  }
                                                                                                }
                                                                                              }
                                                                                }, { region : 'east',
                                                                                     title : 'Report Details&nbsp;<span id="breadcrumbs" class="breadcrumbs"></span>',
                                                                                     id : 'report-details',
                                                                                     layout:"anchor",
                                                                                     autoScroll : true,
                                                                                     collapsible : false,
                                                                                     split : true,
                                                                                     margins : '2 2 0 2',
                                                                                     cmargins : '2 2 2 2',
                                                                                     width : "80%",
                                                                                     defaults: { border: false },
                                                                                     items : [{ html:"" }]
                                                                                   }]
                                                                     }]
                                                       });
                           },

                           getTreeNodesFromTableOfContent : function(tableOfContents)
                           {
                             var treeNodes = [];
                             if (tableOfContents.platform != null) {
                               treeNodes.push({ text : i18n._(tableOfContents.platform.title),
                                                name : tableOfContents.platform.name,
                                                leaf: true,
                                                icon : "./node-icons/untangle-vm.png"
                                              });
                             }

                             if (tableOfContents.applications != null) {
                               var tn = { text : i18n._("Applications"),
                                          name : "applications"
                                        };
                               var tc = tableOfContents.applications;

                               if (tc.list != null && tc.list.length > 0) {
                                 tn.leaf = false;
                                 tn.children = [];
                                 for (var i = 0; i < tc.list.length; i++) {
                                   this.appNames[tc.list[i].name] = tc.list[i].title;
                                   tn.children.push({ text : i18n._(tc.list[i].title),
                                                      name : tc.list[i].name,
                                                      leaf : true,
                                                      icon : "./node-icons/" + tc.list[i].name + ".png"
                                                    });
                                   tn.expanded = true;
                                 }
                               } else {
                                 tn.leaf = true;
                               }

                               treeNodes.push(tn);
                             }

                             if (tableOfContents.users != null) {
                               treeNodes.push({ text : i18n._("Users"),
                                                name : "users",
                                                leaf: true,
                                                icon : "./node-icons/users.png",
                                                listeners : {
                                                    'click' : this.refreshContentPane
                                                }
                                              });
                             }

                             if (tableOfContents.hosts != null) {
                               treeNodes.push({ text : i18n._("Hosts"),
                                                name : "hosts",
                                                leaf: true,
                                                icon : "./node-icons/hosts.png",
                                                listeners : {
                                                    'click' : this.refreshContentPane
                                                }
                                              });
                             }

                             if ( tableOfContents.emails!=null ) {
                               treeNodes.push({ text : i18n._("Emails"),
                                                name : "emails",
                                                leaf: true,
                                                icon : "./node-icons/emails.png",
                                                listeners : {
                                                    'click' : this.refreshContentPane
                                                }
                                              });
                             }

                             return treeNodes;
                           },
                           /**
                            * Refreshes the content pane when a selected node is clicked again
                            */
                           refreshContentPane : function(node,e){
                             //check if someone's clicking on the selected node
                             var selModel = Ext.getCmp('tree-panel').getSelectionModel();
                             if(selModel.getSelectedNode().id == node.id){
                                 //refresh the content pane
                                 selModel.fireEvent('selectionchange',selModel,node);
                             }
                           },
                           changeDate : function(date)
                           {
                             this.reportsDate=date;

                             for (var i = 0; i < this.reportDatesItems.length; i++) {
                               var item = this.reportDatesItems[i];
                               var found = false;

                               if (item.dt.time == date.time) {
                                 Ext.getCmp('report-day-menu').setText(item.text);
                                 var found = true;
                                 break;
                               }
                             }

                             rpc.reportingManager.getTableOfContents(function(result, exception)
                                                                     {
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
                             reports.progressBar.wait(i18n._("Please Wait"));
                             rpc.reportingManager.getApplicationData(function (result, exception)
                                                                     {
                                                                       if (exception) {
                                                                         Ext.MessageBox.alert("Failed",exception.message);
                                                                         return;
                                                                       }
                                                                       rpc.applicationData=result;
                                                                       reports.breadcrumbs.push({ text: this.selectedNode.attributes.text,
                                                                                                  handler: this.getApplicationData.createDelegate(this, [nodeName])
                                                                                                });

                                                                       Ung.Util.loadModuleTranslations( nodeName, i18n,
                                                                                                        function(){
                                                                                                          reports.reportDetails = new Ung.ReportDetails({reportType: nodeName});
                                                                                                          reports.progressBar.hide();
                                                                                                        }
                                                                                                      );
                                                                     }.createDelegate(this), reports.reportsDate,nodeName);
                           },

                           getDrilldownTableOfContents: function(fnName, value)
                           {
                             rpc.drilldownValue = value;
                             reports.progressBar.wait(i18n._("Please Wait"));
                             rpc.reportingManager[fnName](function (result, exception)
                                                          {
                                                            if (exception) {
                                                              Ext.MessageBox.alert(this.i18n._("Failed"),exception.message);
                                                            }
                                                            rpc.applicationData=result;
                                                            reports.breadcrumbs.push({ text: value +" "+i18n._("Reports"),
                                                                                       handler: this.getDrilldownTableOfContents.createDelegate(this, [fnName, value])
                                                                                     });
                                                            this.reportDetails.buildReportDetails(); // XXX take to correct page
                                                            reports.progressBar.hide();
                                                          }.createDelegate(this), reports.reportsDate, value);
                           },

                           getTableOfContentsForUser: function(user)
                           {
                             return this.getDrilldownTableOfContents('getTableOfContentsForUser', user);
                           },

                           getTableOfContentsForHost: function(host)
                           {
                             return this.getDrilldownTableOfContents('getTableOfContentsForHost', host);
                           },

                           getTableOfContentsForEmail: function(email)
                           {
                             return this.getDrilldownTableOfContents('getTableOfContentsForEmail', email);
                           },

                           getDrilldownApplicationData: function(fnName, app, value)
                           {
                             rpc.drilldownValue = value;
                             reports.progressBar.wait(i18n._("Please Wait"));
                             rpc.reportingManager[fnName](function (result, exception)
                                                          {
                                                            if (exception) {
                                                              Ext.MessageBox.alert(i18n._("Failed"),exception.message);
                                                              return;
                                                            }
                                                            rpc.applicationData=result;
                                                            reports.breadcrumbs.push({ text: i18n.sprintf("%s reports ", this.appNames[app]),
                                                                                       handler: this[fnName].createDelegate(this,[app, value])
                                                                                     });
                                                            this.reportDetails.buildReportDetails(); // XXX take to correct page
                                                            reports.progressBar.hide();
                                                          }.createDelegate(this), reports.reportsDate, app, value);
                           },

                           getApplicationDataForUser: function(app, user)
                           {
                             this.getDrilldownApplicationData('getApplicationDataForUser', app, user);
                           },

                           getApplicationDataForHost: function(app, host)
                           {
                             this.getDrilldownApplicationData('getApplicationDataForHost', app, host);
                           },

                           getApplicationDataForEmail: function(app, email)
                           {
                             this.getDrilldownApplicationData('getApplicationDataForEmail', app, email);
                           },

                           openBreadcrumb: function(breadcrumbIndex) {
                             if (this.breadcrumbs.length>breadcrumbIndex) {
                               var breadcrumb = this.breadcrumbs[breadcrumbIndex];
                               reports.breadcrumbs.splice(breadcrumbIndex, this.breadcrumbs.length-breadcrumbIndex);
                               breadcrumb.handler.call(this);
                             }
                           }
                         });

// Right section object class
Ung.ReportDetails = Ext.extend(Object,
                               { reportType : null,
                                 constructor : function(config) {
                                   Ext.apply(this, config);
                                   // this.i18n should be used in ReportDetails to have i18n context based
                                   this.appName = reports.selectedNode.attributes.name;
                                   this.i18n = Ung.i18nModuleInstances[reports.selectedNode.attributes.name];
                                   this.reportType = config.reportType;
                                   this.buildReportDetails();
                                 },

                                 buildDrilldownTableOfContents : function(type)
                                 {
                                   var upperName = type[0].toUpperCase() + type.substr(1);

                                   var data = [];
                                   var i = 0;
                                   var list = rpc.applicationData.applications.list;


                                   for (i=0; i<list.length; i++) {
                                     data.push([list[i].javaClass,list[i].name,list[i].title]);
                                   }

                                   return new Ext.grid.GridPanel({ store: new Ext.data.SimpleStore({ fields: [
                                                                                                       { name: 'javaClass' },
                                                                                                       { name: 'name' },
                                                                                                       { name: 'title' }
                                                                                                     ],
                                                                                                     data: data
                                                                                                   }),
                                                                   columns: [{ id:'title',
                                                                               header: "Application Name",
                                                                               width: 500,
                                                                               sortable: false,
                                                                               dataIndex: 'title',
                                                                               renderer: function(value, medata, record) {
                                                                                 return '<a href="javascript:reports.getApplicationDataFor' + upperName + '(\'' + record.data.name + '\', \'' + rpc.drilldownValue + '\')">' + value + '</a>';
                                                                               }.createDelegate(this)
                                                                             }],
                                                                   title:this.i18n._('Application List'),
                                                                   stripeRows: true,
                                                                   hideHeaders: true,
                                                                   enableHdMenu : false,
                                                                   enableColumnMove: false
                                                                 });
                                 },

                                 buildUserTableOfContents : function()
                                 {
                                   return this.buildDrilldownTableOfContents('user');
                                 },

                                 buildHostTableOfContents : function()
                                 {
                                   return this.buildDrilldownTableOfContents('host');
                                 },

                                 buildEmailTableOfContents : function()
                                 {
                                   return this.buildDrilldownTableOfContents('email');
                                 },

                                 buildDrilldownList : function(type, title, listTitle)
                                 {
                                   var pluralName = type + 's';
                                   var upperName = type[0].toUpperCase() + type.substr(1);

                                   var data = [];
                                   var i = 0;

                                   for(i=0;i<reports.tableOfContents[pluralName].list.length;i++){
                                     data.push([reports.tableOfContents[pluralName].list[i].javaClass,
                                                reports.tableOfContents[pluralName].list[i].name,null]);
                                   }

                                   return new Ext.grid.GridPanel({ store: new Ext.data.SimpleStore({ fields: [
                                                                                                       {name: 'javaClass'},
                                                                                                       {name: 'name'},
                                                                                                       {name: 'linkType'} //this is not used currently
                                                                                                     ],
                                                                                                     data: data }),
                                                                   columns: [{ id:'name',
                                                                               header: title,
                                                                               width: 500,
                                                                               sortable: false,
                                                                               dataIndex: 'name',
                                                                               renderer: function(value, medata, record) {
                                                                                 return '<a href="javascript:reports.getTableOfContentsFor'
                                                                                   + upperName + '(\''+ value + '\')">' + value + '</a>';
                                                                               }.createDelegate(this)
                                                                             }], title:listTitle,
                                                                   stripeRows: true,
                                                                   hideHeaders: true,
                                                                   enableHdMenu : false,
                                                                   enableColumnMove: false
                                                                 });
                                 },

                                 buildUserList: function()
                                 {
                                   return this.buildDrilldownList('user', this.i18n._('User'),
                                                                  this.i18n._('User List'));
                                 },

                                 buildHostList: function()
                                 {
                                   return this.buildDrilldownList('host', this.i18n._('Host'),
                                                                  this.i18n._('Host List'));
                                 },

                                 buildEmailList: function()
                                 {
                                   return this.buildDrilldownList('email', this.i18n._('Email'),
                                                                  this.i18n._('Email List'));
                                 },

                                 buildReportDetails: function()
                                 {
                                   var reportDetails = Ext.getCmp("report-details");
                                   while (reportDetails.items.length!=0) {
                                     reportDetails.remove(reportDetails.items.get(0));
                                   }

                                   var itemsArray=[];
                                   //TODO rpc.applicationData should never be null
                                   if (rpc.applicationData != null) {
                                     if(rpc.applicationData.sections != null){
                                       for(var i=0;i<rpc.applicationData.sections.list.length ;i++) {
                                         var section=rpc.applicationData.sections.list[i];
                                         var sectionPanel=this.buildSection(this.appName, section);
                                         itemsArray.push(sectionPanel);
                                       }
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
                                   } else if(this.reportType != null) {
                                     var selectedType = 'toc';
                                     var reportTypeMap = { 'users':{ 'toc' : this.buildUserList.createDelegate(this),
                                                                     'com.untangle.uvm.reports.TableOfContents' : this.buildUserTableOfContents.createDelegate(this)
                                                                   },
                                                           'hosts':{ 'toc' : this.buildHostList.createDelegate(this),
                                                                     'com.untangle.uvm.reports.TableOfContents' : this.buildHostTableOfContents.createDelegate(this)
                                                                   },
                                                           'emails':{ 'toc' : this.buildEmailList.createDelegate(this),
                                                                     'com.untangle.uvm.reports.TableOfContents' : this.buildEmailTableOfContents.createDelegate(this)
                                                                   }
                                                         };
                                     if (reportTypeMap[this.reportType] != null) {
                                       if (rpc.applicationData != null && reportTypeMap[this.reportType][rpc.applicationData.javaClass] != null) {
                                         selectedType = rpc.applicationData.javaClass;
                                       }
                                     }
                                     reportDetails.add(reportTypeMap[this.reportType][selectedType]());
                                   }
                                   reportDetails.doLayout();
                                 },

                                 buildSection: function(appName, section) {
                                   var sectionPanel=null;
                                   if (section.javaClass=="com.untangle.uvm.reports.SummarySection") {
                                     sectionPanel=this.buildSummarySection(appName, section);
                                   } else if (section.javaClass=="com.untangle.uvm.reports.DetailSection") {
                                     sectionPanel=this.buildDetailSection(appName, section);
                                   }

                                   return sectionPanel;
                                 },

                                 buildSummarySection: function (appName, section) {
                                   var items = [];

                                   for (var i = 0; i < section.summaryItems.list.length; i++) {
                                     var summaryItem = section.summaryItems.list[i];
                                     // graph
                                     items.push({html:'<img src="'+summaryItem.imageUrl+'"/>', bodyStyle:'padding:20px'});
                                     // key statistics

                                     colors = summaryItem.colors.map;

                                     columns = [];
                                     var data = [];
                                     for (var j=0; j<summaryItem.keyStatistics.list.length; j++) {
                                       var keyStatistic = summaryItem.keyStatistics.list[j];
                                       data.push([keyStatistic.label, keyStatistic.value, keyStatistic.unit, keyStatistic.linkType, colors[keyStatistic.label]]);
                                     }

                                     columns = [];

                                     if (summaryItem.plotType == 'pie-chart') {
                                       columns.push({ id:'color',
                                                      header: "Color",
                                                      width: 25,
                                                      sortable: false,
                                                      dataIndex: 'color',
                                                      renderer: function(value, medata, record) {
                                                        return '<div style="position:absolute;height:8px;width:8px;margin-top:2px;background-color:#'+value+'">&nbsp;</div>'
                                                        //return value;
                                                      }.createDelegate(this)
                                                    });
                                     }

                                     columns.push({ id:'label',
                                                    header: "Label",
                                                    width: 150,
                                                    sortable: false,
                                                    dataIndex: 'label',
                                                    renderer: function(value, medata, record) {
                                                      var linkType = record.data.linkType;
                                                      if (linkType == "UserLink") {
                                                        return '<a href="javascript:reports.getApplicationDataForUser(\'' + appName + '\', \'' + value + '\')">' + value + '</a>';
                                                      } else if (linkType == "HostLink") {
                                                        return '<a href="javascript:reports.getApplicationDataForHost(\'' + appName + '\', \'' + value + '\')">' + value + '</a>';
                                                      } else if (linkType == "EmailLink") {
                                                        return '<a href="javascript:reports.getApplicationDataForEmail(\'' + appName + '\', \'' + value + '\')">' + value + '</a>';
                                                      } else {
                                                        return this.i18n._(value);
                                                      }
                                                    }.createDelegate(this)
                                                  });

                                     columns.push({ header: "Value",
                                                    width: 150,
                                                    sortable: false,
                                                    dataIndex: 'value',
                                                    renderer: function (value, medata, record) {
                                                      var unit = record.data.unit;
                                                      if (unit && unit.indexOf('bytes') == 0) {
                                                        if (value < 1000000) {
                                                          value = Math.round(value/1000);
                                                          var s = unit.split("/");
                                                          s[0] = "KB";
                                                          unit = s.join("/");
                                                        } else if (value < 1000000000) {
                                                          value = Math.round(value/1000000);
                                                          var s = unit.split("/");
                                                          s[0] = "MB";
                                                          unit = s.join("/");
                                                        } else {
                                                          value = Math.round(value/1000000000);
                                                          var s = unit.split("/");
                                                          s[0] = "GB";
                                                          unit = s.join("/");
                                                        }
                                                      }

                                                      var v = this.i18n.numberFormat(value);

                                                      return unit == null ? v : (v + " " + this.i18n._(unit));
                                                    }.createDelegate(this)
                                                  });

                                     items.push(new Ext.grid.GridPanel({ store: new Ext.data.SimpleStore({ fields: [ {name: 'label'},
                                                                                                                     {name: 'value'},
                                                                                                                     {name: 'unit'},
                                                                                                                     {name: 'linkType'},
                                                                                                                     {name: 'color'}
                                                                                                                   ],
                                                                                                           data: data
                                                                                                         }),
                                                                                                         columns: columns,
                                                                                                         // inline toolbars
                                                                                                         tbar:[{ tooltip:this.i18n._('Export Excel'),
                                                                                                                 iconCls:'export-excel',
                                                                                                                 handler : new Function("window.open('" + summaryItem.csvUrl + "');")
                                                                                                               }, '-', { tooltip:this.i18n._('Export Printer'),
                                                                                                                         iconCls:'export-printer',
                                                                                                                         handler : new Function("window.open('" + summaryItem.printerUrl + "');")
                                                                                                                       }],
                                                                                                               title:this.i18n._('Key Statistics'),
                                                                                                               stripeRows: true,
                                                                                                               hideHeaders: true,
                                                                                                               enableHdMenu : false,
                                                                                                               enableColumnMove: false
                                                                       })
                                               );
                                   }
                                   return new Ext.Panel({ title : section.title,
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

                                 buildDetailSection: function (appName, section)
                                 {
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
                                         if (!value) {
                                           return i18n._('None');
                                         } else {
                                           return i18n.timestampFormat(value);
                                         }
                                       };
                                       col.width = 140;
                                     } else if (c.type == "URL") {
                                       col.renderer = function(value) {
                                         if (!value) {
                                           return i18n._('None');
                                         } else {
                                           return '<a href="' + value + '" target="_new">' + value + '</a>';
                                         }
                                       };
                                       col.width = 160;
                                     } else if (c.type == "UserLink") {
                                       col.renderer = function(value) {
                                         if (!value) {
                                           return i18n._('None');
                                         } else {
                                           return '<a href="javascript:reports.getApplicationDataForUser(\'' + appName + '\', \'' + rpc.drilldownValue + '\')">' + value + '</a>';
                                         }
                                       };
                                       col.width = 100;
                                     } else if (c.type == "HostLink") {
                                       col.renderer = function(value) {
                                         if (!value) {
                                           return i18n._('None');
                                         } else {
                                           return '<a href="javascript:reports.getApplicationDataForHost(\'' + appName + '\', \'' + rpc.drilldownValue + '\')">' + value + '</a>';
                                         }
                                       };
                                       col.width = 100;
                                     } else if (c.type == "EmailLink") {
                                       col.renderer = function(value) {
                                         if (!value) {
                                           return i18n._('None');
                                         } else {
                                           return '<a href="javascript:reports.getApplicationDataForEmail(\'' + appName + '\', \'' + rpc.drilldownValue + '\')">' + value + '</a>';
                                         }
                                       };
                                       col.width = 180;
                                     } else {
                                       col.renderer = function(value) {
                                         if (!value) {
                                           return i18n._('None');
                                         } else {
                                           return value;
                                         }
                                       };
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
                                 }});

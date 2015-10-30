var rpc = {}; // the main json rpc object
var testMode = false;

Ext.define("Ung.Inbox", {
    singleton: true,
    viewport: null,
    init: function(config) {
        Ext.apply(this, config);
        rpc = new JSONRpcClient("/quarantine/JSON-RPC").Quarantine;
        
        this.startApplication();
    },
    buildQuarantine: function() {
        this.gridQuarantine = Ext.create('Ext.grid.Panel', {
            name: 'gridQuarantine',
            flex: 1,
            enableColumnHide: false,
            enableColumnMove: false,
            dockedItems: [],
            store: Ext.create('Ext.data.Store', {
                sortOnLoad: true,
                sorters: { property: 'internDate', direction : 'DESC' },
                fields: [ 
                    {name:'recipients'},
                    {name:'mailID'},
                    {name:'internDate'},
                    {name:'size'},
                    {name:'attachmentCount'},
                    {name:'truncatedSender'},
                    {name:'sender', sortType:Ext.data.SortTypes.asUCString}, 
                    {name:'truncatedSubject'},
                    {name:'subject'},
                    {name:'quarantineCategory'},
                    {name:'quarantineDetail', sortType:'asFloat'},
                    {name:'quarantineSize'}
                ]
            }),
            columns: [{
               header: i18n._( "From" ),
               dataIndex: 'sender',
               width: 250,
               filter: {
                   type: 'string'
               }
           },{
               header: "<div class='quarantine-attachment-header'>&nbsp</div>",
               dataIndex: 'attachmentCount',
               width: 60,
               tooltip: i18n._( "Number of Attachments in the email." ),
               align: 'center',
               filter: {
                   type: 'numeric'
               }
           },{
               header: i18n._( "Score" ),
               dataIndex: 'quarantineDetail',
               width: 60,
               align: 'center',
               filter: {
                   type: 'numeric'
               }
           },{
               header: i18n._( "Subject" ),
               dataIndex: 'truncatedSubject',
               flex: 1,
               width: 250,
               filter: {
                   type: 'string'
               }
           },{
               header: i18n._( "Date" ),
               dataIndex: 'internDate',
               width: 135,
               renderer: function( value ) {
                   var date = new Date();
                   date.setTime( value );
                   d = Ext.util.Format.date( date, 'm/d/Y' );
                   t = Ext.util.Format.date( date, 'g:i a' );
                   return d + ' ' + t;
               },
               filter: { type: 'datetime',
                   dataIndex: 'internDate',
                   date: {
                       format: 'm/d/Y'
                   },
                   time: {
                       format: 'g:i a',
                       increment: 1
                   },
                   validateRecord : function (record) {
                       var me = this, 
                       key,
                       pickerValue,
                       val1 = record.get(me.dataIndex);
                       
                       var val = new Date(val1.time);
                       if(!Ext.isDate(val)){
                           return false;
                       }
                       val = val.getTime();

                       for (key in me.fields) {
                           if (me.fields[key].checked) {
                               pickerValue = me.getFieldValue(key).getTime();
                               if (key == 'before' && pickerValue <= val) {
                                   return false;
                               }
                               if (key == 'after' && pickerValue >= val) {
                                   return false;
                               }
                               if (key == 'on' && pickerValue != val) {
                                   return false;
                               }
                           }
                       }
                       return true;
                   }
                 }
           },{
               header: i18n._( "Size (KB)" ),
               dataIndex: 'size',
               renderer: function( value ) {
                   return Math.round( (( value + 0.0 ) / 1024) * 10 ) / 10;
               },
               width: 60,
               filter: {
                   type: 'numeric'
               }
           }]
        });
        
        this.panelQuarantine = {
            xtype: 'panel',
            title: i18n._("Quarantined Messages" ),
            layout: { type: 'vbox', align: 'stretch' },
            items: [{
                xtype:'component',
                flex: 0,
                html: Ext.String.format( i18n._( "The messages below were quarantined and will be deleted after {0} days." ), this.quarantineDays),
                border: true,
                margin: 5
                
            }, this.gridQuarantine]
        };
    },
    buildSafeList: function() {
        this.gridSafeList = Ext.create('Ext.grid.Panel', {
            name: 'gridSafeList',
            flex: 1,
            enableColumnHide: false,
            enableColumnMove: false,
            selModel: Ext.create('Ext.selection.CheckboxModel',{
                //listeners:
            }),
            dockedItems: [],
            store: Ext.create('Ext.data.ArrayStore',{
                fields:[{name:'emailAddress'}],
                data : this.safelistData
            }),
            columns: [{
                header: i18n._( "Email Address" ),
                dataIndex: 'emailAddress',
                flex: 1,
                menuDisabled: true,
                field: {
                    xtype:'textfield'
                }
            }]
        });
        this.panelSafeList = {
            xtype: 'panel',
            title: i18n._("Safe List" ),
            layout: { type: 'vbox', align: 'stretch' },
            items: [{
                xtype:'component',
                flex: 0,
                html: i18n._( "You can use the Safe List to make sure that messages from these senders are never quarantined." ),
                border: true,
                margin: 5
            }, this.gridSafeList]
        };
    },
    buildRemaps: function() {
        this.panelRemaps = {
            xtype: 'panel',
            title: i18n._("Forward or Receive Quarantines" ),
            html: 'TODO'
        };
    },
    startApplication: function() {
        document.title = Ext.String.format(i18n._("{0} | Quarantine Digest for: {1}"), this.companyName, this.currentAddress);
        this.buildQuarantine();
        this.buildSafeList();
        this.buildRemaps();
        
        this.viewport = Ext.create('Ext.container.Viewport',{
            layout:'border',
            items:[{
                region: 'north',
                padding: 5,
                height: 70,
                xtype: 'container',
                layout: { type: 'hbox', align: 'stretch' },
                items: [{
                    xtype: 'container',
                    html: '<img src="/images/BrandingLogo.png?'+(new Date()).getTime()+'" border="0" height="60"/>',
                    width: 100,
                    flex: 0
                }, {
                    xtype: 'component',
                    padding: '27 10 0 10',
                    style: 'text-align:right; font-family: sans-serif; font-weight:bold;font-size:18px;',
                    flex: 1,
                    html: Ext.String.format(i18n._("Quarantine Digest for: {0}"), this.currentAddress)
                }
            ]}, {
                xtype: 'tabpanel',
                region:'center',
                activeTab: 0,
                deferredRender: false,
                border: false,
                plain: true,
                flex: 1,
                items:[this.panelQuarantine, this.panelSafeList, this.panelRemaps]
            }
        ]});
    }
});
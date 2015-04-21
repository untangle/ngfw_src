Ext.namespace('Ung');
Ext.namespace('Ext.ux');
Ext.BLANK_IMAGE_URL = '/ext4/resources/themes/images/gray/tree/s.gif'; // The location of the blank pixel image
Ext.Loader.setConfig({enabled: true});
Ext.Loader.setPath('Ext.ux', '/ext4/examples/ux');
Ext.require([
    'Ext.ux.data.PagingMemoryProxy',
    'Ext.ux.grid.FiltersFeature'
]);

var testMode = false;
/**
 * @class Ext.ux.toolbar.PagingOptions
 * @author Arthur Kay (http://www.akawebdesign.com)
 * @namespace Ext.ux.toolbar
 * @extends Ext.toolbar.Paging
 * @constructor
 * @param {object} configObj
 */

var quarantineTabPanel = null;
var quarantine = null;
var safelist = null;
var remaps = null;

var i18n = null;

Ung.Quarantine = function() {};

Ung.Quarantine.prototype = {
    rpc: null,
    init: function() {
        //get JSONRpcClient
        this.rpc = new JSONRpcClient("/quarantine/JSON-RPC").Quarantine;
        this.store = new Ung.QuarantineStore( { quarantine: this } );
        this.selectionModel = new Ung.QuarantineSelectionModel( { quarantine: this } );

        this.releaseButton= Ext.create('Ext.button.Button', {
            handler: Ext.bind(function() { this.releaseOrDelete( quarantine.rpc.releaseMessages, i18n._("Releasing...") ); }, this ),
            iconCls: 'icon-move-mails',
            text: i18n._( "Release to Inbox" ),
            disabled: true
        } );

        this.safelistButton = Ext.create('Ext.button.Button', {
            handler: Ext.bind(function() { this.safelist(); }, this ),
            iconCls:'icon-safe-list',
            text: i18n._( "Release to Inbox & Add Senders to Safe List" ),
            disabled: true
        } );

        this.deleteButton = Ext.create('Ext.button.Button', {
            handler: Ext.bind(function() { this.releaseOrDelete( quarantine.rpc.purgeMessages, i18n._("Deleting...") ); }, this ),
            iconCls:'icon-delete-row',
            text: i18n._( "Delete" ),
            disabled: true
        } );

        // This is used to show messages when they are available
        this.messageDisplayTip = Ext.create('Ext.tip.Tip', {
            cls: 'action-messages'
        });

        this.messageCount = 0;

        this.hideTimeout = null;

        this.grid = Ext.create('Ung.QuarantineGrid', { quarantine: this } );
    },
    store: null,
    selectionModel: null,
    grid: null,
    pageSize: null,

    releaseOrDelete: function( actionFn, actionStr ) {
        Ext.MessageBox.wait( actionStr , i18n._("Please wait"));
        var mids = [];
        var selections = this.grid.getSelectionModel().getSelection();
        Ext.each(selections, function(item) {
            mids.push(item.data.mailID);
        });

        this.grid.getSelectionModel().deselectAll();
        this.grid.setDisabled( true );
        actionFn( Ext.bind(this.refreshTable, this ), inboxDetails.token, mids );
    },

    safelist: function( addresses ) {
        Ext.MessageBox.wait( i18n._("Releasing and adding Senders to Safe List...") , i18n._("Please wait"));
        if(addresses == null) {
            addresses = [];
        }
        var selections = this.grid.getSelectionModel().getSelection();
        Ext.each(selections, function(item) {
        	if(item.data.sender != null)
        		addresses.push(item.data.sender);
        });
        
        this.grid.getSelectionModel().deselectAll();
        this.grid.setDisabled( true );
        Ext.Function.defer(function() {
        	if (addresses.length == 0){
            	Ext.MessageBox.alert(i18n._("An error has occurred."), i18n._("No sender address to be added to safelist."));
            	this.grid.setDisabled( false );
            	return;
            }
            this.rpc.safelist( Ext.bind(this.refreshTable, this ), inboxDetails.token, addresses );
        }, 1 ,this);
    },

    handleException: function(exception) {
        if(exception) {
            console.error("handleException:", exception);
            if(exception.message == null) {
                exception.message = "";
            }
            var message = null;
            
            /* handle connection lost */
            if( exception.code==550 || exception.code == 12029 || exception.code == 12019 || exception.code == 0 ||
                /* handle connection lost (this happens on windows only for some reason) */
                (exception.name == "JSONRpcClientException" && exception.fileName != null && exception.fileName.indexOf("jsonrpc") != -1) ||
                /* special text for "method not found" and "Service Temporarily Unavailable" */
                exception.message.indexOf("method not found") != -1 ||
                exception.message.indexOf("Service Unavailable") != -1 ||
                exception.message.indexOf("Service Temporarily Unavailable") != -1 ||
                exception.message.indexOf("This application is not currently available") != -1) {
                message  = i18n._("The connection to the server has been lost.") + "<br/>";
                message += i18n._("Press OK to return to the login page.") + "<br/>";
                
            }
            /* worst case - just say something */
            if (message == null) {
                if ( exception && exception.message ) {
                    message = i18n._("An error has occurred") + ":" + "<br/>"  + exception.message;
                } else {
                    message = i18n._("An error has occurred.");
                }
            }
            
            var details = "";
            if ( exception ) {
                if ( exception.javaStack )
                    exception.name = exception.javaStack.split('\n')[0]; //override poor jsonrpc.js naming
                if ( exception.name )
                    details += "<b>" + i18n._("Exception name") +":</b> " + exception.name + "<br/><br/>";
                if ( exception.code )
                    details += "<b>" + i18n._("Exception code") +":</b> " + exception.code + "<br/><br/>";
                if ( exception.message )
                    details += "<b>" + i18n._("Exception message") + ":</b> " + exception.message.replace(/\n/g, '<br/>') + "<br/><br/>";
                if ( exception.javaStack )
                    details += "<b>" + i18n._("Exception java stack") +":</b> " + exception.javaStack.replace(/\n/g, '<br/>') + "<br/><br/>";
                if ( exception.stack ) 
                    details += "<b>" + i18n._("Exception js stack") +":</b> " + exception.stack.replace(/\n/g, '<br/>') + "<br/><br/>";
                details +="<b>" + i18n._("Timestamp") +":&nbsp;</b>" + (new Date()).toString() + "<br/>";
            }
            Ext.MessageBox.alert(message, details);
           
            return true;
        }
        return false;
    },
    refreshTable: function( result, exception ) {
        Ext.MessageBox.hide();
        
        if(this.handleException(exception)) return;

        try {
            /* to refresh the buttons at the bottom */
            this.updateButtons(false);

            /* Reload the data */
            var store=this.grid.getStore();
            store.getProxy().data=store.refresh();
            
            store.load();

            var message = this.getMessage( result );
            if ( message != "" ) this.showMessage( message );

            /* Refresh the table */
            this.grid.setDisabled( false );

            /* Refresh the safelist table */
            if ( result.safelist != null ) {
                var sl = result.safelist;
                /* Build a new set of data */
                for ( var c = 0 ; c < sl.length ; c++ ) {
                    sl[c] = [sl[c]];
                }
                safelist.store.loadData( sl );
            }

        } catch ( e ) {
            alert( "Unable to refresh table: " + e );
        }
    },

    getMessage: function( result ) {
        var messages = [];
        if ( result.purgeCount > 0 ) {
            messages.push( i18n.pluralise( i18n._( "Deleted one Message" ),
                                           Ext.String.format( i18n._( "Deleted {0} Messages" ), result.purgeCount ),
                                           result.purgeCount ));
        }

        if ( result.releaseCount > 0 ) {
            messages.push( i18n.pluralise( i18n._( "Released one Message" ),
                                           Ext.String.format( i18n._( "Released {0} Messages" ), result.releaseCount ),
                                           result.releaseCount ));
        }

        if ( result.safelistCount > 0 ) {
            messages.push( i18n.pluralise( i18n._( "Safelisted one Address" ),
                                           Ext.String.format( i18n._( "Safelisted {0} Addresses" ), result.safelistCount ),
                                           result.safelistCount ));
        }

        return messages.join( "<br/>" );
    },

    updateButtons: function( enabled ) {
        this.releaseButton.setDisabled( !enabled );
        this.deleteButton.setDisabled( !enabled );
        this.safelistButton.setDisabled( !enabled );
    },

    showMessage: function( message ) {
        this.messageCount++;

        this.messageDisplayTip.add( Ext.create('Ext.form.Label',{ html: message + "<br/>" }));
        this.messageDisplayTip.show();

        setTimeout( Ext.bind(this.hideMessageTip, this ), 5000 );
    },

    hideMessageTip: function() {
        this.messageDisplayTip.remove( 0 );

        this.messageCount--;

        if ( this.messageCount <= 0 ) {
            this.messageDisplayTip.hide();
            this.messageCount = 0;
        } else {
            /* This updates the shadow */
            this.messageDisplayTip.show();
        }
    }
};

Ext.define('Ung.QuarantineModel', {
    extend:'Ext.data.Model',
    fields: [ 
      {name:'recipients'},
      {name:'mailID'},
      {name:'internDate'},
      {name:'size'},
      {name:'attachmentCount'},
      {name:'truncatedSender'},
      {name:'sender',
       sortType:Ext.data.SortTypes.asUCString}, 
      {name:'truncatedSubject'},
      {name:'subject'},
      {name:'quarantineCategory'},
      {name:'quarantineDetail', sortType:'asFloat'},
      {name:'quarantineSize'}]
});


Ext.define('Ung.QuarantineStore', {
    extend:'Ext.data.Store',
    constructor: function( config ) {
        config.model ='Ung.QuarantineModel';
        config.totalRecords=inboxDetails.totalCount;
        config.pageSize=config.quarantine.pageSize;
        config.proxy={
            model:'Ung.QuarantineModel',
            type: 'memory',
            reader: {
                type: 'json',
                root:'list'
            }
        };
        config.remoteSort=false;
        config.remoteFilter=false;
        config.data = this.refresh();
        config.sortOnLoad = true;
        config.sortRoot='data';

        config.sorters = {
            property: 'internDate',
            direction: "DESC"
        };
        Ung.QuarantineStore.superclass.constructor.apply(this, arguments);
        this.quarantine = config.quarantine;
    },
    refresh: function () {
        var data;
        var dataFn = Ext.bind( function () {
        	var mails = quarantine.rpc.getInboxRecords(inboxDetails.token); 
        	for(var i=0; i<mails.list.length; i++) {
                /* copy values from mailSummary to object */
                mails.list[i].truncatedSubject = mails.list[i].mailSummary.truncatedSubject;
                mails.list[i].subject = mails.list[i].mailSummary.subject;
                mails.list[i].sender = mails.list[i].mailSummary.sender;
                mails.list[i].quarantineCategory = mails.list[i].mailSummary.quarantineCategory;
                mails.list[i].quarantineDetail = mails.list[i].mailSummary.quarantineDetail;
            }
            return mails;
        }, this);
        try {
            data = dataFn();
            if(testMode) {
                var getTestRecord = function(index) {
                    return { 
                        recipients:'recipients'+index ,
                        sender: "sender"+(index%10)+"@test.com",
                        mailID: 'mailID'+index,
                        internDate: 10000*index,
                        size: 500*index,
                        attachmentCount: 1000-index,
                        quarantineDetail: parseFloat(index)/100,
                        truncatedSubject: "subject spam"+index
                    };
                };
                var length = Math.floor((Math.random()*5000));
                var start = parseInt(length/3);
                for(var i=start; i<length; i++) {
                    data.list.push(getTestRecord(i));
                }
                return data;
            }
        } catch ( exception) {
            var message = exception.message;
            if ( exception.name == "com.untangle.node.smtp.quarantine.NoSuchInboxException" ) {
                message = Ext.String.format( i18n._( "The account {0} doesn't have any quarantined messages." ),
                                             inboxDetails.address );
            }
            
            if (message == null || message == "Unknown") {
                message = i18n._("Please Try Again");
            }
            Ext.MessageBox.alert("Failed",message);
            return null;
        }

        return data;
    }
});

Ext.define('Ung.QuarantineSelectionModel', {
    extend:'Ext.selection.CheckboxModel',
    onSelectionChange: function(model, selected, options) {
        this.quarantine.updateButtons( selected.length>0 );
    },
    constructor: function( config ) {
        this.callParent(arguments);
        this.quarantine = config.quarantine;
        this.addListener('selectionchange', this.onSelectionChange, this );
    }
} );

Ext.define("Ung.GlobalFiltersFeature", {
    extend: "Ext.ux.grid.FiltersFeature",
    encode: false,
    local: true,
    init: function (grid) {
        Ext.applyIf(this,{
            globalFilter: {
                value: "",
                caseSensitive: false
                }
        });
        this.callParent(arguments);
    },
    getRecordFilter: function() {
        var me = this;
        var globalFilterFn = this.globalFilterFn;
        var parentFn = Ext.ux.grid.FiltersFeature.prototype.getRecordFilter.call(this);
        return function(record) {
            return parentFn.call(me, record) && globalFilterFn.call(me, record);
        };
    },
    updateGlobalFilter: function(value, caseSensitive) {
        if(caseSensitive !== null) {
            this.globalFilter.caseSensitive=caseSensitive;
        }
        if(!this.globalFilter.caseSensitive) {
            value=value.toLowerCase();
        }
        this.globalFilter.value = value;
            this.reload();
    },
    globalFilterFn: function(record) {
        //TODO: 1) support regular exppressions
        //2) provide option to search in displayed columns only
            var inputValue = this.globalFilter.value,
        caseSensitive = this.globalFilter.caseSensitive;
        if(inputValue.length === 0) {
            return true;
        }
        var fields = record.fields.items,
        fLen   = record.fields.length,
        f, val;

        for (f = 0; f < fLen; f++) {
            val = record.get(fields[f].name);
            if(val == null) {
                continue;
            }
            if(typeof val == 'boolean' || typeof val == 'number') {
                val=val.toString();
            } else if(typeof val == 'object') {
                if(val.javaClass =="java.util.Date") {
                    val = i18n.timestampFormat(val);
                }
            }
            if(typeof val == 'string') {
                if(caseSensitive) {
                    if(val.indexOf(inputValue) > -1) {
                        return true;
                    }
                } else {
                    if(val.toLowerCase().indexOf(inputValue) > -1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
});

Ext.define('Ung.QuarantineGrid', {
    extend:'Ext.grid.Panel',
    enableColumnHide: false,
    enableColumnMove: false,
    columnMenuDisabled: false,
    features: [{ftype: "filters"}],
    verticalScrollerType: 'paginggridscroller',
    plugins: {
        ptype: 'bufferedrenderer',
        trailingBufferZone: 20,  // Keep 20 rows rendered in the table behind scroll
        leadingBufferZone: 50   // Keep 50 rows rendered in the table ahead of scroll
    },
    constructor: function( config ) {
        this.quarantine = config.quarantine;

        config.columns = [
            {
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
            }];

        config.dockedItems= [{
            xtype: 'toolbar',
            dock: 'top',
            items: [ this.quarantine.releaseButton,
                     this.quarantine.safelistButton,
                     this.quarantine.deleteButton]            
        },{
            xtype: 'toolbar',
            dock: 'bottom',
            items: [i18n._('Filter:'), {
                xtype: 'textfield',
                name: 'searchField',
                hideLabel: true,
                width: 130,
                listeners: {
                    change: {
                        fn: function() {
                            this.filterFeature.updateGlobalFilter(this.searchField.getValue(), this.caseSensitive.getValue());
                        },
                        scope: this,
                        buffer: 600
                    }
                }
            }, {
                xtype: 'checkbox',
                name: 'caseSensitive',
                hideLabel: true,
                margin: '0 4px 0 4px',
                boxLabel: i18n._('Case sensitive'),
                handler: function() {
                    this.filterFeature.updateGlobalFilter(this.searchField.getValue(),this.caseSensitive.getValue());
                },
                scope: this
            }, {
                xtype: 'button',
                iconCls: 'icon-clear-filter',
                text: i18n._('Clear Filters'),
                tooltip: i18n._('Filters can be added by clicking on column headers arrow down menu and using Filters menu'),
                handler: Ext.bind(function () {
                    this.searchField.setValue("");
                    this.filters.clearFilters();
                }, this) 
            }]
        }];

        config.features = this.features;
        config.filterFeature=Ext.create('Ung.GlobalFiltersFeature', {});
        config.features.push(config.filterFeature);
        
        config.store = this.quarantine.store;
        config.selModel = this.quarantine.selectionModel;

        Ung.QuarantineGrid.superclass.constructor.apply(this, arguments);
    },
    
    initComponent: function() {
    	this.callParent(arguments);
        this.searchField=this.down('textfield[name=searchField]');
        this.caseSensitive = this.down('checkbox[name=caseSensitive]');
    },

    trackMouseOver:false,
    loadMask: true,
    frame: true,
    region: "center",
    stripeRows: true
});

Ext.define('Ung.QuarantineTabPanel', {
    extend:'Ext.tab.Panel',
    constructor: function( config ) {
        // Set the active tab to the forward address is mail is being forwarded.
        if ( inboxDetails.forwardAddress != "" ) config.activeTab = 2;
        this.callParent(arguments);
    },

    renderTo: "quarantine-tab-panel",
    width: '100%',
    height: 430,
    activeTab: 0,
    defaults: {
        border: false,
        bodyStyle: 'padding:4px 5px 0px 5px;'
    }
});


Ext.onReady(function() {
    // Initialize the I18n
    Ext.Ajax.request({
        url: 'i18n',
        success: function( response, options ) {
            i18n = new Ung.I18N({ map: Ext.decode( response.responseText )});
            completeInit();
        },
        method: "GET",
        failure: function() {
            Ext.MessageBox.alert("Error", "Unable to load the language pack." );
        },
        params: { module: 'untangle-casing-smtp' }
    });

});

function completeInit() {
    quarantine = new Ung.Quarantine();
    safelist = new Ung.Safelist();
    remaps = new Ung.Remaps();

    quarantine.init();
    safelist.init();
    remaps.init();

    var panels = [];

    var message = Ext.String.format( i18n._( "The messages below were quarantined and will be deleted after {0} days." ), inboxDetails.quarantineDays );
    panels.push( Ext.create('Ext.panel.Panel', {
        title: i18n._("Quarantined Messages" ),
        items: [ { xtype:'label', text: message, region: "north", cls:'message',ctCls:'message-container' ,margins:'4 4 4 4'} , quarantine.grid ],
        layout: "border"
    } ));

    message = i18n._( "You can use the Safe List to make sure that messages from these senders are never quarantined." );
    panels.push( Ext.create('Ext.panel.Panel',{
        title: i18n._("Safe List" ),
        items: [ { xtype:'label', text: message, region: "north", cls:'message',ctCls:'message-container' ,margins:'4 4 4 4' }, safelist.grid ],
        layout: "border"
    } ));
    panels.push( remaps.panel );

    quarantineTabPanel = Ext.create('Ung.QuarantineTabPanel', { items: panels, layout:'border'});
    quarantineTabPanel.setActiveTab(panels[0]);
}


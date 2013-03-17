Ext.namespace('Ung');
Ext.namespace('Ext.ux');

var testMode = false;
/**
 * @class Ext.ux.toolbar.PagingOptions
 * @author Arthur Kay (http://www.akawebdesign.com)
 * @namespace Ext.ux.toolbar
 * @extends Ext.toolbar.Paging
 * @constructor
 * @param {object} configObj
 */
Ext.define('Ext.ux.toolbar.PagingOptions', {
    extend: 'Ext.toolbar.Paging',

    getPagingItems: function() {
        var me = this,
            pagingButtons = me.callParent();

        if (!Ext.ModelManager.getModel('PageSize')) {
            Ext.define('PageSize', {
                extend: 'Ext.data.Model',
                fields: [{ name: 'pagesize' , type: 'int'}]
            });
        }

        if (!me.pageSizeOptions) {
            me.pageSizeOptions = [
                { pagesize: 25 },
                { pagesize: 100 },
                { pagesize: 1000 },
                { pagesize: 10000 }
            ];
        }

        pagingButtons.push({ xtype:'label', text:'Show '});
        pagingButtons.push({
            xtype          : 'combobox',
            queryMode      : 'local',
            triggerAction  : 'all',
            displayField   : 'pagesize',
            valueField     : 'pagesize',
            width          : 100,
            lazyRender     : true,
            enableKeyEvents: true,
            value          : me.pageSize,
            forceSelection : me.forceSelection || false,
            store: Ext.create('Ext.data.Store',{
                model: 'PageSize',
                data : me.pageSizeOptions
            }),

            listeners: {
                select: function(thisField, value) {
                    me.fireEvent('pagesizeselect', value[0].get('pagesize'));
                },
                keypress: function(thisField, eventObj) {
                    if (eventObj.getKey() !== eventObj.ENTER) { return false; }
                    me.fireEvent('pagesizeselect', thisField.getValue());
                    return true;
                }
            }
        });
        pagingButtons.push({xtype:'label', text:' rows/page'});

        return pagingButtons;
    },

    initComponent: function() {
        var me = this;
        me.callParent();
        me.addEvents('pagesizeselect');
    }
});

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
            text: i18n._( "Release to Inbox & Add Senders to Safelist" ),
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
    pageSize: 25,

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
        Ext.MessageBox.wait( i18n._("Releasing and adding Senders to Safelist...") , i18n._("Please wait"));
        if(addresses == null) {
            addresses = [];
        }
        var selections = this.grid.getSelectionModel().getSelection();
        Ext.each(selections, function(item) {
            addresses.push(item.data.sender);
        });
        this.grid.getSelectionModel().deselectAll();
        this.grid.setDisabled( true );
        Ext.Function.defer(function() {
            this.rpc.safelist( Ext.bind(this.refreshTable, this ), inboxDetails.token, addresses );
        }, 1 ,this);
    },

    refreshTable: function( result, exception ) {
        Ext.MessageBox.hide();
        if ( exception ) {
            var message = exception.message;
            if (message == null || message == "Unknown") {
                message = i18n._("Please Try Again");
            }
            
            Ext.MessageBox.alert("Failed",message);
            return;
        }

        try {
            /* to refresh the buttons at the bottom */
            this.updateButtons(false);

            /* Reload the data */
            var store=this.grid.getStore();
            store.getProxy().data=store.refresh();
            
            if(store.currentPage>1) {
                //reset page index if all records from the current page where deleted
                var dataLength=store.getProxy().data.list.length;
                if(store.currentPage > Math.ceil(dataLength/store.pageSize)) {
                    store.currentPage=1;
                }
            }
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
}

Ext.define('Ung.QuarantineModel', {
    extend:'Ext.data.Model',
    fields: [ 
      {name:'recipients'},
      {name:'mailID'},
      {name:'quarantinedDate', sortType:function(value) { return value.time;}},
      {name:'size'},
      {name:'attachmentCount'},
      {name:'truncatedSender'},
      {name:'sender'}, 
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
            type: 'pagingmemory',
            reader: {
                type: 'json',
                root:'list'
            }
        };
        config.remoteSort=true;
        config.data = this.refresh();

        Ung.QuarantineStore.superclass.constructor.apply(this, arguments);
        this.quarantine = config.quarantine;
    },

    refresh: function () {
        var dataFn = Ext.bind( function () {
            return quarantine.rpc.getInboxRecords(inboxDetails.token, 0, inboxDetails.totalCount, null, false); 
        }, this);
        try {
            var data = dataFn();
            if(testMode) {
                var getTestRecord = function(index) {
                    return { 
                        recipients:'recipients'+index ,
                        sender: "sender"+(index%10)+"@test.com",
                        mailID: 'mailID'+index,
                        quarantinedDate: {time: 10000*index},
                        size: 500*index,
                        attachmentCount: 1000-index,
                        quarantineDetail: parseFloat(index)/100,
                        truncatedSubject: "subject spam"+index
                    }
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

Ext.define('Ung.QuarantineGrid', {
    extend:'Ext.grid.Panel',
    enableColumnHide: false,
    enableColumnMove: false,
    constructor: function( config ) {
        this.quarantine = config.quarantine;

        config.columns = [
            {
                header: i18n._( "From" ),
                dataIndex: 'sender',
                menuDisabled: true,
                width: 250
            },{
                header: "<div class='quarantine-attachment-header'>&nbsp</div>",
                dataIndex: 'attachmentCount',
                width: 60,
                tooltip: i18n._( "Number of Attachments in the email." ),
                menuDisabled: true,
                align: 'center'
            },{
                header: i18n._( "Score" ),
                dataIndex: 'quarantineDetail',
                width: 60,
                menuDisabled: true,
                align: 'center'
            },{
                header: i18n._( "Subject" ),
                dataIndex: 'truncatedSubject',
                flex: 1,
                menuDisabled: true,
                width: 250
            },{
                header: i18n._( "Date" ),
                dataIndex: 'quarantinedDate',
                menuDisabled: true,
                width: 135,
                renderer: function( value ) {
                    var date = new Date();
                    date.setTime( value.time );
                    d = Ext.util.Format.date( date, 'm/d/Y' );
                    t = Ext.util.Format.date( date, 'g:i a' );
                    return d + ' ' + t;
                }
            },{
                header: i18n._( "Size (KB)" ),
                dataIndex: 'size',
                menuDisabled: true,
                renderer: function( value ) {
                    return Math.round( (( value + 0.0 ) / 1024) * 10 ) / 10;
                },
                width: 60
            }];

        config.bbar = Ext.create('Ext.ux.toolbar.PagingOptions',{
            pageSize: this.quarantine.pageSize,
            store: this.quarantine.store,
            displayInfo: true,
            displayMsg: i18n._( 'Showing items {0} - {1} of {2}' ),
            emptyMsg: i18n._( 'No messages to display' )
        });
        config.bbar.addListener('pagesizeselect',Ext.bind(this.onPageSizeSelect, this));
        config.dockedItems= [{
            xtype: 'toolbar',
            dock: 'top',
            items: [ this.quarantine.releaseButton,
                     this.quarantine.safelistButton,
                     this.quarantine.deleteButton]            
        }];

        config.store = this.quarantine.store;
        config.selModel = this.quarantine.selectionModel;

        Ung.QuarantineGrid.superclass.constructor.apply(this, arguments);
    },
    onPageSizeSelect: function(value) {
        quarantine.store.pageSize=value;
        quarantine.pageSize=value;
        quarantine.store.currentPage=1;
        quarantine.store.load({params:{start:0, limit:value}});
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

    message = i18n._( "You can use the Safelist to make sure that messages from these senders are never quarantined." );
    panels.push( Ext.create('Ext.panel.Panel',{
        title: i18n._("Safelist" ),
        items: [ { xtype:'label', text: message, region: "north", cls:'message',ctCls:'message-container' ,margins:'4 4 4 4' }, safelist.grid ],
        layout: "border"
    } ));
    panels.push( remaps.panel );

    quarantineTabPanel = Ext.create('Ung.QuarantineTabPanel', { items: panels, layout:'border'});
    quarantineTabPanel.setActiveTab(panels[0]);
}


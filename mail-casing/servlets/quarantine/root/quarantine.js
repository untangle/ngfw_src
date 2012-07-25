Ext.namespace('Ung');
Ext.namespace('Ext.ux');

//The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/ext/resources/images/default/s.gif';

// vim: ts=4:sw=4:nu:fdc=4:nospell

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

        pagingButtons.push({xtype:'label', text:'Show '});
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
// end of file

Ext.define('Ung.SimpleHash', {
    constructor:function() {
        this.data = new Object();
        this.size = 0;
    },
    /**
     * Add an item to internal hash.
     * @param key The key to update
     * @param value The value to insert, use null to remove the item.
     */
    put: function( key, value ) {
        if ( this.data[key] != null ) {
            if ( value == null ) this.size--;
        } else {
            if ( value != null ) this.size++;
        }

        if ( value == null ) delete this.data[key];
        else this.data[key] = value;
    },

    clear: function( key ) {
        if ( this.data[key] != null ) this.size--;
        this.data[key] = null;
    },

    clearAll: function() {
        this.data = {};
        this.size = 0;
    },

    get: function( key ) {
        return this.data[key];
    }
});

Ext.define('Ung.CountingHash', {
    extend:'Ung.SimpleHash',
    add: function( key ) {
        var current = this.get( key );

        if ( current == null ) {
            current = 1;
        } else {
            current++;
        }

        return this.put( key, current );
    },

    minus: function( key ) {
        var current = this.get( key );

        if ( current != null ) {
            if ( current <= 1 ) {
                current = null;
            } else {
                current--;
            }
        }

        return this.put( key, current );
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

    /* This is a hash of message ids that are ready to be deleted or released. */
    actionItems: new Ung.SimpleHash(),

    /* This is a hash of the email addresses to safelist */
    addresses: new Ung.CountingHash(),

    init: function() {
        //get JSONRpcClient
        this.rpc = new JSONRpcClient("/quarantine/JSON-RPC").Quarantine;
        this.store = new Ung.QuarantineStore( { quarantine: this } );
        this.selectionModel = new Ung.QuarantineSelectionModel( { quarantine: this } );

        this.releaseButton= Ext.create('Ext.button.Button', {
            handler: Ext.bind(function() { this.releaseOrDelete( quarantine.rpc.releaseMessages ); }, this ),
            iconCls: 'icon-move-mails',
            text: i18n._( "Move to Inbox (0  messages)" ),
            disabled: true
        } );

        this.safelistButton = Ext.create('Ext.button.Button', {
            handler: Ext.bind(function() { this.safelist(); }, this ),
            iconCls:'icon-safe-list',
            text: i18n._( "Move to Inbox & Add to Safelist (0  Senders)" ),
            disabled: true
        } );

        this.deleteButton = Ext.create('Ext.button.Button', {
            handler: Ext.bind(function() { this.releaseOrDelete( quarantine.rpc.purgeMessages ); }, this ),
            iconCls:'icon-delete-row',
            text: i18n._( "Delete (0  messages)" ),
            disabled: true
        } );

        // This is used to show messages when they are available
        this.messageDisplayTip = Ext.create('Ext.tip.Tip',{
            cls: 'action-messages'
        });

        this.messageCount = 0;

        this.hideTimeout = null;

        this.grid = Ext.create('Ung.QuarantineGrid',{ quarantine: this } );
    },

    store: null,

    selectionModel: null,

    grid: null,

    pageSize: 25,

    clearSelections: function() {
        // Clear the current action items 
        this.actionItems.clearAll();
        this.addresses.clearAll();
        this.selectionModel.deselectAll();
        this.grid.setDisabled( true );
    },

    releaseOrDelete: function( action ) {
        var mids = [];
        for ( var key in this.actionItems.data )   {
            mids.push( key );
        }
        this.store.remove(this.selectionModel.getSelection());

        this.clearSelections();
        action( Ext.bind(this.refreshTable, this), inboxDetails.token, mids );
    },

    safelist: function( addresses )
    {
        if ( addresses == null ) {
            addresses = [];
            for ( var key in this.addresses.data ) addresses.push( key );
        }
        this.selectionModel.selectAll();
        this.store.remove(this.selectionModel.getSelection());

        this.clearSelections();
        this.rpc.safelist( Ext.bind(this.refreshTable, this ), inboxDetails.token, addresses );
    },

    refreshTable: function( result, exception ) {
        if ( exception ) {
            var message = exception.message;
            if (message == null || message == "Unknown") {
                message = i18n._("Please Try Again");
            }
            
            Ext.MessageBox.alert("Failed",message);
            return;
        }

        try {
            /* Update the new total number of records */
            // this.store.proxy.setTotalRecords( result.totalRecords );
            
            this.store.sync();

            /* to refresh the buttons at the bottom */
            this.updateActionItem( null, null, false );

            /* Reload the data */
            // this.grid.bbar.doLoad( 0 );

            var message = this.getMessage( result );
            if ( message != "" ) this.showMessage( message );

            /* Refresh the table */
            this.grid.setDisabled( false );

            /* Refresh the safelist table */
            if ( result.safelist != null ) {
                var sl = result.safelist;
                /* Build a new set of data */
                for ( var c = 0 ; c < sl.length ; c++ ) sl[c] = [sl[c]];
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
                                           Ext.String.format( i18n._( "Deleted {0} Messages" ),
                                                              result.purgeCount ),
                                           result.purgeCount ));
        }

        if ( result.releaseCount > 0 ) {
            messages.push( i18n.pluralise( i18n._( "Released one Message" ),
                                           Ext.String.format( i18n._( "Released {0} Messages" ),
                                                              result.releaseCount ),
                                           result.releaseCount ));
        }

        if ( result.safelistCount > 0 ) {
            messages.push( i18n.pluralise( i18n._( "Safelisted one Address" ),
                                           Ext.String.format( i18n._( "Safelisted {0} Addresses" ),
                                                              result.safelistCount ),
                                           result.safelistCount ));
        }

        return messages.join( "<br/>" );
    },

    updateActionItem: function( mailid, address, ifAdd ) {
        var value = true;
        if ( !ifAdd ) value = null;
        if ( mailid != null ) this.actionItems.put( mailid, value );
        if ( address != null ) {
            if ( ifAdd ) this.addresses.add( address );
            else this.addresses.minus( address );
        }

            var deleteText;
            var releaseText;
        var safelistText;

        var count = this.actionItems.size;
        if ( count == 0 ) {
            deleteText = i18n._( "Delete (0  messages)" );
            releaseText = i18n._( "Move to Inbox (0  messages)" );
            this.releaseButton.setDisabled( true );
            this.deleteButton.setDisabled( true );
        } else if ( count == 1 ) {
            deleteText = i18n._( "Delete (1  message)" );
            releaseText = i18n._( "Move to Inbox (1  message)" );
            this.releaseButton.setDisabled( false );
            this.deleteButton.setDisabled( false );
        } else {
            deleteText = Ext.String.format( i18n._( "Delete ({0} messages)" ), count );
            releaseText = Ext.String.format( i18n._( "Move to Inbox ({0} messages)" ), count );
            this.releaseButton.setDisabled( false );
            this.deleteButton.setDisabled( false );
        }

        count = this.addresses.size;
        if ( count == 0 ) {
            safelistText = i18n._( "Move to Inbox & Add to Safelist (0  Senders)" );
            this.safelistButton.setDisabled( true );
        } else if ( count == 1 ) {
            safelistText = i18n._( "Move to Inbox & Add to Safelist (1  Sender)" );
            this.safelistButton.setDisabled( false );
        } else {
            safelistText = Ext.String.format( i18n._( "Move to Inbox & Add to Safelist ({0} Senders)" ), count );
            this.safelistButton.setDisabled( false );
        }

        this.releaseButton.setText( releaseText );
        this.safelistButton.setText( safelistText );
        this.deleteButton.setText( deleteText );
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
        var dataFn = Ext.bind( function () {
            return quarantine.rpc.getInboxRecords(inboxDetails.token, 0, inboxDetails.totalCount, null, false); 
        }, this);
        try {
            config.data = dataFn();
        } catch ( exception) {
            var message = exception.message;
            if ( exception.name == "com.untangle.node.mail.papi.quarantine.NoSuchInboxException" ) {
                message = Ext.String.format( i18n._( "The account {0} doesn't have any quarantined messages." ),
                                             inboxDetails.address );
            }
            
            if (message == null || message == "Unknown") {
                message = i18n._("Please Try Again");
            }
            Ext.MessageBox.alert("Failed",message);
        }
        Ung.QuarantineStore.superclass.constructor.apply(this, arguments);
        this.quarantine = config.quarantine;
    }
});

Ext.define('Ung.QuarantineSelectionModel', {
    extend:'Ext.selection.CheckboxModel',
    onSelectionChange:function(model, selected, options) {
        Ext.each(this.lastSelectedRecords, Ext.bind(function(record) {
            this.quarantine.updateActionItem( record.data.mailID, record.data.sender, false);
            return true;
        }, this));
        Ext.each(selected, Ext.bind(function(record) {
            this.quarantine.updateActionItem( record.data.mailID, record.data.sender, true );
            return true;
        }, this));
        this.lastSelectedRecords=selected;
    },

    constructor: function( config ) {
        Ung.QuarantineSelectionModel.superclass.constructor.apply(this, arguments);
        this.lastSelectedRecords=[];
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
        params: { module: 'untangle-casing-mail' }
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


Ext.namespace('Ung');
Ext.namespace('Ext.ux');


//The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/ext/resources/images/default/s.gif';

// vim: ts=4:sw=4:nu:fdc=4:nospell
/**
  * Page Size Plugin for Paging Toolbar
  *
  * @author    rubensr, http://extjs.com/forum/member.php?u=13177
  * @see       http://extjs.com/forum/showthread.php?t=14426
  * @author    Ing. Jozef Sakalos, modified combo for editable, enter key handler, config texts
  * @date      27. January 2008
  * @version   $Id: Ext.ux.PageSizePlugin.js 82 2008-03-21 00:17:40Z jozo $
  * @package   perseus
  */

/*global Ext */
Ext.ux.PageSizePlugin = function(config) {
    Ext.apply(this, config);

    Ext.ux.PageSizePlugin.superclass.constructor.call(this, {
        store: new Ext.data.SimpleStore({
            fields: ['text', 'value'],
            data: [['25', 25], ['100', 100], ['1000', 1000], ['All', "all"]]
        }),
        mode: 'local',
        displayField: 'text',
        valueField: 'value',
        allowBlank: false,
        triggerAction: 'all',
        width: 60,
        listWidth : 50,
        maskRe: /[0-9]/
    });
};

Ext.extend(Ext.ux.PageSizePlugin, Ext.form.ComboBox, {
    beforeText:'Show',
    afterText:'rows/page',
    init: function(paging) {
        paging.on('render', this.onInitView, this);
    },

    onInitView: function(paging) {
        paging.add('-', this.beforeText, this, this.afterText);
        this.setValue(paging.pageSize);
        this.on('select', this.onPageSizeChanged, paging);
        this.on('specialkey', function(combo, e) {
            if(13 === e.getKey()) {
                this.onPageSizeChanged.call(paging, this);
            }
        });

    },

    onPageSizeChanged: function(combo) {
        var value = combo.getValue();
        if ( value == "all" ) {
            value = this.store.totalLength;
        } else {
            value = parseInt( value, 10 );
        }

        this.pageSize = value;
        this.doLoad(0);
    }
});

// end of file

Ung.SimpleHash = function()
{
    this.data = new Object();
    this.size = 0;
}

Ung.SimpleHash.prototype = {
    /**
     * Add an item to internal hash.
     * @param key The key to update
     * @param value The value to insert, use null to remove the item.
     */
    put : function( key, value )
    {
        if ( this.data[key] != null ) {
            if ( value == null ) this.size--;
        } else {
            if ( value != null ) this.size++;
        }

        if ( value == null ) delete this.data[key];
        else this.data[key] = value;
    },

    clear : function( key )
    {
        if ( this.data[key] != null ) this.size--;
        this.data[key] = null;
    },

    clearAll : function()
    {
        this.data = {};
        this.size = 0;
    },

    get : function( key )
    {
        return this.data[key];
    }
}

Ung.CountingHash = Ext.extend( Ung.SimpleHash, {
    add : function( key ) {
        var current = this.get( key );

        if ( current == null ) current = 1;
        else current++;

        return this.put( key, current );
    },

    minus : function( key ) {
        var current = this.get( key );

        if ( current != null ) {
            if ( current <= 1 ) current = null;
            else current--;
        }

        return this.put( key, current );
    }
} );


// uses json rpc to get the information from the server
// @param rpcFn Remote JSON call to retrieve results.
// @param paginated True if this table is paginated.
// @param totalRecords The total number of records for the table.
// also have to return it.
Ung.QuarantineProxy = function(rpcFn, paginated, totalRecords){
    Ung.QuarantineProxy.superclass.constructor.call(this);
    this.rpcFn = rpcFn;
    this.totalRecords = totalRecords;

    //specified if we fetch data paginated or all at once
    //default to true
    if (paginated===undefined){
        this.paginated = true;
    } else {
        this.paginated = paginated;
    }
};

Ext.extend(Ung.QuarantineProxy, Ext.data.DataProxy, {
    setTotalRecords : function( totalRecords )
    {
        this.totalRecords = totalRecords;
    },

    //load function for Proxy class
    load : function(params, reader, callback, scope, arg)
    {
        var obj={};
        obj.params=params;
        obj.reader=reader;
        obj.callback=callback;
        obj.scope=scope;
        obj.arg=arg;
        obj.totalRecords=this.totalRecords;
        var sortColumns=[];

        var token = inboxDetails.token;
        var start = params.start ? params.start : 0;
        var limit = params.limit ? params.limit : -1;
        var sort = params.sort ? params.sort : null;
        var isAscending = params.dir == "ASC";
        if (this.paginated){
        this.rpcFn(this.errorHandler.createDelegate(obj), token, start, limit, sort, isAscending );
        } else {
            this.rpcFn(this.errorHandler.createDelegate(obj));
        }
    },

    errorHandler: function (result, exception)
    {
        if(exception) {
            var message = exception.message;
            if ( exception.name == "com.untangle.node.mail.papi.quarantine.NoSuchInboxException" ) {
                message = String.format( i18n._( "The account {0} doesn't have any quarantined messages." ),
                                         inboxDetails.address );
            }
            
            if (message == null || message == "Unknown") {
                message = i18n._("Please Try Again");
            }

            Ext.MessageBox.alert("Failed",message);
            this.callback.call(this.scope, null, this.arg, true);
            return;
        }
        var res=null;
        try {
            res= this.reader.readRecords(result);
            if(this.totalRecords) {
                res.totalRecords=this.totalRecords;
            }
            this.callback.call(this.scope, res, this.arg, true);
        }catch(e){
            this.callback.call(this.scope, null, this.arg, true);
            return;
        }
    }
});

var quarantineTabPanel = null;
var quarantine = null;
var safelist = null;
var remaps = null;

var i18n = null;

Ung.Quarantine = function() {
}

Ung.Quarantine.prototype = {
    rpc : null,

    /* This is a hash of message ids that are ready to be deleted or released. */
    actionItems : new Ung.SimpleHash(),

    /* This is a hash of the email addresses to safelist */
    addresses : new Ung.CountingHash(),

    init: function() {
    //get JSONRpcClient
        //
    this.rpc = new JSONRpcClient("/quarantine/JSON-RPC").Quarantine;
        this.store = new Ung.QuarantineStore( { quarantine : this } );
        this.selectionModel = new Ung.QuarantineSelectionModel( { quarantine : this } );

        this.releaseButton= new Ext.Button( {
            handler : function() { this.releaseOrDelete( quarantine.rpc.releaseMessages ) }.createDelegate( this ),
            iconCls : 'icon-move-mails',
            text : i18n._( "Move to Inbox (0  messages)" ),
            disabled : true
        } );

        this.safelistButton = new Ext.Button( {
            handler : function() { this.safelist() }.createDelegate( this ),
            iconCls:'icon-safe-list',
            text : i18n._( "Move to Inbox & Add to Safelist (0  Senders)" ),
            disabled : true
        } );

        this.deleteButton = new Ext.Button( {
            handler : function() { this.releaseOrDelete( quarantine.rpc.purgeMessages ) }.createDelegate( this ),
            iconCls:'icon-delete-row',
            text : i18n._( "Delete (0  messages)" ),
            disabled : true
        } );

        /* This is used to show messages when they are available */
        this.messageDisplayTip = new Ext.Tip({
            cls : 'action-messages',
            layout : 'form'
        });

        this.messageCount = 0;

        this.hideTimeout = null;

        this.grid = new Ung.QuarantineGrid( { quarantine : this } );
    },

    store : null,

    selectionModel : null,

    grid : null,

    pageSize : 25,

    clearSelections : function()
    {
        /* Clear the current action items */
        this.actionItems.clearAll();
        this.addresses.clearAll();
        this.selectionModel.clearSelections();
        this.grid.setDisabled( true );
        if ( Ext.fly(this.grid.getView().getHeaderCell(0)).first().hasClass('x-grid3-hd-checker-on')){
            Ext.fly(this.grid.getView().getHeaderCell(0)).first().removeClass('x-grid3-hd-checker-on');
        }

    },

    releaseOrDelete : function( action ) {
        var mids = [];
        for ( var key in this.actionItems.data ) mids.push( key );

        this.clearSelections();

        action( this.refreshTable.createDelegate(this), inboxDetails.token, mids );
    },

    safelist : function( addresses )
    {
        if ( addresses == null ) {
            addresses = [];
            for ( var key in this.addresses.data ) addresses.push( key );
        }

        this.clearSelections();
        this.rpc.safelist( this.refreshTable.createDelegate( this ), inboxDetails.token, addresses );
    },

    refreshTable : function( result, exception )
    {
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
            this.store.proxy.setTotalRecords( result.totalRecords );

            /* to refresh the buttons at the bottom */
            this.updateActionItem( null, null, false );

            /* Reload the data */
            this.grid.bottomToolbar.doLoad( 0 );

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

    getMessage : function( result )
    {
        var messages = [];
        if ( result.purgeCount > 0 ) {
            messages.push( i18n.pluralise( i18n._( "Deleted one Message" ),
                                          String.format( i18n._( "Deleted {0} Messages" ),
                                                         result.purgeCount ),
                                           result.purgeCount ));
        }

        if ( result.releaseCount > 0 ) {
            messages.push( i18n.pluralise( i18n._( "Released one Message" ),
                                          String.format( i18n._( "Released {0} Messages" ),
                                                         result.releaseCount ),
                                          result.releaseCount ));
        }

        if ( result.safelistCount > 0 ) {
            messages.push( i18n.pluralise( i18n._( "Safelisted one Address" ),
                                          String.format( i18n._( "Safelisted {0} Addresses" ),
                                                         result.safelistCount ),
                                          result.safelistCount ));
        }

        return messages.join( "<br/>" );
    },

    updateActionItem : function( mailid, address, ifAdd )
    {
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
            deleteText = String.format( i18n._( "Delete ({0} messages)" ), count );
            releaseText = String.format( i18n._( "Move to Inbox ({0} messages)" ), count );
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
            safelistText = String.format( i18n._( "Move to Inbox & Add to Safelist ({0} Senders)" ), count );
            this.safelistButton.setDisabled( false );
        }

        this.releaseButton.setText( releaseText );
        this.safelistButton.setText( safelistText );
        this.deleteButton.setText( deleteText );
    },

    showMessage : function( message )
    {
        this.messageCount++;

        this.messageDisplayTip.add( new Ext.form.Label({ html : message + "<br/>" }));
        this.messageDisplayTip.show();

        setTimeout( this.hideMessageTip.createDelegate( this ), 5000 );
    },

    hideMessageTip : function()
    {
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

Ung.QuarantineStore = Ext.extend( Ext.data.Store, {
    constructor : function( config ) {
        Ung.QuarantineStore.superclass.constructor.apply(this, arguments);

        this.quarantine = config.quarantine;
        this.addListener('load',this.onLoad, this );
        // load using script tags for cross domain, if the data in on the same domain as
        // this page, an HttpProxy would be better
        this.proxy = new Ung.QuarantineProxy( quarantine.rpc.getInboxRecords, true, inboxDetails.totalCount );
    },

    proxy : null,

    // create reader that reads the Topic records
    reader: new Ext.data.JsonReader({
            root: 'list',
            totalProperty: 'totalRecords',
            fields: [ 'recipients',
                      'mailID',
                      'quarantinedDate',
                      'size',
                      'attachmentCount',
                      'truncatedSender',
                      'sender',
                      'truncatedSubject',
                      'subject',
                      'quarantineCategory',
                      'quarantineDetail',
                      'quarantineSize'
            ]
     }),

    // turn on remote sorting
    remoteSort: true,

    onLoad : function( store, records, options ) {
        /* now update the selection model? */
        var rows = [];

        for ( var c= 0 ; c < records.length ; c++ ) {
            var record = records[c];

            if ( this.quarantine.actionItems.get( record.data.mailID ) == true ) rows.push( c );
        }

        this.quarantine.selectionModel.selectRows( rows );
    }
} );

Ung.QuarantineSelectionModel = Ext.extend(  Ext.grid.CheckboxSelectionModel, {

    onRowSelect : function( sm, rowIndex, record ) {
        this.quarantine.updateActionItem( record.data.mailID, record.data.sender, true );
    },

    onRowDeselect : function( sm, rowIndex, record ) {
        this.quarantine.updateActionItem( record.data.mailID, record.data.sender, false );
    },

    constructor : function( config ) {
        Ung.QuarantineSelectionModel.superclass.constructor.apply(this, arguments);

        this.quarantine = config.quarantine;

        this.addListener('rowselect',this.onRowSelect, this );
        this.addListener('rowdeselect',this.onRowDeselect, this );
    }
} );

Ung.QuarantineGrid = Ext.extend( Ext.grid.GridPanel, {
    constructor : function( config ) {
        this.quarantine = config.quarantine;

        config.cm = new Ext.grid.ColumnModel([
            this.quarantine.selectionModel,
            {
                header: i18n._( "From" ),
                dataIndex: 'sender',
                width: 150
            },{
                header: "<div class='quarantine-attachment-header'>&nbsp</div>",
                dataIndex: 'attachmentCount',
                width: 60,
                tooltip : i18n._( "Number of Attachments in the email." ),
                align : 'center'
            },{
                header: i18n._( "Score" ),
                dataIndex: 'quarantineDetail',
                width: 60,
                align : 'center'
            },{
                header: i18n._( "Subject" ),
                dataIndex: 'truncatedSubject',
                width: 250
            },{
                header: i18n._( "Date" ),
                dataIndex: 'quarantinedDate',
                width: 135,
                renderer : function( value ) {
                    var date = new Date();
                    date.setTime( value.time );
                    d = Ext.util.Format.date( date, 'm/d/Y' );
                    t = Ext.util.Format.date( date, 'g:i a' );
                    return d + ' ' + t;
                }
            },{
                header: i18n._( "Size (KB)" ),
                dataIndex: 'size',
                renderer : function( value ) {
                    return Math.round( (( value + 0.0 ) / 1024) * 10 ) / 10;
                },
                width: 60
            }]);

        config.cm.defaultSortable = true;

        config.bbar = new Ext.PagingToolbar({
            pageSize: this.quarantine.pageSize,
            store: this.quarantine.store,
            displayInfo: true,
            displayMsg: i18n._( 'Showing items {0} - {1} of {2}' ),
            emptyMsg: i18n._( 'No messages to display' ),
            plugins : [new Ext.ux.PageSizePlugin()]
        });

        config.tbar = new Ext.Toolbar({
            items: [ this.quarantine.releaseButton,
                     this.quarantine.safelistButton,
                     this.quarantine.deleteButton]
        });

        config.store = this.quarantine.store;
        config.sm = this.quarantine.selectionModel;

        Ung.QuarantineGrid.superclass.constructor.apply(this, arguments);
    },
    trackMouseOver:false,
    loadMask: true,
    frame : true,
    region : "center",
    stripeRows : true,
    autoExpandColumn : 4

});

Ung.QuarantineTabPanel = Ext.extend( Ext.TabPanel, {
    constructor : function( config )
    {
        /* Set the active tab to the forward address is mail is being forwarded. */
        if ( inboxDetails.forwardAddress != "" ) config.activeTab = 2;
        Ung.QuarantineTabPanel.superclass.constructor.apply(this, arguments);
    },

    el : "quarantine-tab-panel",
    width : '100%',
    height : 430,
    activeTab : 0,
    layoutOnTabChange : true,
    /* defaults : { autoHeight : true }, */
    frame : true,
    defaults : {
        border : false,
        bodyStyle : 'padding:4px 5px 0px 5px;'
    }
});


Ext.onReady(function() {
    // Initialize the I18n
    Ext.Ajax.request({
        url : 'i18n',
            success : function( response, options ) {
            i18n = new Ung.I18N({ map : Ext.decode( response.responseText )});
            completeInit();
        },
        method : "GET",
        failure : function() {
            Ext.MessageBox.alert("Error", "Unable to load the language pack." );
        },
        params : { module : 'untangle-casing-mail' }
    });

});

function completeInit()
{
    quarantine = new Ung.Quarantine();
    safelist = new Ung.Safelist();
    remaps = new Ung.Remaps();

    quarantine.init();
    safelist.init();
    remaps.init();

    var message = String.format( i18n._( "The messages below were quarantined and will be deleted after {0} days." ), inboxDetails.quarantineDays );

    var panels = [];

    panels.push( new Ext.Panel( {
        title : i18n._("Quarantined Messages" ),
        items : [ new Ext.form.Label( { text : message, region : "north", cls:'message',ctCls:'message-container' ,margins:'4 4 4 4'} ), quarantine.grid ],
        layout : "border"
    } ));

    message = i18n._( "You can use the Safelist to make sure that messages from these senders are never quarantined." );

    panels.push( new Ext.Panel( {
        title : i18n._("Safelist" ),
        items : [ new Ext.form.Label( { text : message, region : "north", cls:'message',ctCls:'message-container' ,margins:'4 4 4 4' } ), safelist.grid ],
        layout : "border"
    } ));

    panels.push( remaps.panel );

    quarantineTabPanel = new Ung.QuarantineTabPanel( { items : panels } );

    quarantineTabPanel.render();

    quarantineTabPanel.activate(panels[0]);
    // trigger the data store load
    quarantine.store.load({params:{start:0, limit:quarantine.pageSize}});
}


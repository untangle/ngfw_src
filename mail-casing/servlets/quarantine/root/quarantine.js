Ext.namespace('Ung');

//The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/webui/ext/resources/images/default/s.gif';

Ung.SimpleHash = function()
{
    this.data = new Object();
    this.size = 0;
}

Ung.SimpleHash.prototype = {
    put : function( key, value )
    {
        if ( this.data[key] != null ) {
            if ( value == null ) this.size--;
        } else {
            if ( value != null ) this.size++;
        }

        this.data[key] = value;
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
    setTotalRecords : function( totalRecords ) {
        this.totalRecords = totalRecords;
    },

	//load function for Proxy class
    load : function(params, reader, callback, scope, arg) {
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

	errorHandler: function (result, exception) {
        if(exception) {
            Ext.MessageBox.alert("Failed",exception.message); 
            this.callback.call(this.scope, null, this.arg, false);
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
            this.callback.call(this.scope, null, this.arg, false);
            return;
        }
	}
});

var quarantine = null;

var i18n = null;

Ung.Quarantine = function() {
}

Ung.Quarantine.prototype = {
    disableThreads: true, // in development environment is useful to disable threads.
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
            handler : function() { quarantine.releaseOrDelete( quarantine.rpc.releaseMessages,
                                                               [ i18n._( "Release Messages" ),
                                                                 i18n._( "Release one Message" ),
                                                                 i18n._( "Release %d Messages" ) ] ) },
                text : i18n._( "Release Messages" ),
                    disabled : true
                    } );

        this.deleteButton = new Ext.Button( {
            handler : function() { quarantine.releaseOrDelete( quarantine.rpc.purgeMessages, 
                                                               [ i18n._( "Delete Messages" ),
                                                                 i18n._( "Delete one Message" ),
                                                                 i18n._( "Delete %d Messages" ) ]); },
            text : i18n._( "Delete Messages" ),
            disabled : true
                } );
        
        this.safelistButton = new Ext.Button( {
            handler : function() { quarantine.safelist() },
            text : i18n._( "Safelist Messages" ),
            disabled : true
                } );

        this.grid = new Ung.QuarantineGrid( { quarantine : this } );
    },

    store : null, 

    selectionModel : null,

    grid : null,

    pageSize : 25,

    clearSelections : function()
    {
        /* Clear the current action items */
        quarantine.actionItems.clearAll();
        quarantine.addresses.clearAll();
        quarantine.selectionModel.clearSelections();
        quarantine.grid.setDisabled( true );
    },
    
    releaseOrDelete : function( action, messages ) {
        var mids = [];
        for ( var key in quarantine.actionItems.data ) mids.push( key );

        this.clearSelections();
        
        var getMessage = function( result, messages ) {
            var message = messages[1];
            var count = quarantine.store.proxy.totalRecords - result;
            if( count <= 0 ) message = "";
            else if ( count == 1 ) message = messages[0];
            return i18n.sprintf( message, count );
        };
        
        var getCount = function( result ) { return result; };
        action( this.refreshTable.createDelegate(quarantine, [ messages, getCount, getMessage ], true ), 
                inboxDetails.token, mids );
    },
    
    safelist : function()
    {
        var addresses = [];
        for ( var key in quarantine.addresses.data ) addresses.push( key );

        this.clearSelections();
        var getMessage = function( result, messages ) {
            var safelist = i18n.pluralise( i18n._( "Safelisted one addressess" ), i18n.sprintf( i18n._( "Safelisted %d addresses" ), result.safelistCount ), result.safelistCount );

            var count = quarantine.store.proxy.totalRecords - result.totalRecords;
            var release = i18n.pluralise( i18n._( "Released one message" ), i18n.sprintf( i18n._( "Released %d messages" ), result.safelistCount ), count );

            
            if ( result.safelistCount <= 0 ) safelist = "";
            if ( count <= 0 ) release = "";
            
            if ( safelist == "" && release == "" ) return "";
            if ( safelist == "" ) return release;
            if ( release == "" ) return safelist;
            return safelist + " , " + release;
        };

        var getSafelist = function( result ) { return result.safelist; };

        var getCount = function( result ) { return result.totalRecords };
        var action = quarantine.rpc.safelist;
        action( this.refreshTable.createDelegate( quarantine, [ null, getCount, getMessage, getSafelist ], true ),
                inboxDetails.token, addresses );
    },
    
    refreshTable : function( result, exception, foo, messages, getCount, getMessage, getSafelist )
    {
        if ( exception ) {
            Ext.MessageBox.alert("Failed",exception.message); 
            return;
        }

        try {
            var message = getMessage( result, messages );

            /* Update the new total number of records */
            quarantine.store.proxy.setTotalRecords( getCount( result ));

            /* to refresh the buttons at the bottom */
            quarantine.updateActionItem( null, null,false );
            
            /* Reload the data */
            quarantine.store.load({params:{start:0, limit:quarantine.pageSize}});
            
            /* need some sprintf or something here */
            var title = i18n._( "Quarantined Messages" );
            if ( message != null && message != "" ) title += " (" + message + ")";
            quarantine.grid.setTitle( title );
            
            /* Refresh the table */
            quarantine.grid.setDisabled( false );

            /* Refresh the safelist table */
            if ( getSafelist != null ) {
                var sl = getSafelist( result );
                /* Build a new set of data */
                for ( var c = 0 ; c < sl.length ; c++ ) sl[c] = [sl[c]];
                safelist.store.loadData( sl );
            }

        } catch ( e ) {
            alert( "Unable to refresh table: " + e );
        }
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
            deleteText = i18n._( "Delete Messages" );
            releaseText = i18n._( "Release Messages" );
            quarantine.releaseButton.setDisabled( true );
            quarantine.deleteButton.setDisabled( true );
        } else if ( count == 1 ) {
            deleteText = i18n._( "Delete one Message" );
            releaseText = i18n._( "Release one Message" );
            quarantine.releaseButton.setDisabled( false );
            quarantine.deleteButton.setDisabled( false );
        } else {
            deleteText = i18n.sprintf( i18n._( "Delete %d Messages" ), count );
            releaseText = i18n.sprintf( i18n._( "Release %d Messages" ), count );
            quarantine.releaseButton.setDisabled( false );
            quarantine.deleteButton.setDisabled( false );
        }

        count = this.addresses.size;
        if ( count == 0 ) {
            safelistText = i18n._( "Safelist Addresses" );
            quarantine.safelistButton.setDisabled( true );
        } else if ( count == 1 ) {
            safelistText = i18n._( "Safelist one Address" );
            quarantine.safelistButton.setDisabled( false );
        } else {
            safelistText = i18n.sprintf( i18n._( "Safelist %d Addresses" ), count );
            quarantine.safelistButton.setDisabled( false );
        }        

        quarantine.releaseButton.setText( releaseText );
        quarantine.deleteButton.setText( deleteText );
        quarantine.safelistButton.setText( safelistText );
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
        quarantine.updateActionItem( record.data.mailID, record.data.sender, false );
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
           width: 200
        },{
           header: "&nbsp",
           dataIndex: 'attachmentCount',
           width: 40
        },{
           header: i18n._( "Score" ),
           dataIndex: 'quarantineDetail',
           width: 40,
        },{
           header: i18n._( "Subject" ),
           dataIndex: 'truncatedSubject',
           width: 300
        },{
           header: i18n._( "Date" ),
           dataIndex: 'quarantinedDate',
           width: 100,
           renderer : function( value ) {
               var date = new Date();
               date.setTime( value.time );
               d = Ext.util.Format.date( date, 'm/d/Y' );
               t = Ext.util.Format.date( date, 'g:i a' );
               return d + '<br/>' + t;
           },
        },{
            header: i18n._( "Size (KB)" ),
           dataIndex: 'size',
           renderer : function( value ) {
               return Math.round( (( value + 0.0 ) / 1024) * 10 ) / 10;
           },
           width: 100
        }]);        

        config.cm.defaultSortable = true;

        config.bbar = new Ext.PagingToolbar({
            pageSize: this.quarantine.pageSize,
            store: this.quarantine.store,
            displayInfo: true,
            displayMsg: 'Displaying Messages {0} - {1} of {2}',
            emptyMsg: 'No messges to display',
            items: [ this.quarantine.releaseButton, this.quarantine.deleteButton, 
                     this.quarantine.safelistButton ] });
        
        config.store = this.quarantine.store;
        config.sm = this.quarantine.selectionModel;
        config.title = i18n._( "Quarantined Messages" );
        Ung.QuarantineGrid.superclass.constructor.apply(this, arguments);
    },
    el : "quarantine-inbox-records",
    width: 800,
    height: 500,
    trackMouseOver:false,
    loadMask: true,
    frame : true
});


Ext.onReady(function() {
    // Initialize the I18n
    Ext.Ajax.request({
        url : '/webui/i18n',
            success : function( response, options ) {
            i18n = new Ung.I18N({ map : Ext.decode( response.responseText )});
            completeInit();
        },
        method : "GET",
        failure : function() {
            Ext.MessageBox.alert("Error", "Unable to load the language pack." );
        },
      params : { module : 'quarantine' }
    });

});

function completeInit()
{
    quarantine = new Ung.Quarantine();

    quarantine.init();
    // pluggable renders
    quarantine.grid.render();
    
    // trigger the data store load
    quarantine.store.load({params:{start:0, limit:quarantine.pageSize}});
}

// Grid check column (copied from main.js)
Ext.grid.CheckColumn = function(config) {
    Ext.apply(this, config);
    if (!this.id) {
        this.id = Ext.id();
    }
    if (!this.width) {
        this.width = 40;
    }
    this.renderer = this.renderer.createDelegate(this);
};

Ext.grid.CheckColumn.prototype = {
    init : function(grid) {
        this.grid = grid;
        this.grid.on('render', function() {
            var view = this.grid.getView();
            view.mainBody.on('mousedown', this.onMouseDown, this);
        }, this);
    },
    changeRecord : function(record) {
        record.set(this.dataIndex, !record.data[this.dataIndex]);
    },
    onMouseDown : function(e, t) {
        if (t.className && t.className.indexOf('x-grid3-cc-' + this.id) != -1) {
            e.stopEvent();
            var index = this.grid.getView().findRowIndex(t);
            var record = this.grid.store.getAt(index);
            this.changeRecord(record);
        }
    },

    renderer : function(value, metadata, record) {
        metadata.css += ' x-grid3-check-col-td';
        return '<div class="x-grid3-check-col' + (value ? '-on' : '') + ' x-grid3-cc-' + this.id + '">&#160;</div>';
    }
};

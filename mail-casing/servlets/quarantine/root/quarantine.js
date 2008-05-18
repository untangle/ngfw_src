Ext.namespace('Ung');

//The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/webui/ext/resources/images/default/s.gif';

Ung.SimpleHash = function()
{
}

Ung.SimpleHash.prototype = {
    data : {},
    size : 0,

    put : function( key, value )
    {
        if ( this.data[key] ) {
            if ( value == null ) this.size--;
        } else {
            if ( value != null ) this.size++;
        }

        this.data[key] = value;
    },

    clear : function( key )
    {
        if ( this.data[key] ) this.size--;
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

//RpcProxy
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
            res=this.reader.readRecords(result);
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

Ung.Quarantine = function() {
}

Ung.Quarantine.prototype = {
    disableThreads: true, // in development environment is useful to disable threads.
    rpc : null,
    
    /* This is a hash of message ids that are ready to be deleted or released. */
    actionItems : new Ung.SimpleHash(),

    init: function() {
		//get JSONRpcClient
		this.rpc = new JSONRpcClient("/quarantine/JSON-RPC").Quarantine;
    },
    
    store : null,

    selectionModel : null,

    grid : null,

    pageSize : 25,
    
    executeAction : function( action, messages ) {
        var mids = [];
        for ( var key in quarantine.actionItems.data ) mids.push( key );

        /* Clear the current action items */
        quarantine.actionItems.clearAll();
        quarantine.selectionModel.clearSelections();

        quarantine.grid.setDisabled( true );
        
        var obj = {};
        obj.messages = messages;
        action(this.refreshTable.createDelegate(obj), inboxDetails.token, mids );
    },

    releaseButton : new Ext.Button( {
        handler : function() { quarantine.executeAction( quarantine.rpc.releaseMessages,
                                                         globalStrings.releaseMessages); },
        text : globalStrings.buttonRelease[0],
        disabled : true
    } ),

    deleteButton : new Ext.Button( {
        handler : function() { quarantine.executeAction( quarantine.rpc.purgeMessages, 
                                                         globalStrings.deletedMessages ); },
        text : globalStrings.buttonDelete[0],
        disabled : true
    } ),
    
    refreshTable : function( result, exception ) {
        if ( exception ) {
            Ext.MessageBox.alert("Failed",exception.message); 
            return;
        }

        try {
            /* This is the number of messages released or deleted */
            var count = quarantine.store.proxy.totalRecords - result;

            /* Update the new total number of records */
            quarantine.store.proxy.setTotalRecords( result );

            /* to refresh the buttons at the bottom */
            quarantine.updateActionItem( null, false );
            
            /* Reload the data */
            quarantine.store.load({params:{start:0, limit:quarantine.pageSize}});

            var message = this.messages[1];
            if ( count <= 0 ) message = "";
            else if ( count == 1 ) message = this.messages[0];

            message = sprintf( message, count );
            
            /* need some sprintf or something here */
            quarantine.grid.setTitle( globalStrings.quarantineGridTitle + " (" + message + ")" );
            
            /* Refresh the table */
            quarantine.grid.setDisabled( false );

        } catch ( e ) {
            alert( "Unable to refresh table: " + e );
        }
    },

    updateActionItem : function( mailid, ifAdd )
    {
        var value = true;
        if ( !ifAdd ) value = null;
        if ( mailid != null ) this.actionItems.put( mailid, value );

        var deleteText;
        var releaseText;
        
        var count = this.actionItems.size;
        if ( count == 0 ) {
            deleteText = globalStrings.buttonDelete[0];
            releaseText = globalStrings.buttonRelease[0];
            quarantine.releaseButton.setDisabled( true );
            quarantine.deleteButton.setDisabled( true );
        } else if ( count == 1 ) {
            deleteText = globalStrings.buttonDelete[1];
            releaseText = globalStrings.buttonRelease[1];
            quarantine.releaseButton.setDisabled( false );
            quarantine.deleteButton.setDisabled( false );
        } else {
            deleteText = sprintf( globalStrings.buttonDelete[2], count );
            releaseText = sprintf( globalStrings.buttonRelease[2],count );
            quarantine.releaseButton.setDisabled( false );
            quarantine.deleteButton.setDisabled( false );
        }

        quarantine.releaseButton.setText( releaseText );
        quarantine.deleteButton.setText( deleteText );
    }
}

Ext.onReady(function() {
    quarantine = new Ung.Quarantine();
    quarantine.init();
    
    quarantine.selectionModel = new Ext.grid.CheckboxSelectionModel({
        listeners : {
            rowselect : function( sm, rowIndex, record ) {
                quarantine.updateActionItem( record.data.mailID, true );
            },
            rowdeselect : function( sm, rowIndex, record ) {
                quarantine.updateActionItem( record.data.mailID, false );
            }
        }});
    
    // create the Data Store
    quarantine.store = new Ext.data.Store({
        // load using script tags for cross domain, if the data in on the same domain as
        // this page, an HttpProxy would be better
        proxy: new Ung.QuarantineProxy( quarantine.rpc.getInboxRecords, true, inboxDetails.totalCount ),

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

        listeners : {
            load : function( store, records, options ) {
                /* now update the selection model? */
                var rows = [];

                for ( var c= 0 ; c < records.length ; c++ ) {
                    var record = records[c];
                    
                    if ( quarantine.actionItems.get( record.data.mailID ) == true ) rows.push( c );
                }

                quarantine.selectionModel.selectRows( rows );
            }
        }
                   
    });

    // pluggable renders

    // the column model has information about grid columns
    // dataIndex maps the column to the specific data field in
    // the data store
    var cm = new Ext.grid.ColumnModel([
        quarantine.selectionModel,
        {
           header: globalStrings.headerSender,
           dataIndex: 'sender',
           width: 200
        },{
           header: "&nbsp",
           dataIndex: 'attachmentCount',
           width: 40
        },{
           header: globalStrings.headerQuarantineDetail,
           dataIndex: 'quarantineDetail',
           width: 40,
        },{
           header: globalStrings.headerSubject,
           dataIndex: 'truncatedSubject',
           width: 300
        },{
           header: globalStrings.headerQuarantineDate,
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
           header: globalStrings.headerSize,
           dataIndex: 'size',
           renderer : function( value ) {
               return Math.round( (( value + 0.0 ) / 1024) * 10 ) / 10;
           },
           width: 100
        }]);

    // by default columns are sortable
    cm.defaultSortable = true;    

    quarantine.grid = new Ext.grid.GridPanel({
        el : "quarantine-inbox-records",
        width: 800,
        height: 500,
        title: globalStrings.quarantineGridTitle,
        store: quarantine.store,
        cm: cm,
        sm: quarantine.selectionModel,
        trackMouseOver:false,
        loadMask: true,
        bbar: new Ext.PagingToolbar({
            pageSize: quarantine.pageSize,
            store: quarantine.store,
            displayInfo: true,
            displayMsg: 'Displaying Messages {0} - {1} of {2}',
            emptyMsg: 'No messges to display',
            items: [ quarantine.releaseButton, quarantine.deleteButton ] })
    });

    quarantine.grid.render();

    // trigger the data store load
    quarantine.store.load({params:{start:0, limit:quarantine.pageSize}});
});

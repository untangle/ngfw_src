Ext.namespace('Ung');

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

Ext.grid.CheckColumn = function(config){
    Ext.apply(this, config);
    if(!this.id){
        this.id = Ext.id();
    }
    if(!this.width) {
        this.width = 40;
    }
    this.renderer = this.renderer.createDelegate(this);
};

Ext.grid.CheckColumn.prototype ={
    init: function(grid){
        this.grid = grid;
        this.grid.on('render', function(){
            var view = this.grid.getView();
            view.mainBody.on('mousedown', this.onMouseDown, this);
        }, this);
    },
        changeRecord : function(record){
        record.set(this.dataIndex, !record.data[this.dataIndex]);
        },
    onMouseDown: function(e, t){
        if(t.className && t.className.indexOf('x-grid3-cc-'+this.id) != -1){
            e.stopEvent();
                var index = this.grid.getView().findRowIndex(t);
                var record = this.grid.store.getAt(index);
            this.changeRecord(record);
        }
    },

    renderer: function(value, metadata, record){
        metadata.css += ' x-grid3-check-col-td'; 
        return '<div class="x-grid3-check-col'+(value?'-on':'')+' x-grid3-cc-'+this.id+'">&#160;</div>';
    }
};

//The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/webui/ext/resources/images/default/s.gif';

var quarantine = null;

Ung.Quarantine = function() {
}

Ung.Quarantine.prototype = {
    disableThreads: true, // in development environment is useful to disable threads.
    rpc : null,

    init: function() {
		//get JSONRpcClient
		this.rpc = new JSONRpcClient("/quarantine/JSON-RPC").Quarantine;

        
    }
}

Ext.onReady(function() {
    quarantine = new Ung.Quarantine();
    quarantine.init();
    
    var logColumn = new Ext.grid.CheckColumn({
	       header: "<b>"+"log"+"</b>", dataIndex: 'log', fixed:true
                       });

    // create the Data Store
    var store = new Ext.data.Store({
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
        remoteSort: true
    });

    // pluggable renders

    // the column model has information about grid columns
    // dataIndex maps the column to the specific data field in
    // the data store
    var cm = new Ext.grid.ColumnModel([{
           id: 'topic', // id assigned so we can apply custom css (e.g. .x-grid-col-topic b { color:#333 })
           header: "Topic",
           dataIndex: 'title',
           width: 10,
           hidden: true,
        },{
           header: "From",
           dataIndex: 'sender',
           width: 100
        },{
           header: "&nbsp",
           dataIndex: 'attachmentCount',
           width: 70
        },{
           header: "Score",
           dataIndex: 'quarantineDetail',
           width: 70,
        },{
           header: "Subject",
           dataIndex: 'truncatedSubject',
           width: 200
        }]);

    // by default columns are sortable
    cm.defaultSortable = true;

    var grid = new Ext.grid.GridPanel({
        el : "quarantine-inbox-records",
        width: 700,
        height: 500,
        title:'Quarantined Messages',
        store: store,
        cm: cm,
        trackMouseOver:false,
        sm: new Ext.grid.RowSelectionModel({selectRow:Ext.emptyFn}),
        loadMask: true,
            /*
        viewConfig: {
            forceFit:true,
        },
            */

        bbar: new Ext.PagingToolbar({
            pageSize: 25,
            store: store,
            displayInfo: true,
            displayMsg: 'Displaying Messages {0} - {1} of {2}',
            emptyMsg: 'No messges to display',
            items:[]
            })
    });

    grid.render();

    // trigger the data store load
    store.load({params:{start:0, limit:25}});
});

Ext.define('Ung.view.dashboard.Queue', {
    alternateClassName: 'DashboardQueue',
    singleton: true,
    processing: false,
    paused: false,
    queue: [],
    queueMap: {},
    tout: null,
    add: function (widget) {
        if (!this.queueMap[widget.id]) {
            this.queue.push(widget);
            // console.log('Adding: ' + widget.itemId);
            this.process();
        } /* else { console.log("Prevent Double queuing: " + widget.title); } */
    },
    addFirst: function (widget) {
        if (!this.queueMap[widget.id]) {
            this.queue.unshift(widget);
            //console.log("Adding first: " + widget.title);
            this.process();
        }
    },
    next: function () {
        //console.log("Finish last started widget.");
        this.processing = false;
        this.process();
    },
    remove: function (widget) {
        console.log('removing');
        if (this.processing) {
            this.processing = false;
        }
    },
    process: function () {
        var me = this;
        // ensure processing is false in some cases when it remains true
        clearTimeout(this.tout);
        this.tout = setTimeout(function () {
            me.processing = false;
            me.queue = [];
        }, 3000);

        //console.log(this.queue);
        if (!this.paused && !this.processing && this.queue.length > 0) {
            this.processing = true;
            var widget = this.queue.shift();

            delete this.queueMap[widget.id];

            //if (this.inView(widget)) {
            if (!widget.isHidden()) {
                if (widget.down('#report-widget')) {
                    widget.down('#report-widget').getController().fetchData();
                }
            } else {
                widget.toQueue = true;
                Ung.view.dashboard.Queue.next();
            }
        }
    },
    reset: function () {
        this.queue = [];
        this.queueMap = {};
        this.processing = false;
    },
    pause: function () {
        this.paused = true;
    },
    resume: function () {
        this.paused = false;
        this.process();
    },

    inView: function (widget) {
        // checks if the widget is visible
        if (!widget.getEl()) {
            return false;
        }
        var widgetGeometry = widget.getEl().dom.getBoundingClientRect();
        return (widgetGeometry.top + widgetGeometry.height / 2) > 0 && (widgetGeometry.height / 2 + widgetGeometry.top < window.innerHeight);
    }

});

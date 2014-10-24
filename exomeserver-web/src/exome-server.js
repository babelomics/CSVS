function ExomeServer(args) {
    _.extend(this, Backbone.Events);

    var _this = this;
    this.id = Utils.genId("ExomeServer");

    //set default args
    this.suiteId = -1;
    this.title = 'Exome Server';
    this.title = '<span>Exome Server<img src="http://bioinfo.cipf.es/bierwiki/lib/tpl/arctic/images/logobier.jpg" height="35px"></span>';
    this.description = 'beta';
    this.version = '1.0.5';
    this.border = true;
    this.targetId;
    this.width;
    this.height;


    //set instantiation args, must be last
    _.extend(this, args);

    this.accountData = null;

    this.resizing = false;

    this.rendered = false;
    if (this.autoRender) {
        this.render();
    }
}


ExomeServer.prototype = {
    render: function (targetId) {
        var _this = this;
        this.targetId = (targetId) ? targetId : this.targetId;
        if ($('#' + this.targetId).length < 1) {
            console.log('targetId not found in DOM');
            return;
        }

        console.log("Initializing ExomeServer");
        this.targetDiv = $('#' + this.targetId)[0];
        this.div = $('<div id="ExomeServer" style="height:100%;position:relative;"></div>')[0];
        $(this.targetDiv).append(this.div);

        this.headerWidgetDiv = $('<div id="header-widget" style="padding: 25px 0 20px 25px;"><div class="appName">' + this.title + '</div></div>')[0];
        $(this.div).append(this.headerWidgetDiv);

        this.contentDiv = $('<div id="content" style="height: 100%;"></div>')[0];
        $(this.div).append(this.contentDiv);

        this.width = ($(this.div).width());
        this.height = ($(this.div).height());

        console.log(this.width);
        console.log(this.height);


        if (this.border) {
            var border = (_.isString(this.border)) ? this.border : '1px solid lightgray';
            $(this.div).css({border: border});
        }

        $(window).resize(function (event) {
            if (event.target == window) {
                if (!_this.resizing) {//avoid multiple resize events
                    _this.resizing = true;
                    _this.setSize($(_this.div).width(), $(_this.div).height());
                    setTimeout(function () {
                        _this.resizing = false;
                    }, 400);
                }
            }
        });

        this.rendered = true;
    },
    draw: function () {
        var _this = this;
        if (!this.rendered) {
            console.info('ExomeServer is not rendered yet');
            return;
        }

        /* check height */
        var topOffset = 80;
        $(this.contentDiv).css({height: 'calc(100% - ' + topOffset + 'px)'});

        $.cookie('bioinfo_account', 'example');
        $.cookie('bioinfo_sid', 'eVr5HnCMxP93RMG2lXKw');

        var data = {
            command: {
                data: {
                    'vcf-file': 'bier.vcf'
                }
            },
            id: 'FaQ3FYucIJScliu'
        };

        var variantWidget = new ExomeServerWidget({
            targetId: $('#content').attr('id'),
            job: data,
            autoRender: true
        });
        variantWidget.draw();

    }
};

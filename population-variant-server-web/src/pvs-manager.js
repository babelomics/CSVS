var PVSManager = {
//    host: (typeof OPENCGA_HOST === 'undefined') ? 'http://ws.bioinfo.cipf.es/opencga/rest' : OPENCGA_HOST,
    host: PVS_HOST,
    version: 'v3',

    //studies: {
    //    list: function (args) {
    //        return PVSManager._doRequest(args, 'studies', 'list');
    //    },
    //    read: function (args) {
    //        return PVSManager._doRequest(args, 'studies', 'info');
    //    }
    //},

    diseases: {
        list: function (args) {
            return PVSManager._doRequest(args, 'diseases', 'list');
        },
        //read: function (args) {
        //    return PVSManager._doRequest(args, 'studies', 'info');
        //}
    },

    variants: {
        fetch: function (args) {
            return PVSManager._doRequest(args, 'variants', 'fetch');
        }
    },

    _url: function (args, api, action) {
        var host = PVSManager.host;
        if (typeof args.request.host !== 'undefined' && args.request.host != null) {
            host = args.request.host;
        }
        //var version = PVSManager.version;
        //if (typeof args.request.version !== 'undefined' && args.request.version != null) {
        //    version = args.request.version;
        //}
        var id = '';
        if (typeof args.id !== 'undefined' && args.id != null) {
            id = '/' + args.id;
        }

        var url = host + '/' + api + id + '/' + action;
        url = Utils.addQueryParamtersToUrl(args.query, url);
        return url;
    },

    _doRequest: function (args, api, action) {
        var url = PVSManager._url(args, api, action);
        if (args.request.url === true) {
            return url;
        } else {
            var method = 'GET';
            if (typeof args.request.method !== 'undefined' && args.request.method != null) {
                method = args.request.method;
            }
            var async = true;
            if (typeof args.request.async !== 'undefined' && args.request.async != null) {
                async = args.request.async;
            }

            console.log(url);
            var request = new XMLHttpRequest();
            request.onload = function () {
                var contentType = this.getResponseHeader('Content-Type');
                if (contentType === 'application/json') {
                    args.request.success(JSON.parse(this.response), this);
                } else {
                    args.request.success(this.response, this);
                }
            };
            request.onerror = function () {
                args.request.error(this);
            };
            request.open(method, url, async);
            request.send();
            return url;
        }
    }
};
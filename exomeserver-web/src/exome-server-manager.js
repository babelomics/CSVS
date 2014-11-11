var ExomeServerManager = {
//    host: (typeof OPENCGA_HOST === 'undefined') ? 'http://ws.bioinfo.cipf.es/opencga/rest' : OPENCGA_HOST,
    host: "http://localhost:8080/exomeserver/rest",
    version: 'v3',

    studies: {
        list: function (args) {
            return ExomeServerManager._doRequest(args, 'studies', 'list');
        },
        read: function (args) {
            return ExomeServerManager._doRequest(args, 'studies', 'info');
        },
        update: function (args) {
            return ExomeServerManager._doRequest(args, 'studies', 'modify');
        },
        create: function (args) {
            return ExomeServerManager._doRequest(args, 'studies', 'create');
        },
        delete: function (args) {
            return ExomeServerManager._doRequest(args, 'studies', 'delete');
        }
    },

    files: {
        list: function (args) {
            return ExomeServerManager._doRequest(args, 'files', 'list');
        },
        fetch: function (args) {
            return ExomeServerManager._doRequest(args, 'files', 'fetch');
        },
        read: function (args) {
            return ExomeServerManager._doRequest(args, 'files', 'info');
        },
        delete: function (args) {
            return ExomeServerManager._doRequest(args, 'files', 'delete');
        },
        index: function (args) {
            return ExomeServerManager._doRequest(args, 'files', 'index');
        }
    },

    _url: function (args, api, action) {
        var host = ExomeServerManager.host;
        if (typeof args.request.host !== 'undefined' && args.request.host != null) {
            host = args.request.host;
        }
        //var version = ExomeServerManager.version;
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
        var url = ExomeServerManager._url(args, api, action);
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
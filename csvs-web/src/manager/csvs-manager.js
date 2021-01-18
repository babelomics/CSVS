var CSVSManager = {
    host: (typeof CSVS_HOST === 'undefined') ? 'http://csvs-dev.clinbioinfosspa.es:8080/csvs/rest' : CSVS_HOST,
    version: 'v3',

    diseases: {
        list: function (args) {
            return CSVSManager._doRequest(args, 'diseases', 'list');
        }
    },
    technologies: {
        list: function (args) {
            return CSVSManager._doRequest(args, 'technologies', 'list');
        }
    },
    variants: {
        fetch: function (args) {
            return CSVSManager._doRequest(args, 'variants', 'fetch');
        },
        metadata: function (args) {
            return CSVSManager._doRequest(args, 'variants', 'metadata');
        }
    },
    regions: {
        saturation: function (args) {
            return CSVSManager._doRequest(args, 'regions', 'saturation');
        }
    },
    files:{
        samples: function (args) {
            return CSVSManager._doRequest(args, 'files', 'samples');
        }
    },
    pathologies:{
        fetch: function (args) {
            return CSVSManager._doRequest(args, 'pathologies', 'fetch');
        }
    },

    _url: function (args, api, action) {
        var host = CSVSManager.host;
        if (typeof args.request.host !== 'undefined' && args.request.host != null) {
            host = args.request.host;
        }
        var id = '';
        if (typeof args.id !== 'undefined' && args.id != null) {
            id = '/' + args.id;
        }

        var url = host + '/' + api + id + '/' + action;
        url = Utils.addQueryParamtersToUrl(args.query, url);

        if (Cookies("bioinfo_user") && Cookies("bioinfo_sid"))
            url = Utils.addQueryParamtersToUrl({"user":Cookies("bioinfo_user"), "sid":Cookies("bioinfo_sid")}, url);

        return url;
    },

    _doRequest: function (args, api, action) {
        var url = CSVSManager._url(args, api, action);
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

            //console.log(url);
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
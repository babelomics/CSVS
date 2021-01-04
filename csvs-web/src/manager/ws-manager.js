/*
 * 2020 GRG
 */

var WSManager = {
    host: (typeof window.WS_HOST === 'undefined') ? '' : window.WS_HOST,
    version: (typeof window.WS_VERSION === 'undefined') ? 'v1' : window.WS_VERSION,
    ensembl: (typeof window.WS_ENSEMBL === 'undefined') ? '82' : window.WS_ENSEMBL,
    assembly: (typeof window.WS_ASSEMBLY === 'undefined') ? '37' : window.WS_ASSEMBLY,

    get: function (args) {
        var success = args.success;
        var error = args.error;
        var async = (args.async == false) ? false: true;
        var method  = !args.method? "GET": args.method;


        // remove XMLHttpRequest keys
        var ignoreKeys = ['success', 'error', 'async'];
        var urlConfig = {};
        for (var prop in args) {
            if (hasOwnProperty.call(args, prop) && args[prop] != null && ignoreKeys.indexOf(prop) == -1) {
                urlConfig[prop] = args[prop];
            }
        }

        var url = WSManager.url(urlConfig);
        if (typeof url === 'undefined') {
            return;
        }

        if (window.WS_LOG != null && WS_LOG === true) {
            console.log(url);
        }

        var d;
        var request = new XMLHttpRequest();
        request.onload = function () {
            var contentType = this.getResponseHeader('Content-Type');
            if (contentType.indexOf('application/json')!= -1) {
                var parsedResponse = JSON.parse(this.response);
                if (typeof success === "function") success(parsedResponse);
                d = parsedResponse;
            } else {
                console.log('WS returned a non json object or list, please check the url.');
                console.log(url);
                console.log(this.response);
            }
        };

        request.onerror = function () {
            console.log("WSManager: Ajax call returned " + this.statusText);
            if (typeof error === "function") error(this);
        };



        request.open(method, url, async);
        if (args.headers != null) {
            for (var header in args.headers) {
                request.setRequestHeader(header, args.headers[header]);
            }
        }

        var body = null;
        if (args.body != null) {
            body = args.body;
        }
        try {
            request.send(body);
        } catch (e) {
            console.log("WSManager: Ajax call returned " + this.statusText);
            if (typeof error === "function") error(this);
        }

        return d;

    },
    url: function (args) {
        if (args == null) {
            args = {};
        }
        if (args.params == null) {
            args.params = {};
        }

        var version = this.version;
        if (args.version != null) {
            version = args.version
        }

        var host = this.host;
        if (args.host != null) {
            host = args.host;
        }

        var ensembl = this.ensembl;
        if (args.ensembl != null) {
            ensembl = args.ensembl
        }
        var assembly = this.assembly;
        if (args.assembly != null) {
            assembly = args.assembly
        }
        delete args.host;
        delete args.version;
        delete args.ensembl;
        delete args.assembly;

        var config = {
            host: host,
            version: version,
            ensembl: ensembl,
            assembly: assembly
        };

        for (var prop in args) {
            if (hasOwnProperty.call(args, prop) && args[prop] != null) {
                config[prop] = args[prop];
            }
        }


        var url = config.host + config.version + '/ensembl/' + config.ensembl + '/homo_sapiens/' + config.assembly + '/' +  config.category + "/" + config.subCategory;

        return url;
    }
};

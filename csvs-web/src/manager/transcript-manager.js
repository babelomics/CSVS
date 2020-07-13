/*
 * Copyright (c) 2020 GRG
 *
 * This file is part of JS Common Libs.
 *
 * JS Common Libs is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * JS Common Libs is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JS Common Libs. If not, see <http://www.gnu.org/licenses/>.
 */

var TranscriptManager = {
    host: (typeof window.TRANSCRIPT_HOST === 'undefined') ? '' : window.TRANSCRIPT_HOST,
    version: (typeof window.TRANSCRIPT_VERSION === 'undefined') ? 'v1' : window.TRANSCRIPT_VERSION,
    ensembl: (typeof window.TRANSCRIPT_ENSEMBL === 'undefined') ? '82' : window.TRANSCRIPT_ENSEMBL,
    assembly: (typeof window.TRANSCRIPT_ASSEMBLY === 'undefined') ? '37' : window.TRANSCRIPT_ASSEMBLY,

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

        var url = TranscriptManager.url(urlConfig);
        if (typeof url === 'undefined') {
            return;
        }

        if (window.TRANSCRIPT_LOG != null && TRANSCRIPT_LOG === true) {
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
                console.log('Transcript returned a non json object or list, please check the url.');
                console.log(url);
                console.log(this.response)
            }
        };

        request.onerror = function () {
            console.log("TranscriptManager: Ajax call returned " + this.statusText);
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
            console.log("TranscriptManager: Ajax call returned " + this.statusText);
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
/*
        var query = '';
        if (config.query != null) {
            query = '/' + config.query.toString();
        }*/

        /*
        //species can be the species code(String) or an object with text attribute
        if (config.species && config.species.id != null) {
            if (config.species.assembly != null) {
                config.params["assembly"] = config.species.assembly.name;
            }
            // TODO Remove temporary fix
            if (config.subCategory === 'chromosome') {
                delete config.params["assembly"]
            }
            config.species = stv.utils.getSpeciesCode(config.species.scientificName);
        }
        */
        // COMMENT var url = config.host + config.version + '/ensembl/' + config.ensembl + '/homo_sapiens/' + config.assembly + '/genes' + query + '?ids='+ config.params.ids;

        var url = config.host + config.version + '/ensembl/' + config.ensembl + '/homo_sapiens/' + config.assembly + '/' +  config.category + "/" + config.subCategory;




        //if (config.category === 'meta') {
//            url = config.host + '/webservices/rest/' + config.version + '/' + config.category + '/' + config.subCategory;
  //      } else {
            //url = config.host + '/webservices/rest/' + config.version + '/' + config.species + '/' + config.category + '/' + config.subCategory + query + '/' + config.resource;
       // }

        //url = stv.utils.addQueryParamtersToUrl(config.params, url);




        return url;
    }
};

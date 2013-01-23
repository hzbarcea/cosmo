/* * Copyright 2006-2007 Open Source Applications Foundation *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 /**
 * summary:
 */

dojo.provide("cosmo.service.transport.Rest");

dojo.require("cosmo.env");
dojo.require("cosmo.util.auth");

dojo.declare("cosmo.service.transport.Rest", null,
    {
        translator: null,

        constructor: function (translator){

        },

        methodIsSupported: {
            'get': true,
            'post': true,
            'head': true
        },

        METHOD_GET: "GET",
        METHOD_PUT: "PUT",
        METHOD_POST: "POST",
        METHOD_DELETE: "DELETE",
        METHOD_HEAD: "HEAD",

        /**
         * summary: Return request populated with attributes common to all calls.
         */
        getDefaultRequest: function (url, kwArgs){
            kwArgs = kwArgs || {};
            if (url){
                if (!!url.match(/.*ticket=.*/)){
                    kwArgs.noAuth = true;
                }
            }
            // Add error fo transport layer problems

            var request = cosmo.util.auth.getAuthorizedRequest({}, kwArgs);

            //FIXME
            if (false){
                if (!kwArgs.noErr){
                    deferred.addErrback(function(e) { console.log("Transport Error: ");
                                                      console.log(e);
                                                      return e;});
                }
                request.load = request.load || this.resultCallback(deferred);
                request.error = request.error || this.errorCallback(deferred);
            }
            request.transport = request.transport || "XMLHTTPTransport";
            request.contentType = request.contentType || 'text/xml';
            request.sync = kwArgs.sync || false;
            request.headers = request.headers || {};
            request.url = url;
            request.handleAs = "xml";
            return request;
        },

        errorCallback: function(/* dojo.Deferred */ deferredRequestHandler){
            // summary
            // create callback that calls the Deferreds errback method
            return function(){
                // Workaround to not choke on 204s
                if (dojo.isIE &&
                    xhr.status == 1223){
                    xhr = {};
                    xhr.status = 204;
                    xhr.statusText = "No Content";
                    xhr.responseText = "";

                    deferredRequestHandler.callback("", xhr);

                } else {
                    // Turns out that when we get a 204 in IE it raises an error
                    // that causes Dojo to call this function with a fake
                    // 404 response (they return {status: 404}. Unfortunately,
                    // the xhr still does return with a 1223, and the
                    // Deferred's load methods get called twice, raising a fatal error.
                    // This works around this, but is very tightly coupled to the Dojo
                    // implementation.
                    // TODO: find a better way to do this
                    if (deferredRequestHandler.fired == -1){
                        var err = new Error(e.message);
                        err.xhr = xhr;
                        deferredRequestHandler.errback(err);
                    }
                }
            }
        },

        resultCallback: function(/* dojo.Deferred */ deferredRequestHandler){
            // summary
            // create callback that calls the Deferred's callback method
            var tf = dojo.hitch(this,
                function(type, obj, xhr){
                    if (obj && obj["error"] != null) {
                        var err = new Error(obj.error);
                        err.id = obj.id;
                        err.xhr = xhr;
                        deferredRequestHandler.errback(err);
                    } else {
                        obj = xhr.responseXML || obj;
                        deferredRequestHandler.callback(obj, xhr);
                    }
                }
            );
            return tf;
        },

        queryHashToString: function(/*Object*/ queryHash){
            var queryList = [];
            for (var key in queryHash){
                queryList.push(key + "=" + encodeURIComponent(queryHash[key]));
            }
            if (queryList.length > 0){
                return "?" + queryList.join("&");
            }
            else return "";
        },

        addErrorCodeToExceptionErrback: function(deferred, responseCode, exception){
            deferred.addErrback(function (err){
                if (err.statusCode == responseCode){
                    err = new exception(err);
                }
                return err;
            });
        },

        addStandardErrorHandling: function (deferred, url, postContent, method){
            deferred.addErrback(function (err) {
                if (err.xhr.status == 404){
                    return new cosmo.service.exception.ResourceNotFoundException(url);
                }

                if (err.xhr.status >= 400 &&  err.xhr.status <= 599){
                    return new cosmo.service.exception.ServerSideError({
                        url: url,
                        statusCode: err.xhr.status,
                        responseContent: err.xhr.responseText,
                        postContent: postContent,
                        method: method
                    });
                }
                return err;
            });
        }

    }
);

/**
 ** Copyright 2016 General Electric Company
 **
 ** Authors:  Paul Cuddihy, Justin McHugh
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

/*
 * 
 * This is just for lumping a sparqlConnection with a SemanticNodeGroup so they can be stored and retrieved together
 * 
 */
define([	// properly require.config'ed
        		
			// shimmed
        	'sparqlgraph/js/belmont'
		],

	function() {
		var SparqlGraphJson = function(conn, nodegroup, mappingTab, optDeflateFlag) {
			var deflateFlag = (typeof optDeflateFlag == "undefined") ? false : optDeflateFlag;
			
			// intended that either all or none of the parameters are given
			
			this.jObj = {
					sparqlConn: null,
					sNodeGroup: null,
					importSpec: null,
			};
			
			if (typeof conn      != "undefined") { 
				this.setSparqlConn(conn); 
			}
			
			if (typeof nodegroup != "undefined") { 
				if (deflateFlag) {
					var mappedPropItems = (typeof mappingTab != "undefined" && mappingTab != null) ? mappingTab.getMappedPropItems() : [];
					this.jObj.sNodeGroup = nodegroup.toJson(true, mappedPropItems); 
				} else {
					this.jObj.sNodeGroup = nodegroup.toJson(false, []); 
				}
			}
			
			if (typeof mappingTab != "undefined") { 
				this.jObj.importSpec = mappingTab.toJson();
			}

		};
	
		SparqlGraphJson.prototype = {
			
			getSparqlConn : function() {
				var ret = new SparqlConnection();
				ret.fromJson(this.jObj.sparqlConn);
				return ret;
			},
			
			getNodeGroup : function(ng, optInflateOInfo) {
				// different from Java due to canvas stuff: takes ng param.
				
				ng.clear();
				var json = this.getSNodeGroupJson();
				if (json == null) {
					return null;
				} else {
					ng.addJson(json, optInflateOInfo);
					ng.setSparqlConnection(this.getSparqlConn());
				}
			},
			
			getSNodeGroupJson : function() {
				return this.jObj.sNodeGroup;
			},
			
			getMappingTabJson : function() {
				if (this.jObj.hasOwnProperty("importSpec")) {
					return this.jObj.importSpec;
				} else {
					return null;
				}
			},
			setSparqlConn : function(conn) {
				this.jObj.sparqlConn = conn.toJson();
			},
			
			stringify : function () {
				return JSON.stringify(this.jObj, null, '\t');
			},
			
			parse : function (jsonString) {
				this.jObj = JSON.parse(jsonString);
			},
			
			toJson : function () {
				return this.jObj;
			},
			
			fromJson : function (json) {
				this.jObj = json;
			}
			
		};
		return SparqlGraphJson;            
	}
);
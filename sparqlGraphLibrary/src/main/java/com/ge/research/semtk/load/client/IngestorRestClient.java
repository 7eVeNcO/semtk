/**
 ** Copyright 2016 General Electric Company
 **
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


package com.ge.research.semtk.load.client;

import java.net.ConnectException;

import org.json.simple.JSONObject;

import com.ge.research.semtk.edc.client.EndpointNotFoundException;
import com.ge.research.semtk.resultSet.RecordProcessResults;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClient;


public class IngestorRestClient extends RestClient{

	RecordProcessResults lastResult = null;
	
	public IngestorRestClient (IngestorClientConfig config){
		this.conf = config;
	}
	
	@Override
	public void buildParametersJSON() throws Exception {
		// TODO: what do you think of this
		((IngestorClientConfig) this.conf).addParameters(this.parametersJSON);
	}

	@Override
	public void handleEmptyResponse() throws Exception {
		throw new Exception("Received empty response");		
	}

	/**
	 * Not meant to be used.
	 * @return
	 * @throws Exception
	 */
	public RecordProcessResults execute() throws ConnectException, EndpointNotFoundException, Exception {
		
		if (conf.getServiceEndpoint().isEmpty()) {
			throw new Exception("Attempting to execute IngestionClient with no enpoint specified.");
		}
		JSONObject resultJSON = (JSONObject)super.execute();	
		
		RecordProcessResults ret = new RecordProcessResults(resultJSON); 

		// the ingestor responds with a bit of extra information that should be added. 
		// the unprocessed result is available in case one wants to see everything not included in the strict table version.
		
		return ret;
	}
	
	/**
	 * Ingest from CSV, with the option to override the SPARQL connection
	 * @param template the template (as a String)
	 * @param data the data (as a String)
	 * @param sparqlConnectionOverride the SPARQL connection as a String, or null to use the connection from the template.
	 */
	public void execIngestionFromCsv(String template, String data, String sparqlConnectionOverride) throws ConnectException, EndpointNotFoundException, Exception{
		conf.setServiceEndpoint("ingestion/fromCsvWithNewConnectionPrecheck");
		this.parametersJSON.put("template", template);
		this.parametersJSON.put("data", data);
		this.parametersJSON.put("connectionOverride", sparqlConnectionOverride);
		
		try{
			this.lastResult = this.execute();
			
			this.lastResult.throwExceptionIfUnsuccessful();
	
			return;
		} 
		finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("template");
			this.parametersJSON.remove("data");
			this.parametersJSON.remove("connectionOverride");
		}
	}
	
	/**
	 * Ingest from CSV.
	 * @param template the template (as a String)
	 * @param data the data (as a String)
	 */
	public void execIngestionFromCsv(String template, String data) throws ConnectException, EndpointNotFoundException, Exception{
		conf.setServiceEndpoint("ingestion/fromCsvPrecheck");
		this.parametersJSON.put("template", template);
		this.parametersJSON.put("data", data);
		
		try{
			this.lastResult = this.execute();
			this.lastResult.throwExceptionIfUnsuccessful();
	
			return;
		} 
		finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("template");
			this.parametersJSON.remove("data");
		}
	}
	public RecordProcessResults getLastResult(){
		return this.lastResult;
	}
	
	public Boolean getLastResultSuccess(){
		if(this.lastResult != null){
			return this.lastResult.getSuccess();
		}
		else{
			return null; //valid return for a class
		}
	}
}

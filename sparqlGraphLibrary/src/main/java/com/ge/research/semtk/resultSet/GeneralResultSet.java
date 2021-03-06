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


package com.ge.research.semtk.resultSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import com.ge.research.semtk.edc.client.EndpointNotFoundException;

public abstract class GeneralResultSet {

	public final static String SUCCESS = "success";
	public final static String FAILURE = "failure";
	
	private final static String SuccessMessage = "operations succeeded.";
	private final static String FailureMessage = "operations failed.";
	private final static String AmbiguousMessage = "operations state unknown.";
	
	protected JSONObject resultsContents; // this is what will, ultimately be returned
	protected Boolean success;			// this indicates whether the operation was successful or not
	protected ArrayList<String> rationale;// this holds information related to runtime exceptions, error info, etc.
	
	public GeneralResultSet(){
		this.rationale = new ArrayList<String>();
	}
	
	public GeneralResultSet(Boolean succeeded){
		if(succeeded != null){
			this.success = succeeded;
		}
		this.rationale = new ArrayList<String>();
	}
		
	// get the JSON key used to store the results block.
	public abstract String getResultsBlockName();
	
	public boolean getSuccess(){
		return success;		
	}
	
	public void addRationaleMessage(String message){
		this.rationale.add(message);
	}
	
	public String getRationaleAsString(String delimiter){
		String retval = "";
		
		// spin through the array list and return the elements. delimit them by something.
		for(String excuse : this.rationale){
			retval += excuse + delimiter;
		}
		if(retval.endsWith("||")){
			retval = retval.substring(0, retval.length() - 2);
		}
		return retval;
	}
	
	public void setSuccess(Boolean successful){
		this.success = successful;
	}
	
	public String getResultCodeString(){
		// do we have results?
		if(this.success == null){ return GeneralResultSet.AmbiguousMessage; }
		if(this.success){ return GeneralResultSet.SuccessMessage; }
		else{ return GeneralResultSet.FailureMessage; }
	}
	
	public void throwExceptionIfUnsuccessful () throws Exception {
		if (success != true) {
			throw new Exception(this.getRationaleAsString("\n"));
		}
	}
	/**
	 * Add result content as JSON
	 */
	public void addResultsJSON(JSONObject results) {
		this.resultsContents = results;
	}	
	
	/**
	 * Get result contents as JSON
	 */
	public JSONObject getResultsJSON(){
		return resultsContents;
	}
	
	/**
	 * Get result content as an Object (e.g. for NodeGroupResultSet, override to return a NodeGroup)
	 * @throws Exception 
	 */
	public abstract Object getResults() throws Exception;
	
	
	/**
	 * Create a GeneralResultSet from JSON
	 */
	public void readJson(JSONObject jsonObj) throws EndpointNotFoundException {
		
		// check the json to see if it looks like "we" generated it or not
		if (jsonObj.containsKey("error") || !jsonObj.containsKey("message")) {
			String message = "Couldn't reach service endpoint:\n";
			// Json was not generated by our microservice.   Presumably swagger or something else running on this port.
			
			for (Object k : jsonObj.keySet()) {
				message += (String.format("\t%s: %s\n", k.toString(), jsonObj.get(k).toString()));
			}
			
			throw new EndpointNotFoundException(message);
			
		} else {
			String s = jsonObj.get("status").toString();
			if (s.equals(GeneralResultSet.SUCCESS)) {
				success = true;
				// subclass has set resultsBlockName
				resultsContents = (JSONObject) jsonObj.get(getResultsBlockName());
				
			} else if (s.equals(GeneralResultSet.FAILURE)) {
				success = false;
				resultsContents = null;
				
			} else {
				success = null;
				resultsContents = null;
			}
			
			if (jsonObj.containsKey("rationale")) {
				String fullRationale = (String)jsonObj.get("rationale");
				rationale = new ArrayList<String>( Arrays.asList(fullRationale.split(Pattern.quote("||"))) );
				
			} else {
				rationale = new ArrayList<String>();
			}
		}
	}		
	
	/*
	abstract public static GeneralResultSet fromJson(JSONObject jsonObj);
	*/
	
	/**
	 * Convert the GeneralResultSet to JSON
	 */
	@SuppressWarnings("unchecked")
	public JSONObject toJson(){
		JSONObject retval = new JSONObject();
		
		if(this.success == null){  // the status is unknown. return of results called before values set.
			retval.put("status", GeneralResultSet.FAILURE);
			retval.put("message", GeneralResultSet.AmbiguousMessage);
			
			this.addRationaleMessage(AmbiguousMessage);
		}
		else if(success){
			retval.put("status", GeneralResultSet.SUCCESS);
			retval.put("message", GeneralResultSet.SuccessMessage);
		}
		else{
			retval.put("status", GeneralResultSet.FAILURE);
			retval.put("message", GeneralResultSet.FailureMessage);
		}
		
		String rationaleString = this.getRationaleAsString("||");
		if(rationaleString.length() > 0){
			retval.put("rationale", rationaleString);
		}
		
		// in any case, if the results are not null, let's include them. 
		// partial results may be meaningful to some entities.
		if(this.resultsContents != null && getResultsBlockName() != null && getResultsBlockName().length() > 0){
			retval.put(getResultsBlockName(), this.resultsContents);
		}
		
		return retval;	// return the JSONObject
	}	
	
}

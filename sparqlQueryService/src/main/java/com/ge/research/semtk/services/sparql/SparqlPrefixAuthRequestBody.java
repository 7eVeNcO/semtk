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


package com.ge.research.semtk.services.sparql;

/**
 * For service calls needing SPARQL connection information, a query, and authentication.
 */
public class SparqlPrefixAuthRequestBody extends SparqlAuthRequestBody {

    public String prefix;
    
    /**
     * Validate request contents.  Throws an exception if validation fails.
     */
    public void validate() throws Exception{
    	
    	super.validate();  // validate items from non-auth request 
    	
		if(prefix == null || prefix.trim().isEmpty()){
			throw new Exception("No prefix specified");
		}
    }
	
    /**
     * Print request info to console
     */
    public void printInfo(){
    	super.printInfo();
		System.out.println("prefix: " + prefix);
    }
}

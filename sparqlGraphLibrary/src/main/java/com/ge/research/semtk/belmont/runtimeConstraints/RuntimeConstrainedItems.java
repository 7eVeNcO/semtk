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

package com.ge.research.semtk.belmont.runtimeConstraints;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.Returnable;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.belmont.XSDSupportUtil;
import com.ge.research.semtk.belmont.XSDSupportedTypes;
import com.ge.research.semtk.load.utility.ImportSpecHandler;
import com.ge.research.semtk.resultSet.Table;

public class RuntimeConstrainedItems {

	// date formats are supposed to look like this: 2014-05-23T10:20:13
	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
	
	public static enum SupportedTypes { NODE , PROPERTYITEM };
	
	private NodeGroup parentNodeGroup;
	private HashMap<String, RuntimeConstrainedObject> members;
	
	public RuntimeConstrainedItems(NodeGroup parent){
		this.parentNodeGroup = parent;
		
		// set up the constraint items
		this.members = parent.getConstrainedItems();
	}
	
	public void addConstrainedItem(Returnable rt, SupportedTypes type){
		
		RuntimeConstrainedObject curr = new RuntimeConstrainedObject(rt, type);
		this.members.put(rt.getSparqlID(), curr);
	}
	
	
	public ArrayList<String> getConstrainedItemIds(){
		ArrayList<String> retval = new ArrayList<String>();
		
		for(String s : this.members.keySet() ){
			retval.add(s);
		}
		return retval;
	}
	
	// acccept a json describing the runtime constraints and apply them.
	
	public void applyConstraintJson(JSONArray runtimeConstraints) throws Exception {
		// read out and apply the various RT constraints from the JSON. 
		// Throw an exception if anything seems amiss.
		
		// NOTE: we will be inferring the type of the operands by type of the constrained item.
		//       this is why we do not require the type in the incoming JSON. this might break one
		//       day if multiple primitive types become supported in ranges for properties.
		
		if(this.parentNodeGroup == null){
			throw new Exception("RuntimConstrainedItems.applyConstraintJson :: the parent node group was null. unable to apply contraints to a null node group.");
		}
		if(this.parentNodeGroup.getNodeCount() == 0){
			throw new Exception("RuntimConstrainedItems.applyConstraintJson :: the parent node group was empty. unable to apply contraints to an empty node group.");
		}
		if(this.members.isEmpty() ){
			return;
		}
		if(runtimeConstraints == null || runtimeConstraints.isEmpty()){
			return; // nothing to do
		}
		
		for(Object curr : runtimeConstraints){
			JSONObject c1 = (JSONObject) curr; 	// just a shortcut to avoid a multitude of casts.
			
			// check that the sparqlID exists skipped because it will be checked when a direct assignment is made.
			String sparqlId = c1.get("SparqlID").toString();
			String operator = c1.get("Operator").toString();
			String operandType = "";
			ArrayList<String> operands = new ArrayList<String>();   // however obvious, the operands will go here. 
			
			// get the object referenced by this sparql ID.
			if( this.members.get(sparqlId).getObjectType().equals(SupportedTypes.NODE) ){
				// this was a node and the type should be URI.
				operandType = XSDSupportedTypes.NODE_URI.name();
			}
			
			else if ( this.members.get(sparqlId).getObjectType().equals(SupportedTypes.PROPERTYITEM) ){
				// check the property item itself to get the expected type.
				operandType = this.members.get(sparqlId).getValueType();  // this should return the expected XSD type with no prefix. 
			}
			else{
				throw new Exception("RuntimeConstrainedItems.applyConstraintJson :: " + this.members.get(sparqlId).getObjectType() + " was not understood for " + sparqlId);
			}
			
			JSONArray opers = (JSONArray) c1.get("Operands");
			
			// step through the array and get the operands.
			try{
				for( Object currOpperand : opers){
					// add the next operand to the list if it passes the tests.
					// check the type are at least convertible. for this, we should reuse the code from the import spec handler.				
					ImportSpecHandler.validateDataType(currOpperand.toString(), operandType);
					operands.add( currOpperand.toString() );  // here we go. 
				}
			} catch(Exception eee){
				// we were passed a bad value that could not be cast to the suggested type.
				throw new Exception("RuntimConstrainedItems.applyConstraintJson :: for " + sparqlId + " one of the input values could not be cast to " + operandType);
			}
			
			// create the appropriate constraint and apply it. 
			this.selectAndSetConstraint(sparqlId, operator, this.members.get(sparqlId).getValueType(), operands);
			
		}
		
	}
	// find and create the appropriate constraint
	public void selectAndSetConstraint(String sparqlId, String operationID, String xsdTypeName, ArrayList<String> operands) throws Exception{
		// a big switch statement to figure out which operation, if any we want. 
		
		// check that operator is sane.
		try{
			SupportedOperations.valueOf(operationID.toUpperCase());
		}
		catch(Exception e){
			String supported = "";
			int supportCount = 0;
			for (SupportedOperations c :  SupportedOperations.values()){
				if(supportCount != 0){ supported += " or "; }
				supported += c.name();
				supportCount++;
			}	
			throw new Exception("RuntimConstrainedItems.selectAndSetConstraint :: Operation " + operationID + " not recognized. Recognized operations are: " + supported);
		}
		
		// inputs are sane (sparqlId and operands checked earlier)
		// a large switch statement of our options with a catch all for misunderstood options at the bottom. 
		
		if(operationID.toUpperCase().equals(SupportedOperations.GREATERTHAN.name()) && XSDSupportUtil.numericOperationAvailable(xsdTypeName)){
			// this only handles numeric types right now. dates will likely break.
			Double val = Double.parseDouble(operands.get(0));
			
			this.setNumberGreaterThan(sparqlId, val, false);
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.GREATERTHAN.name()) && XSDSupportUtil.dateOperationAvailable(xsdTypeName)){
			// this only handles numeric types right now. dates will likely break.
			String val = operands.get(0);
			
			this.setDateAfter(sparqlId, val, false);
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.GREATERTHANOREQUALS.name()) && XSDSupportUtil.numericOperationAvailable(xsdTypeName)){
			// this only handles numeric types right now. dates will likely break.
			Double val = Double.parseDouble(operands.get(0));
						
			this.setNumberGreaterThan(sparqlId, val, true);
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.GREATERTHANOREQUALS.name()) && XSDSupportUtil.dateOperationAvailable(xsdTypeName)){
			// this only handles numeric types right now. dates will likely break.
			String val = operands.get(0);
						
			this.setDateAfter(sparqlId, val, true);
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.LESSTHAN.name()) && XSDSupportUtil.numericOperationAvailable(xsdTypeName)){
			// this only handles numeric types right now. dates will likely break.
			Double val = Double.parseDouble(operands.get(0));
						
			this.setNumberLessThan(sparqlId, val, false);
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.LESSTHAN.name()) && XSDSupportUtil.dateOperationAvailable(xsdTypeName)){
			// this only handles numeric types right now. dates will likely break.
			String val = operands.get(0);
						
			this.setDateBefore(sparqlId, val, false);
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.LESSTHANOREQUALS.name()) && XSDSupportUtil.numericOperationAvailable(xsdTypeName)){
			// this only handles numeric types right now. dates will likely break.
			Double val = Double.parseDouble(operands.get(0));
						
			this.setNumberLessThan(sparqlId, val, true);
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.LESSTHANOREQUALS.name()) && XSDSupportUtil.dateOperationAvailable(xsdTypeName)){
			// this only handles numeric types right now. dates will likely break.
			String val = operands.get(0);
						
			this.setDateBefore(sparqlId, val, true);
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.MATCHES.name()) && XSDSupportUtil.dateOperationAvailable(xsdTypeName)){
			// create a constraint to match the provided
			this.setDateMatchesConstraint(sparqlId, operands);
		
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.MATCHES.name())){
			// create a constraint to match the provided
			this.setMatchesConstraint(sparqlId, operands);
		
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.REGEX.name())){
			// create a regex entry
			this.setRegexConstraint(sparqlId, operands.get(0));
		
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.VALUEBETWEEN.name()) && XSDSupportUtil.numericOperationAvailable(xsdTypeName)){
			// this only handles numeric types right now. dates will likely break.
			Double valLow  = Double.parseDouble(operands.get(0));
			Double valHigh = Double.parseDouble(operands.get(1));
			
			this.setNumberInInterval(sparqlId, valLow, valHigh, true, true);
		
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.VALUEBETWEEN.name()) && XSDSupportUtil.dateOperationAvailable(xsdTypeName)){
			// this only handles numeric types right now. dates will likely break.
			String valLow  = operands.get(0);
			String valHigh = operands.get(1);
			
			this.setDateInInterval(sparqlId, valLow, valHigh, true, true);
		
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.VALUEBETWEENUNINCLUSIVE.name()) && XSDSupportUtil.numericOperationAvailable(xsdTypeName)){
			// this only handles numeric types right now. dates will likely break.
			Double valLow  = Double.parseDouble(operands.get(0));
			Double valHigh = Double.parseDouble(operands.get(1));
						
			this.setNumberInInterval(sparqlId, valLow, valHigh, false, false);			
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.VALUEBETWEENUNINCLUSIVE.name()) && XSDSupportUtil.dateOperationAvailable(xsdTypeName)){
			// this only handles numeric types right now. dates will likely break.
			String valLow  = operands.get(0);
			String valHigh = operands.get(1);
						
			this.setDateInInterval(sparqlId, valLow, valHigh, false, false);			
		}
		else{
			throw new Exception("RuntimConstrainedItems.selectAndSetConstraint :: Operation " + operationID + " has no mapped operations. this is likely an oversite.");
		}
		
	
	}
	//_____________________end runtime constraint applications.
	
	// methods that are most likely in the first rev to be used to fill in constraints. 
	// matching constraints.
	public void setMatchesConstraint(String sparqlId, ArrayList<String> inputs) throws Exception{
		// create the constraint string. 
		String constraintStr = ConstraintUtil.getMatchesOneOfConstraint(sparqlId, inputs, getTypeName(sparqlId));
		System.out.println("======= setMatchesConstraint for " + sparqlId + " to " + constraintStr);
		this.setValueContraint(sparqlId, constraintStr);
	}

	public void setDateMatchesConstraint(String sparqlId, ArrayList<String> inputs) throws Exception{
		// create the constraint string. 
		String constraintStr = ConstraintUtil.getDateMatchesOneOfConstraint(sparqlId, inputs, getTypeName(sparqlId));
		this.setValueContraint(sparqlId, constraintStr);
	}
	
	// regex
	public void setRegexConstraint(String sparqlId, String regexFragment) throws Exception{
		String constraintStr = ConstraintUtil.getRegexConstraint(sparqlId, regexFragment, getTypeName(sparqlId));
		this.setValueContraint(sparqlId, constraintStr);
	}
	
	// intervals.
	public void setNumberInInterval(String sparqlId, Double lowerBound, Double upperBound, Boolean greaterThanOrEqualToLower, Boolean lessThanOrEqualToUpper) throws Exception{
		String constraintStr = ConstraintUtil.getNumberBetweenConstraint(sparqlId, lowerBound.toString(), upperBound.toString(), getTypeName(sparqlId), greaterThanOrEqualToLower, lessThanOrEqualToUpper);
		this.setValueContraint(sparqlId, constraintStr);
	}
	
	public void setDateInInterval(String sparqlId, String lowerBound, String upperBound, Boolean greaterThanOrEqualToLower, Boolean lessThanOrEqualToUpper) throws Exception{
		String constraintStr = ConstraintUtil.getTimePeriodBetweenConstraint(sparqlId, lowerBound, upperBound, getTypeName(sparqlId), greaterThanOrEqualToLower, lessThanOrEqualToUpper);
		this.setValueContraint(sparqlId, constraintStr);
	}
	
	
	// greater or less than.
	public void setNumberLessThan(String sparqlId, Double upperBound, Boolean lessThanOrEqualTo) throws Exception{
		String constraintStr = ConstraintUtil.getLessThanConstraint(sparqlId, upperBound.toString(), getTypeName(sparqlId), lessThanOrEqualTo);
		this.setValueContraint(sparqlId, constraintStr);
	}
	
	public void setDateBefore(String sparqlId, String upperBound, Boolean lessThanOrEqualTo) throws Exception{
		String constraintStr = ConstraintUtil.getTimePeriodBeforeConstraint(sparqlId, upperBound, getTypeName(sparqlId), lessThanOrEqualTo);
		this.setValueContraint(sparqlId, constraintStr);
	}
	
	public void setNumberGreaterThan(String sparqlId, Double lowerBound, Boolean greaterThanOrEqualTo) throws Exception{
		String constraintStr = ConstraintUtil.getGreaterThanConstraint(sparqlId, lowerBound.toString(), getTypeName(sparqlId), greaterThanOrEqualTo);
		this.setValueContraint(sparqlId, constraintStr);
	}
	
	public void setDateAfter(String sparqlId, String lowerBound, Boolean greaterThanOrEqualTo) throws Exception{
		String constraintStr = ConstraintUtil.getTimePeriodAfterConstraint(sparqlId, lowerBound, getTypeName(sparqlId), greaterThanOrEqualTo);
		this.setValueContraint(sparqlId, constraintStr);
	}
	
	
	
	// get the value type of the constraint based on the sparqlId
	private String getTypeName(String sparqlId) throws Exception{
		RuntimeConstrainedObject rco = this.members.get(sparqlId);
		return rco.getValueType();
	}
	
	
	public ValueConstraint getValueConstraint(String itemSparqlId){
		ValueConstraint retval = null;
		
		// check to see if this item is in our list.
		if(members.containsKey(itemSparqlId)){
			retval = members.get(itemSparqlId).getValueConstraint();
		}
		
		return retval;
	}
	
	public String getValueType(String itemSparqlId) throws Exception{
		String retval = null;
		
		// check to see if this item is in our list.
		if(members.containsKey(itemSparqlId)){
			retval = members.get(itemSparqlId).getValueType();
		}
		else{
			throw new Exception(itemSparqlId + " does not exist in the available runtime constrained items.");
		}
		
		return retval;
	}
	
	public void setValueContraint(String itemSparqlId, String constraint) throws Exception{
		
		// check to see if this item is in our list.
		if(members.containsKey(itemSparqlId)){
			members.get(itemSparqlId).setConstraint(constraint);
		}
		else{
			throw new Exception("Constraint setting failed for SparqlId (" + itemSparqlId +
					") because it does not exist in the available runtime constrained items.");
		}
	}
	
	public String getItemType(String itemSparqlId) throws Exception{
		String retval = "";
		
		if(members.containsKey(itemSparqlId)){
			SupportedTypes st = members.get(itemSparqlId).getObjectType();

			retval = st.name();		// get the name.
		}
		else{
			throw new Exception(itemSparqlId + " does not exist in the available runtime constrained items.");
		}
		return retval;
	}
	
	public Table getConstrainedItemsDescription() throws Exception{
		Table retval = null;
		
		ArrayList<ArrayList<String>> itemInfo = new ArrayList<ArrayList<String>>();
		for(String item : this.getConstrainedItemIds()){
			// add each to the table. 
			ArrayList<String> currentItemInfo = new ArrayList<String>();
			
			currentItemInfo.add(item);
			currentItemInfo.add(this.getItemType(item));
			currentItemInfo.add(this.getValueType(item));
			
			// add to outgoing list
			itemInfo.add(currentItemInfo);
		}
		String cols[] = {"valueId", "itemType", "valueType"};
		String type[] = {"string", "string", "string"};
		
		retval = new Table(cols, type, itemInfo);
		
		return retval;
	}
}

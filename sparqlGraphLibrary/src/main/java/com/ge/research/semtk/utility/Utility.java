/*
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

package com.ge.research.semtk.utility;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.resultSet.Table;


/*
 * Utility methods
 */
public abstract class Utility {
	
	private final static Charset CHAR_ENCODING = StandardCharsets.ISO_8859_1; // needed for string compression
	
	public static String SPARQL_QUERY_TRIPLE_COUNT = "select count(*) where {?x ?y ?z}"; // SPARQL query to count triples
	
	public static ArrayList<DateTimeFormatter> DATE_FORMATTERS = new ArrayList<DateTimeFormatter>(); 
	public static ArrayList<DateTimeFormatter> DATETIME_FORMATTERS = new ArrayList<DateTimeFormatter>(); 

	static{
		// supported input date formats 
		/**
		 *  Please keep the wiki up to date
		 *  https://github.com/ge-semtk/semtk/wiki/Ingestion-type-handling
		 */
		
		// need a builder for some special ones
		DateTimeFormatterBuilder builder;
		DateTimeFormatter dateFormat;
		
		// date formatters:
		
		DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
		DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
		DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		
		// case-insensitive dd-MMM-yyyy (e.g. 12-Jun-2008 or 12-JUN-2008)
		builder = new DateTimeFormatterBuilder();
		builder.parseCaseInsensitive().appendPattern("dd-MMM-yyyy");
		dateFormat = builder.toFormatter();
		DATE_FORMATTERS.add(dateFormat);
		
		// date time formatters:
		
		DATETIME_FORMATTERS.add(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"));
		DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss"));
		DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
		DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss"));
	
		// case-insensitive dd-MMM-yyyy HH:mm:ss (e.g. 12-Jun-2008 05:00:00 or 12-JUN-2008 05:00:00)
		builder = new DateTimeFormatterBuilder();
		builder.parseCaseInsensitive().appendPattern("dd-MMM-yyyy HH:mm:ss");
		dateFormat = builder.toFormatter();
		DATETIME_FORMATTERS.add(dateFormat);
	}

	/**
	 * Gets a CSV record string from an arraylist of Strings.
	 * Does not include a record separator (will not append \n at the end of the line)
	 */
	public static String getCSVString(ArrayList<String> row) throws IOException{
		StringWriter stringWriter = new StringWriter();
		CSVFormat csvFormat = CSVFormat.EXCEL.withRecordSeparator("");  // don't include a record separator
		CSVPrinter csvPrinter = new CSVPrinter(stringWriter, csvFormat);		
		csvPrinter.printRecord(row);
		csvPrinter.close();
		
		String ret = stringWriter.toString();
		stringWriter.close();
		return ret;
		
	}
	
	public static JSONArray getJsonArrayFromString(String s) throws Exception{
		return (JSONArray) (new JSONParser()).parse(s);
	}
	
	public static JSONArray getJsonArray(String[] arr) throws Exception{
		JSONArray ret = new JSONArray();
		for(String s : arr){
			ret.add(s);
		}
		return ret;
	}
	
	public static JSONObject getJsonObjectFromString(String s) throws Exception{
		return (JSONObject) (new JSONParser()).parse(s);
   	}
	
	/**
	 * Get the contents of a URL as a string
	 * @throws IOException 
	 */
	public static String getURLContentsAsString(URL url) throws IOException{
		StringBuffer ret = new StringBuffer();
		BufferedReader br = null;
		try{
			URLConnection conn = url.openConnection();
			br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				ret.append(line).append("\n");
			}
		}finally{
			if(br != null){
				br.close();
			}
		}
		return ret.toString();
	}
	
	public static Table getURLResultsContentAsTable(URL url) throws Exception{
		Table retval = null;
		
		// first, get the content
		String contents = getURLContentsAsString(url);
		
		// split it into lines.
		String[] lines = contents.split("\n");
		
		if(lines.length > 0){
			// get our columns
			String[] colName = lines[0].split(",");
			String[] colType = new String[colName.length];
			
			// add col type info
			for(int i = 0; i < colName.length; i++){
				colType[i] = "string";
			}
			
			// make arraylist of the remaining values
			ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
			if(lines.length >=2 ){
				for(int k = 1; k < lines.length; k++){
					ArrayList<String> curr = new ArrayList<String>();
					// add each element
					for(String t : lines[k].split(",")){
						curr.add(t);
					}
					// add to the rows collection
					rows.add(curr);
				}
			}
			
			retval = new Table(colName, colType, rows);
			
		}
		else{
			// do nothing. 
		}
		
		return retval;
	}
	
	
	/**
	 * Determine if two String arrays have the same elements, ignoring order
	 */
	public static boolean arraysSameMinusOrder(String[] arr1, String[] arr2) {
		String[] arr1Clone = arr1.clone();	// clone so as to not reorder the actual array passed in here
		String[] arr2Clone = arr2.clone();
	    Arrays.sort(arr1Clone);
	    Arrays.sort(arr2Clone);
	    return Arrays.equals(arr1Clone, arr2Clone);
	}
	
	/**
	 * Create a JSON object from a path to a JSON file
	 * @param jsonFilePath the file path
	 * @return the JSON object
	 * @throws Exception 
	 */
	public static JSONObject getJSONObjectFromFilePath(String jsonFilePath) throws Exception{
		
		// validate that file has a .json extension
		if(!jsonFilePath.endsWith(".json")){
			throw new Exception("Error: File " + jsonFilePath + " is not a JSON file");
		}
		
		// load the file
		File jsonFile = null;	
		try {
			jsonFile = new File(jsonFilePath);
		} catch (Exception e) {
			throw new Exception("Could not find JSON file " + jsonFilePath);
		}
			
		// load JSON file to JSON object
		JSONObject jsonObject = null;
		try{
			jsonObject = Utility.getJSONObjectFromFile(jsonFile);	
		}catch (Exception e){
			throw new Exception("Could not load JSON from file " + jsonFilePath);
		}
		
		return jsonObject;
	}

	public static JSONArray getJSONArrayFromFilePath(String jsonFilePath) throws Exception{
		
		// validate that file has a .json extension
		if(!jsonFilePath.endsWith(".json")){
			throw new Exception("Error: File " + jsonFilePath + " is not a JSON file");
		}
		
		// load the file
		File jsonFile = null;	
		try {
			jsonFile = new File(jsonFilePath);
		} catch (Exception e) {
			throw new Exception("Could not find JSON file " + jsonFilePath);
		}
			
		// load JSON file to JSON object
		JSONArray jsonArr = null;
		try{
			jsonArr = Utility.getJSONArrayFromFile(jsonFile);	
		}catch (Exception e){
			throw new Exception("Could not load JSON from file " + jsonFilePath);
		}
		
		return jsonArr;
	}

	/**
	 * Get a JSON object from a file
	 * @param f the file
	 * @return the JSON object
	 */
	public static JSONObject getJSONObjectFromFile(File f) throws Exception{
		InputStreamReader reader = new InputStreamReader(new FileInputStream(f.getAbsolutePath()));
		return (JSONObject) (new JSONParser()).parse(reader);
	}	

	/**
	 * Get a JSON array from a file
	 * @param f the file
	 * @return the JSON array
	 */
	public static JSONArray getJSONArrayFromFile(File f) throws Exception{
		InputStreamReader reader = new InputStreamReader(new FileInputStream(f.getAbsolutePath()));
		return (JSONArray) (new JSONParser()).parse(reader);	
	}
	
	/**
	 * Get colName entries from the import spec portion of the JSON template.
	 * Converts all column names to lower case.
	 * @param json the JSON object
	 * @return an array of column names, with no duplicates
	 */
	//public static String[] getColNamesFromJSONTemplate(JSONObject json){
	// MOVED TO ImportSpecHandler	
	
	
	/**
	 * Create a SPARQL-friendly string (e.g. 2011-12-03T10:15:30) from a date time string. 
	 */
	public static String getSPARQLDateTimeString(String s) throws Exception{

		// ISO_OFFSET_DATE_TIME is the only valid format with timezone
		// Try it first
		// If it succeeds then return as-is, since it is also valid SPARQL
		try{
			ZonedDateTime zonedObj = ZonedDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			return s;
		} catch (Exception e) {
        	// move on
        }
		
		// try all formatters until find one that works
		for (DateTimeFormatter formatter : DATETIME_FORMATTERS){  
	        try{        	
	        	LocalDateTime dateObj = LocalDateTime.parse(s, formatter);
	        	return dateObj.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);				 
	        }catch (Exception e) {
	        	// try the next one
	        }
		}			
		
		// none of the datetime formatters worked.  Try formatting as date and appending T00:00:00
		try{
			s = getSPARQLDateString(s); 
			s += "T00:00:00";
			return s; 
		}catch(Exception e){
			// move on 
		}
		
		throw new Exception("Cannot parse " + s + " using available formatters");
	}
	
	/**
	 * Create a SPARQL-friendly string (e.g. 2011-12-03) from a date string
	 */
	public static String getSPARQLDateString(String s) throws Exception{
		// try all formatters until find one that works
		for (DateTimeFormatter formatter : DATE_FORMATTERS){  
	        try{        	
	        	LocalDate dateObj = LocalDate.parse(s, formatter);
				return dateObj.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")); 	
	        }catch (Exception e) {
	        	// try the next one
	        }
		}				
		throw new Exception("Cannot parse " + s + " using available formatters");
	}
	
	/**
	 * Create a SPARQL-friendly string (e.g. 2017-06-13) for the current date.
	 */
	public static String getSPARQLCurrentDateString() throws Exception {
		String s = (new SimpleDateFormat("MM/dd/YYYY")).format(new Date());
		return getSPARQLDateString(s);
	}
	
	
	/**
	 * Retrieve a property from a properties file.
	 * @param propertyFile the property file
	 * @param key the name of the property to retrieve
	 */
	public static String getPropertyFromFile(String propertyFile, String key) throws Exception{
	
		Properties properties = new Properties();
		try {
			properties.load(new FileReader(new File(propertyFile)));
		} catch (Exception e) {
		    throw new Exception("Cannot load properties file " + propertyFile, e);
		}
		// now read the property		
		String ret = properties.getProperty(key);
		if(ret == null){
		    throw new Exception("Cannot read property '" + key + "' from " + propertyFile);	
		}
		
		return ret.trim();
	}	

	/**
	 * Compress a string.
	 */
	public static String compress(String s) throws Exception {  
		byte[] inputBytes = s.getBytes(CHAR_ENCODING);
		Deflater deflater = new Deflater();  
		deflater.setInput(inputBytes);  
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(inputBytes.length);   
		deflater.finish();  
		byte[] buffer = new byte[1024];   
		while (!deflater.finished()) {  
			int count = deflater.deflate(buffer); 
			outputStream.write(buffer, 0, count);   
		}  
		outputStream.close();  
		byte[] compressedBytes = outputStream.toByteArray();  
		return new String(compressedBytes, CHAR_ENCODING);
	}  

	/**
	 * Decompress a string.
	 */
	public static String decompress(String s) throws Exception {   
		byte[] inputBytes = s.getBytes(CHAR_ENCODING);
		Inflater inflater = new Inflater();   
		inflater.setInput(inputBytes);  
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(inputBytes.length);  
		byte[] buffer = new byte[1024];  
		while (!inflater.finished()) {  
			int count = inflater.inflate(buffer);  
			outputStream.write(buffer, 0, count);  
		}  
		outputStream.close();  
		byte[] decompressedBytes = outputStream.toByteArray();  
		return new String(decompressedBytes, CHAR_ENCODING);
	}  

}

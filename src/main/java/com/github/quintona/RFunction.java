package com.github.quintona;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import backtype.storm.tuple.Values;

import storm.trident.operation.BaseFunction;
import storm.trident.operation.TridentCollector;
import storm.trident.operation.TridentOperationContext;
import storm.trident.tuple.TridentTuple;

public class RFunction extends BaseFunction {
	
	Process process;
	DataOutputStream rInput;
	String rExecutable;
	List<String> libraries;
	String functionName;
	BufferedReader reader;
	static String ls = System.getProperty("line.separator");
	private String initCode = null;
	
	public static final String START_LINE = "<s>";
	public static final String END_LINE = "<e>";
	
	public RFunction(String rExecutable, List<String> libraries, String functionName){
		this.rExecutable = rExecutable;
		this.functionName = functionName;
		this.libraries = libraries;
	}
	
	public RFunction(List<String> libraries, String functionName){
		rExecutable = "/usr/bin/R";
		this.functionName = functionName;
		this.libraries = libraries;
	}
	
	public RFunction withInitCode(String rCode){
		this.initCode = rCode;
		return this;
	}
	
	public RFunction withNamedInitCode(String name){
		this.initCode = readFile("/" + name + ".R");
		return this;
	}
	
	public void prepare(Map conf, TridentOperationContext context) {
		ProcessBuilder builder = new ProcessBuilder(rExecutable, "--vanilla", "-q", "--slave");
		try {
			process = builder.start();
			rInput = new DataOutputStream(process.getOutputStream());
			reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			loadLibraries();
			if(initCode != null){
				rInput.writeBytes(initCode + "\n");
				rInput.flush();
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not start R, please check install and settings" + e);
		}
    }
	
	private void loadLibraries() throws IOException{
		rInput.writeBytes("library('rjson')\n");
		for(String lib : libraries){
			rInput.writeBytes("library('"+ lib +"')\n");
		}
		rInput.flush();
	}
	
	private static String trimOutput(String output){
		output = output.replace("[1]", "");
		output = output.replace("\\", "");
		output = output.trim();
		return output.substring(1, output.length() - 1);
	}
	
	private JSONArray getResult() throws ParseException{
		StringBuilder stringBuilder = new StringBuilder();
		boolean awaitingStart = true;
		try {
			//This first read is the slow blocking one. It waits for the R process to run
        	String line = reader.readLine();
			while (line != null) {
				System.out.println(line);
				if(line.equals(START_LINE)){
					awaitingStart = false;
				} else if(line.equals(END_LINE)) {
					if(awaitingStart)
						throw new RuntimeException("Something went wrong. Received response ending before beginning!");
                    break;
                } else if(!awaitingStart){
					stringBuilder.append(line);
					stringBuilder.append(ls);
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(awaitingStart)return null;
		String trimmedContent = trimOutput(stringBuilder.toString());
        return (JSONArray)JSONValue.parseWithException(trimmedContent);
	}
	
	public static String readFile(String file) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				RFunction.class.getResourceAsStream(file)));
		String line = null;
		StringBuilder stringBuilder = new StringBuilder();

		try {
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
				stringBuilder.append(ls);
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not load resource: " + e);
		}
		return stringBuilder.toString();
	}

    @Override
    public void cleanup() {
    	process.destroy();
    }
    
    public JSONArray coerceTuple(TridentTuple tuple){
    	JSONArray array = new JSONArray();
    	array.addAll(tuple);
    	return array;
    }
    
    public Values coerceResponce(JSONArray array){
    	return new Values(array.toArray());
    }
    
    public JSONArray performFunction(JSONArray functionInput){
    	try {
    		String input = functionInput.toJSONString();
    		input = input.replace("\\", "");
			rInput.writeBytes("list <- fromJSON('" + input + "')\n");
			rInput.writeBytes("output <- " + functionName + "(list)\n");
			rInput.writeBytes("write('" + START_LINE + "', stdout())\n");
			rInput.writeBytes("toJSON(output)\n");
			rInput.writeBytes("write('" + END_LINE + "', stdout())\n");
			rInput.flush();
			return getResult();
		} catch (IOException | ParseException e) {
			throw new RuntimeException("Exception handling response from R" + e);
		}
    }

	@Override
	public void execute(TridentTuple tuple, TridentCollector collector) {
		JSONArray functionInput = coerceTuple(tuple);
		collector.emit(coerceResponce(performFunction(functionInput)));
	}

	

}

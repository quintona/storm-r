package com.github.quintona;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import org.json.simple.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.perf4j.LoggingStopWatch;
import org.perf4j.StopWatch;

import com.github.quintona.RFunction;

import storm.trident.testing.MockTridentTuple;
import storm.trident.tuple.TridentTuple;

@RunWith(Parameterized.class)
public class TestRecommendation {
	
	@Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { new String[]{"liquor","red/blush wine"}, "bottled beer" }, 
                { new String[]{"citrus fruit",                                                   
                		   "other vegetables",                                                          
                		   "soda",                                                                      
                		   "fruit/vegetable juice"}, "root vegetables" } });
    }
    
    String[] inputs;
    String expected;
    
    public TestRecommendation(String[] inputs, String expected){
    	this.inputs = inputs;
    	this.expected = expected;
    }
    
    private String[] getNames(int count){
    	String[] names = new String[count];
    	for(int i = 0; i < count; i++){
    		names[i] = "val" + i;
    	}
    	return names;
    }

	@Test
	public void test() {
		TridentTuple values = new MockTridentTuple(Arrays.asList(getNames(inputs.length)), Arrays.asList(inputs));

		RFunction function = new RFunction(
				Arrays.asList(new String[] { "arules" }), "recommend")
				.withNamedInitCode("recommend");
		
		function.prepare(null, null);
		StopWatch stopWatch = new LoggingStopWatch("First Run");
		JSONArray array = function.coerceTuple(values);
		JSONArray result = function.performFunction(array);
		stopWatch.stop();
		assertEquals(expected,result.get(0).toString());

		for(int i = 0; i < 3;i++){
			stopWatch = new LoggingStopWatch("Run " + i);
			array = function.coerceTuple(values);
			result = function.performFunction(array);
			stopWatch.stop();
		}
	}

}

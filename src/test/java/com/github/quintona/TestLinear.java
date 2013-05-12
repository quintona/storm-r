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

import backtype.storm.tuple.Values;

import com.github.quintona.RFunction;

import storm.trident.testing.MockTridentTuple;
import storm.trident.tuple.TridentTuple;

@RunWith(Parameterized.class)
public class TestLinear {
	
	@Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { new Integer[]{2005, 2010}, new Double[]{5.683, 2.158} }, 
                { new Integer[]{2006, 2007, 2008, 2009}, new Double[]{4.978, 4.273, 3.568, 2.863 } } });
    }
    
    Integer[] inputs;
    Double[] expected;
    
    public TestLinear(Integer[] inputs, Double[] expected){
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
				Arrays.asList(new String[] {}), "predict_linear")
				.withNamedInitCode("linear");
		
		function.prepare(null, null);
		StopWatch stopWatch = new LoggingStopWatch("First Run");
		JSONArray array = function.coerceTuple(values);
		JSONArray result = function.performFunction(array);
		stopWatch.stop();
		Double[] actuals = (Double[])result.toArray(new Double[0]);
		for(int i = 0; i < expected.length; i++){
			assertEquals(expected[i],actuals[i],0.01);
		}
		

		for(int i = 0; i < 3;i++){
			stopWatch = new LoggingStopWatch("Run " + i);
			array = function.coerceTuple(values);
			result = function.performFunction(array);
			stopWatch.stop();
		}
	}

}

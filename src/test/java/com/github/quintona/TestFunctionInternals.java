package com.github.quintona;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestFunctionInternals {

	@Test
	public void test() {
		String output = "[1] \"[]\"";
		String result = RFunction.trimOutput(output);
		assertEquals("[]",result);
	}

}

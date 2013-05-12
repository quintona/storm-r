#Storm.R

This project provides a trident function that enables integration with R functions. 

In an ideal world we would have all the statistical and ML functions we would like available within Storm, until that day arrives, there are going to be certain things that we need to use that exist within the R community but have yet to be implemented within something like [Storm Pattern](https://github.com/quintona/storm-pattern) or [Trident ML](https://github.com/pmerienne/trident-ml). For that reason I have implemented a small bridege between Storm and R. This bridge is based on similar principle to the multi-lang concepts already in storm, except that I implemented it to be far more light weight a protocol for R, because it doesn't have to nearly as generic as the actual multi-lang protocol. Furthermore I could take care of much of the coersion between tuples and vectors, making the job of the developer much simpler than in the case of the multi-lang protocol. 

I did investigate other options such as JRI and a java based R interpretter, but they not viable either because of maturity or theading models that are too restrictive in the parallel processing cases. 

## Use Cases

I have implemented an example for Association Rules, and a Linear model (take a look at the unity tests for details). From a learning perspective, R is probably one of the best places to do these things, from a scoring perspective, maybe not as it doesn't perform brilliantly, but when you don't have a choice then you have to make do. For the ARules case, once things have warmed up the prediction function represents a massive 30ms overhead (ouch), but the linear case is only 1ms. The bridge then doesn't represent a massive overhead, you simply have to live with the performance of R, but it does scale with Storm :)

It is therefore still very useful, given the range of functionality in R (massive!)

## Usage

I haven't released this into a binary repo yet, so clone it and run "mvn clean:install". Add the dependency:

	<dependency>
  		<groupId>com.github.quintona</groupId>
  		<artifactId>storm-r</artifactId>
  		<version>0.0.1-SNAPSHOT</version>
	</dependency>
	
Then include the function in your trident stream:

	topology.newStream("valueStream", spout)
				.each(new Fields(fields), new RFunction(Arrays.asList(new String[] {}, 				"predict_linear").withNamedInitCode("linear"),
						new Fields("rate"))
						
I have chosen convention over configuration here, so the assumption inherent is that there is a single R function, that takes a vector as input and returns a vector as output (sound familiar?). You can supply the function in 3 ways:

* Via an R package
* Via a string passed to the .withInitCode function
* Via a file on the root of the classpath, you can then pass the name with .withNamedInitCode

The constructor takes a list of packages that are required, and the name of the function as args. 

Ensure that you have installed all required packages to the instance of R on each of the storm nodes, the libraries will get loaded, not installed. 

The example above uses the last method and the R code is as follows:

	year <- c(2000 ,   2001  ,  2002  ,  2003 ,   2004)
	rate <- c(9.34 ,   8.50  ,  7.62  ,  6.93  ,  6.60)
	fit <- lm(rate ~ year)

	predict_linear <- function(list){
		as.vector(predict(fit, data.frame(year=list)))
	}

Obviously you would want to load a more useful model that had been generated elsewhere...

## Future Additions

* Support for traditional Storm functionality, not only Trident
* More examples
* Add crates to storm deploy for the R installations
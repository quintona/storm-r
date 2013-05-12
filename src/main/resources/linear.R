year <- c(2000 ,   2001  ,  2002  ,  2003 ,   2004)
rate <- c(9.34 ,   8.50  ,  7.62  ,  6.93  ,  6.60)
fit <- lm(rate ~ year)

predict_linear <- function(list){
	as.vector(predict(fit, data.frame(year=list)))
}
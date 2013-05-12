data("Groceries")
rules <- apriori(Groceries, parameter = list(supp = 0.001, conf = 0.8))
recommend <- function(list){
	rules.found <- subset(rules, subset = lhs %ain% list & lift > 1.3)
	as(rhs(rules.found), "list")
}
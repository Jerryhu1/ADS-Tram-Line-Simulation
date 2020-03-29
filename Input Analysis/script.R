library('e1071')

data <- read.csv("B:/School/Master/Algorithms for Decision Support/IDS-Assignment/ADS-Assignment/data/runtimes.csv", sep = ';')


col1 <- data[,2]

cv <- function(data){
  sd(data)/mean(data)
}

stats <- function(data){
  skewness(data)
  cv(data)
  summary(data)
}

lexis <- function(data){
  sd(data)^2/mean(data)
}
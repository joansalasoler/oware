# Creates a drop-out diagram from a CSV file
# 

leaves <- read.csv("leaves.csv")
groups <- data.frame(table(leaves$score, leaves$depth))
colnames(groups) <- c("score", "depth", "freq")

groups <- transform(
    groups,
    score = as.numeric(as.character(score)),
    depth = as.numeric(as.character(depth))
)

# groups <- subset(groups, freq > 1)

symbols(
    groups$score,
    groups$depth,
    circles=groups$freq,
    fg = "blue",
    bg = "blue",
    xlab = "Score",
    ylab = "Depth",
    inches = 0.1
)

# Marvi Hoxha – Project Portfolio

Master's student in Business Informatics at Philipps-Universität Marburg. This repository collects university projects in Java and Python.

## Projects

### [Clean SVG Export for Henshin](clean-svg-export-henshin/)
**Java · Gradle · JUnit · team project (ongoing)**

[Henshin](https://projects.eclipse.org/projects/modeling.emft.henshin) is an Eclipse-based tool for modelling software transformation rules. This team project builds a tool that turns those rules into clean, scalable SVG images.

My part was most of the Henshin integration:
- reading Henshin rule files (XML) and converting each rule into a Java model
- drawing each rule as an SVG diagram in the style of the Eclipse editor
- a command-line tool to list rules and export one or all of them as images
- 29 automated JUnit tests, using a sample banking model

Example output: [`examples/bank`](clean-svg-export-henshin/examples/bank)

### [Applications of AI in Business](applications-of-ai-in-business/)
**Python · pandas · scikit-learn · mlxtend · team project**

Data analysis for a targeted advertising campaign, based on 2,800+ historical sales records. The pipeline answers five business questions:

| Question | Method |
|---|---|
| Who should we target? | RFM analysis and K-Means customer segmentation |
| Where should we advertise? | Country and city market analysis |
| What should we promote? | Product-line performance |
| When should campaigns run? | Monthly and quarterly sales patterns |
| Which products sell together? | Association rules (Apriori) |

Key results: 2 VIP customers generated 15% of total revenue, November was the peak month (22% of revenue), and the strongest product pair had a lift of 2.4.

Details and how to run it: [project README](applications-of-ai-in-business/README.md)

## Contact
[LinkedIn](https://www.linkedin.com/in/marvi-hoxha-935a14255)

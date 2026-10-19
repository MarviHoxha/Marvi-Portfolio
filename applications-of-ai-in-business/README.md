# MVP Targeted Advertising Analysis

This project analyzes historical sales data for **Marburg Vehicles and Parts (MVP)**.  
The goal is to support a targeted advertising campaign by using data analysis and machine learning to understand customer behavior, market performance, product-line performance, seasonal sales behavior, and possible cross-selling opportunities.

The project helps answer the following business questions:

- **Who** should MVP target?
- **Where** should MVP advertise?
- **What** products should MVP promote?
- **When** should campaigns be launched?
- **Which** products can be advertised together?

---

## 1. Clone the Project

To download the project, open Git Bash, Command Prompt, PowerShell, or a terminal and run:

```bash
git clone https://gitlab.uni-marburg.de/hoxham/mvp-dataset.git
```

Then move into the project folder:

```bash
cd mvp-dataset
```

---

## 2. Project Structure

```text
mvp-dataset/
├── data/
│   └── sales_data_sample.csv
│
├── outputs/
│   └── generated analysis results and plots
│
├── forecast_results/
│   └── generated forecasting results
│
├── src/
│   ├── data_loader.py
│   ├── rfm.py
│   ├── customer_insights.py
│   ├── market_insights.py
│   ├── product_insights.py
│   ├── seasonal_insights.py
│   ├── product_analysis.py
│   ├── seasonal.py
│   ├── crossselling.py
│   └── forecasting.py
│
├── main.py
├── README.md
└── .gitignore
```

The main file to run is:

```text
main.py
```

This file runs the complete analysis pipeline.

---

## 3. Dataset

The dataset is stored in:

```text
data/sales_data_sample.csv
```

It contains historical sales order-line data from 2003 to 2005.

Important columns include:

- `ORDERNUMBER`: order identifier
- `QUANTITYORDERED`: number of units ordered
- `PRICEEACH`: unit price
- `SALES`: sales value for the order line
- `ORDERDATE`: date of the order
- `STATUS`: order status
- `PRODUCTLINE`: product category
- `PRODUCTCODE`: product identifier
- `CUSTOMERNAME`: customer name
- `CITY`, `COUNTRY`, `TERRITORY`: geographic information
- `DEALSIZE`: size category of the order
- `MSRP`: manufacturer’s suggested retail price

---

## 4. Install Requirements

The project uses Python.

Required libraries:

- pandas
- matplotlib
- scikit-learn
- mlxtend

Install them with:

```bash
python -m pip install pandas matplotlib scikit-learn mlxtend
```

On Windows, if `python` does not work, use:

```bash
py -m pip install pandas matplotlib scikit-learn mlxtend
```

If you are using a virtual environment, activate it before installing the packages.

---

## 5. How to Run the Project

From the project root folder, run:

```bash
python main.py
```

On Windows, you can also use:

```bash
py main.py
```

If you are using the local virtual environment, run:

```bash
.\.venv\Scripts\python.exe main.py
```

After running the script, the generated results will be saved in:

```text
outputs/
forecast_results/
```

---

## 6. What Happens When You Run `main.py`

Running `main.py` will:

1. Load the dataset from `data/sales_data_sample.csv`
2. Preprocess and clean the data
3. Run customer segmentation analysis
4. Run market analysis
5. Run product-line business insight analysis
6. Run seasonal sales analysis
7. Run cross-selling analysis, if included in the current pipeline
8. Run forecasting analysis, if included in the current pipeline
9. Save CSV files and plots into the output folders

---

## 7. Module Overview

### `src/data_loader.py`

Loads and preprocesses the dataset.

Main tasks:

- reads the CSV file
- keeps relevant records
- converts order dates
- removes unnecessary columns
- handles missing values
- removes duplicates
- encodes selected categorical values
- scales selected numerical columns for machine learning

---

### `src/rfm.py`

Creates customer-level RFM features and performs customer segmentation.

RFM means:

- **Recency**: how recently a customer purchased
- **Frequency**: how often a customer purchased
- **Monetary**: how much revenue the customer generated

K-Means clustering is used to group customers into meaningful customer segments.

This helps answer:

```text
Who should MVP target?
```

---

## 8. Business Insight Modules

The business insight modules convert the technical analysis into business-friendly summaries, CSV files, and visualizations. These modules are important because they connect the code output to MVP’s targeted advertising goal.

---

### `src/customer_insights.py`

Creates customer segmentation business insights from the RFM clustering results.

This module generates:

- customer segment summaries
- number of customers per segment
- average recency, frequency, and monetary value per segment
- business recommendations for each segment
- customer segmentation output files and plots

This helps MVP identify customer groups such as:

- VIP customers
- regular customers
- occasional customers
- at-risk high-value customers

Business question answered:

```text
Who should MVP target?
```

Example business use:

- VIP customers can receive premium offers.
- At-risk high-value customers can receive reactivation campaigns.
- Regular customers can receive loyalty campaigns.
- Occasional customers can receive general promotions.

---

### `src/market_insights.py`

Creates market-level business insights using country and city information.

This module generates:

- country-level sales summaries
- city-level sales summaries
- top countries by total sales
- countries with strong average order value
- market visualizations

Business question answered:

```text
Where should MVP advertise?
```

Example business use:

- Countries with high total sales can receive larger advertising budgets.
- Countries with high average order value can receive premium-focused campaigns.
- Strong cities can be prioritized for localized advertising.

---

### `src/product_insights.py`

Creates product-line business insights.

This module analyzes:

- total sales by product line
- total quantity ordered by product line
- number of orders per product line
- number of customers per product line
- product-line performance by country
- best product line per country

Business question answered:

```text
What should MVP advertise?
```

Example business use:

- High-revenue product lines should be promoted more strongly.
- Product lines that perform well in specific countries can be used for localized campaigns.
- Product-market fit can be identified by comparing product-line performance across countries.

---

### `src/seasonal_insights.py`

Creates seasonal sales business insights.

This module analyzes:

- monthly sales trends
- quarterly sales trends
- strongest months
- strongest quarters
- general sales patterns over time

Business question answered:

```text
When should MVP advertise?
```

Example business use:

- MVP can launch campaigns before strong sales periods.
- If certain months or quarters show higher sales, advertising can be planned around those periods.
- Seasonal trends can improve campaign timing.

---

## 9. Other Analysis Modules

### `src/product_analysis.py`

Analyzes additional product-line performance and pricing-related information.

This module may include:

- product-line revenue analysis
- product demand analysis
- pricing comparison between actual unit price and MSRP
- product-level business summaries

This supports the product promotion part of the project.

---

### `src/seasonal.py`

Analyzes seasonal behavior in the dataset.

This module may include:

- monthly sales
- quarterly sales
- yearly sales
- product-line seasonality

This supports the campaign timing part of the project.

---

### `src/crossselling.py`

Finds product combinations using association rule mining.

This module identifies products or product lines that are often bought together.

The main metrics used are:

- **Support**
- **Confidence**
- **Lift**

This helps MVP create bundled offers and product recommendation campaigns.

Business question answered:

```text
Which products can MVP advertise together?
```

---

### `src/forecasting.py`

Creates simple country-level sales forecasts using historical sales trends.

The forecasting results should be interpreted carefully because the dataset covers only a limited historical time period. The forecasting part is useful for exploring possible sales trends, but the strongest business insights come from customer segmentation, market analysis, product-line analysis, seasonal sales behavior, and cross-selling.

---

## 10. Generated Outputs

After running:

```bash
python main.py
```

the project creates results in the following folders.

### `outputs/`

This folder contains generated analysis results and plots, such as:

- customer segmentation outputs
- segment summary files
- elbow method plot
- market analysis summaries
- product-line analysis results
- monthly sales analysis
- quarterly sales analysis
- yearly sales analysis
- product-line seasonality results
- cross-selling association rules
- visualizations for the final report

### `forecast_results/`

This folder contains forecasting results, such as:

- country-level forecast results
- forecast metrics
- forecast plots for countries with enough historical data

---

## 11. Business Questions Answered

| Business Question | Analysis Used |
|---|---|
| Who should MVP target? | RFM analysis, K-Means clustering, and `customer_insights.py` |
| Where should MVP advertise? | Country and city-level market analysis using `market_insights.py` |
| What should MVP advertise? | Product-line performance analysis using `product_insights.py` |
| When should campaigns be launched? | Monthly and quarterly sales analysis using `seasonal_insights.py` |
| Which products can be advertised together? | Cross-selling and association rule mining |
| How might sales develop in some markets? | Country-level forecasting |

---

## 12. Recommended Workflow for a New User

A new user can run the full project with these commands:

```bash
git clone https://gitlab.uni-marburg.de/hoxham/mvp-dataset.git
cd mvp-dataset
python -m pip install pandas matplotlib scikit-learn mlxtend
python main.py
```

Then check the generated result folders:

```text
outputs/
forecast_results/
```

---

## 13. Notes

This project was created for the course **Applications of AI in Business**.

The main goal is not only technical modeling, but also deriving useful business insights for MVP’s targeted advertising campaign.

The most important project insights come from:

- customer segmentation
- market analysis
- product-line performance
- seasonal sales behavior
- cross-selling rules
- forecasting as an additional exploratory method

The forecasting results should be interpreted with caution because the dataset only covers a limited time period.
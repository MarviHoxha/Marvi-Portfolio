# main.py

import os
from sklearn.preprocessing import StandardScaler
from src.forecasting import forecast_country_sales
from src.data_loader import load_data, preprocess_data
from src.rfm import build_rfm_table, find_optimal_k, plot_elbow, run_rfm_clustering
from src.customer_insights import CustomerSegmentationInsight
from src.product_insights import ProductInsight
from src.market_insights import MarketInsight
from src.seasonal_insights import SeasonalInsight

from src.product_analysis import analyse_product_lines, analyse_pricing
from src.seasonal import (
    analyse_monthly_sales,
    analyse_quarterly_sales,
    analyse_yearly_sales,
    analyse_product_line_seasonality,
    find_peak_months,
    plot_monthly_sales,
    plot_quarterly_sales
)
from src.crossselling import (
    format_rules_for_export,
    keep_strongest_single_item_rules,
    plot_top_rules_by_lift,
    print_top_rules,
    run_apriori
)


# Create outputs folder if missing
os.makedirs("outputs", exist_ok=True)

# 1. Load + preprocess data
df = load_data("data/sales_data_sample.csv")
df, df_scaled = preprocess_data(df)

# 2. Find optimal k (Elbow Method)
rfm = build_rfm_table(df)
rfm_scaled = StandardScaler().fit_transform(rfm[["Recency", "Frequency", "Monetary"]])
inertias = find_optimal_k(rfm_scaled)
plot_elbow(inertias, save_path="outputs/elbow.png")


# 3. Run clustering with chosen k = 3
clustered = run_rfm_clustering(df, n_clusters=3)
customer_insight = CustomerSegmentationInsight(output_dir="outputs")
segment_summary = customer_insight.run(clustered)
print(clustered.head())
print(clustered["Segment"].value_counts())

# 5. Market analysis business insight
market_insight = MarketInsight(output_dir="outputs")
country_summary, city_summary = market_insight.run(df)

# 6. Product-line performance business insight
product_insight = ProductInsight(output_dir="outputs")
productline_summary, productline_country_matrix, best_productline_by_country = product_insight.run(df)

# 7. Seasonal sales business insight
seasonal_insight = SeasonalInsight(output_dir="outputs")
monthly_summary, quarterly_summary, month_pattern = seasonal_insight.run(df)

# 4. Inspect results
print("\n=== Cluster sizes ===")
print(clustered["Segment"].value_counts())

print("\n=== Cluster characteristics (averages) ===")
print(clustered.groupby("Segment")[["Recency", "Frequency", "Monetary"]].mean().round(0))

print("\n=== Sample customers per segment ===")
print(clustered.sort_values("Segment").head(20))

# Use df (unscaled!), not df_scaled
print("\n=== Product Line Performance ===")
print(analyse_product_lines(df))

print("\n=== Pricing Analysis ===")
print(analyse_pricing(df))

# 5. Seasonal Sales Behavior
print("\n=== Seasonal Sales Behavior: Monthly Sales ===")
monthly_sales = analyse_monthly_sales(df)
print(monthly_sales)
monthly_sales.to_csv("outputs/monthly_sales.csv", index=False)

print("\n=== Seasonal Sales Behavior: Quarterly Sales ===")
quarterly_sales = analyse_quarterly_sales(df)
print(quarterly_sales)
quarterly_sales.to_csv("outputs/quarterly_sales.csv")

print("\n=== Seasonal Sales Behavior: Yearly Sales ===")
yearly_sales = analyse_yearly_sales(df)
print(yearly_sales)
yearly_sales.to_csv("outputs/yearly_sales.csv")

print("\n=== Product-Line Seasonality by Quarter ===")
product_line_seasonality = analyse_product_line_seasonality(df)
print(product_line_seasonality)
product_line_seasonality.to_csv("outputs/product_line_seasonality.csv")

print("\n=== Peak Months for Campaign Timing ===")
print(find_peak_months(df, top_n=3))

plot_monthly_sales(df, save_path="outputs/monthly_sales.png")
plot_quarterly_sales(df, save_path="outputs/quarterly_sales.png")

# 6. Cross-Selling Analysis with Apriori
print("\n=== Cross-Selling Analysis ===")

rules = run_apriori(
    df,
    item_column="PRODUCTLINE",
    min_support=0.03,
    min_confidence=0.30
)

simple_rules = keep_strongest_single_item_rules(rules)

if not simple_rules.empty:
    print_top_rules(simple_rules, top_n=10)
    format_rules_for_export(simple_rules).to_csv(
        "outputs/association_rules.csv",
        index=False
    )
    print("\nAssociation rules saved to outputs/association_rules.csv")
else:
    print("No simple one-to-one association rules found.")

plot_top_rules_by_lift(
    simple_rules,
    top_n=10,
    save_path="outputs/top_apriori_rules_by_lift.png"
)

def main():

    print("Linear Regression Country Sales Forecasting")

    forecast_country_sales(df)


if __name__ == "__main__":
    main()


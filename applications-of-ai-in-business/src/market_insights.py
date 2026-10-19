import os
import pandas as pd
import matplotlib.pyplot as plt


class MarketInsight:
    """
    Creates business insight outputs for market analysis.
    This class helps identify which countries and cities generate
    the highest sales and where MVP should focus advertising.
    """

    def __init__(self, output_dir: str = "outputs"):
        self.output_dir = output_dir
        os.makedirs(self.output_dir, exist_ok=True)

    def create_country_summary(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Creates a country-level sales summary.
        """

        country_summary = df.groupby("COUNTRY").agg(
            Total_Sales=("SALES", "sum"),
            Total_Orders=("ORDERNUMBER", "nunique"),
            Total_Customers=("CUSTOMERNAME", "nunique"),
            Avg_Order_Value=("SALES", "mean"),
            Total_Quantity=("QUANTITYORDERED", "sum")
        ).round(2).reset_index()

        country_summary = country_summary.sort_values(
            by="Total_Sales",
            ascending=False
        )

        return country_summary

    def create_city_summary(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Creates a city-level sales summary.
        """

        city_summary = df.groupby(["COUNTRY", "CITY"]).agg(
            Total_Sales=("SALES", "sum"),
            Total_Orders=("ORDERNUMBER", "nunique"),
            Total_Customers=("CUSTOMERNAME", "nunique"),
            Avg_Order_Value=("SALES", "mean")
        ).round(2).reset_index()

        city_summary = city_summary.sort_values(
            by="Total_Sales",
            ascending=False
        )

        return city_summary

    def save_market_outputs(self, country_summary: pd.DataFrame, city_summary: pd.DataFrame):
        """
        Saves market analysis results as CSV files.
        """

        country_summary.to_csv(
            os.path.join(self.output_dir, "country_market_summary.csv"),
            index=False
        )

        city_summary.to_csv(
            os.path.join(self.output_dir, "city_market_summary.csv"),
            index=False
        )

    def plot_top_countries_by_sales(self, country_summary: pd.DataFrame, top_n: int = 10):
        """
        Creates a bar chart of top countries by total sales.
        """

        top_countries = country_summary.head(top_n)

        plt.figure(figsize=(10, 5))
        plt.bar(top_countries["COUNTRY"], top_countries["Total_Sales"])
        plt.title("Top Countries by Total Sales")
        plt.xlabel("Country")
        plt.ylabel("Total Sales")
        plt.xticks(rotation=45)
        plt.tight_layout()
        plt.savefig(
            os.path.join(self.output_dir, "top_countries_by_sales.png"),
            dpi=150
        )
        plt.close()

    def plot_avg_order_value_by_country(self, country_summary: pd.DataFrame, top_n: int = 10):
        """
        Creates a bar chart of top countries by average order value.
        """

        top_avg = country_summary.sort_values(
            by="Avg_Order_Value",
            ascending=False
        ).head(top_n)

        plt.figure(figsize=(10, 5))
        plt.bar(top_avg["COUNTRY"], top_avg["Avg_Order_Value"])
        plt.title("Top Countries by Average Order Value")
        plt.xlabel("Country")
        plt.ylabel("Average Order Value")
        plt.xticks(rotation=45)
        plt.tight_layout()
        plt.savefig(
            os.path.join(self.output_dir, "top_countries_by_avg_order_value.png"),
            dpi=150
        )
        plt.close()

    def run(self, df: pd.DataFrame):
        """
        Runs the full market insight pipeline.
        """

        country_summary = self.create_country_summary(df)
        city_summary = self.create_city_summary(df)

        self.save_market_outputs(country_summary, city_summary)
        self.plot_top_countries_by_sales(country_summary)
        self.plot_avg_order_value_by_country(country_summary)

        print("\nCountry Market Summary:")
        print(country_summary.head(10))

        print("\nCity Market Summary:")
        print(city_summary.head(10))

        return country_summary, city_summary
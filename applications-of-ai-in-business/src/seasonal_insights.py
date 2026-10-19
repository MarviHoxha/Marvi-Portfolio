import os
import pandas as pd
import matplotlib.pyplot as plt


class SeasonalInsight:
    """
    Creates business insight outputs for seasonal sales analysis.
    This class helps identify when sales are strongest so MVP can
    plan advertising campaigns at the right time.
    """

    def __init__(self, output_dir: str = "outputs"):
        self.output_dir = output_dir
        os.makedirs(self.output_dir, exist_ok=True)

    def create_monthly_summary(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Creates a monthly sales summary.
        """

        df = df.copy()
        df["YearMonth"] = df["ORDERDATE"].dt.to_period("M").astype(str)

        monthly_summary = df.groupby("YearMonth").agg(
            Total_Sales=("SALES", "sum"),
            Total_Orders=("ORDERNUMBER", "nunique"),
            Total_Quantity=("QUANTITYORDERED", "sum"),
            Avg_Sales=("SALES", "mean")
        ).round(2).reset_index()

        return monthly_summary

    def create_quarterly_summary(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Creates a quarterly sales summary.
        """

        df = df.copy()
        df["YearQuarter"] = df["ORDERDATE"].dt.to_period("Q").astype(str)

        quarterly_summary = df.groupby("YearQuarter").agg(
            Total_Sales=("SALES", "sum"),
            Total_Orders=("ORDERNUMBER", "nunique"),
            Total_Quantity=("QUANTITYORDERED", "sum"),
            Avg_Sales=("SALES", "mean")
        ).round(2).reset_index()

        return quarterly_summary

    def create_month_pattern_summary(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Creates a month-level pattern across all years.
        This helps identify generally strong sales months.
        """

        df = df.copy()
        df["Month_Number"] = df["ORDERDATE"].dt.month
        df["Month_Name"] = df["ORDERDATE"].dt.month_name()

        month_pattern = df.groupby(["Month_Number", "Month_Name"]).agg(
            Total_Sales=("SALES", "sum"),
            Total_Orders=("ORDERNUMBER", "nunique"),
            Total_Quantity=("QUANTITYORDERED", "sum"),
            Avg_Sales=("SALES", "mean")
        ).round(2).reset_index()

        month_pattern = month_pattern.sort_values("Month_Number")

        return month_pattern

    def save_seasonal_outputs(
        self,
        monthly_summary: pd.DataFrame,
        quarterly_summary: pd.DataFrame,
        month_pattern: pd.DataFrame
    ):
        """
        Saves seasonal analysis outputs as CSV files.
        """

        monthly_summary.to_csv(
            os.path.join(self.output_dir, "monthly_sales_summary.csv"),
            index=False
        )

        quarterly_summary.to_csv(
            os.path.join(self.output_dir, "quarterly_sales_summary.csv"),
            index=False
        )

        month_pattern.to_csv(
            os.path.join(self.output_dir, "month_pattern_summary.csv"),
            index=False
        )

    def plot_monthly_sales_trend(self, monthly_summary: pd.DataFrame):
        """
        Creates a line chart showing sales trend over time.
        """

        plt.figure(figsize=(12, 5))
        plt.plot(
            monthly_summary["YearMonth"],
            monthly_summary["Total_Sales"],
            marker="o"
        )
        plt.title("Monthly Sales Trend")
        plt.xlabel("Month")
        plt.ylabel("Total Sales")
        plt.xticks(rotation=45)
        plt.tight_layout()
        plt.savefig(
            os.path.join(self.output_dir, "monthly_sales_trend.png"),
            dpi=150
        )
        plt.close()

    def plot_quarterly_sales(self, quarterly_summary: pd.DataFrame):
        """
        Creates a bar chart showing quarterly sales.
        """

        plt.figure(figsize=(10, 5))
        plt.bar(
            quarterly_summary["YearQuarter"],
            quarterly_summary["Total_Sales"]
        )
        plt.title("Quarterly Sales Summary")
        plt.xlabel("Quarter")
        plt.ylabel("Total Sales")
        plt.xticks(rotation=45)
        plt.tight_layout()
        plt.savefig(
            os.path.join(self.output_dir, "quarterly_sales_summary.png"),
            dpi=150
        )
        plt.close()

    def plot_month_pattern(self, month_pattern: pd.DataFrame):
        """
        Creates a bar chart showing which months have the strongest sales overall.
        """

        plt.figure(figsize=(10, 5))
        plt.bar(
            month_pattern["Month_Name"],
            month_pattern["Total_Sales"]
        )
        plt.title("Sales Pattern by Month")
        plt.xlabel("Month")
        plt.ylabel("Total Sales")
        plt.xticks(rotation=45)
        plt.tight_layout()
        plt.savefig(
            os.path.join(self.output_dir, "sales_pattern_by_month.png"),
            dpi=150
        )
        plt.close()

    def run(self, df: pd.DataFrame):
        """
        Runs the full seasonal insight pipeline.
        """

        monthly_summary = self.create_monthly_summary(df)
        quarterly_summary = self.create_quarterly_summary(df)
        month_pattern = self.create_month_pattern_summary(df)

        self.save_seasonal_outputs(
            monthly_summary,
            quarterly_summary,
            month_pattern
        )

        self.plot_monthly_sales_trend(monthly_summary)
        self.plot_quarterly_sales(quarterly_summary)
        self.plot_month_pattern(month_pattern)

        print("\nMonthly Sales Summary:")
        print(monthly_summary.head(10))

        print("\nQuarterly Sales Summary:")
        print(quarterly_summary)

        print("\nMonth Pattern Summary:")
        print(month_pattern.sort_values(by="Total_Sales", ascending=False).head(5))

        return monthly_summary, quarterly_summary, month_pattern
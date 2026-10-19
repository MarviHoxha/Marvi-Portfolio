import os
import pandas as pd
import matplotlib.pyplot as plt


class CustomerSegmentationInsight:
    """
    Creates business insight outputs from RFM customer segmentation results.
    This class converts technical clustering results into business-friendly
    summaries, visualizations, and recommendations.
    """

    def __init__(self, output_dir: str = "outputs"):
        self.output_dir = output_dir
        os.makedirs(self.output_dir, exist_ok=True)

    def create_segment_summary(self, clustered_df: pd.DataFrame) -> pd.DataFrame:
        """
        Creates a summary table for each customer segment.
        """

        segment_summary = clustered_df.groupby("Segment").agg(
            Customer_Count=("CUSTOMERNAME", "count"),
            Avg_Recency=("Recency", "mean"),
            Avg_Frequency=("Frequency", "mean"),
            Avg_Monetary=("Monetary", "mean"),
            Total_Revenue=("Monetary", "sum")
        ).round(2).reset_index()

        recommendation_map = {
            "VIP": "Target with premium offers, exclusive campaigns, and early access promotions.",
            "Regular": "Use loyalty campaigns, product recommendations, and moderate promotional offers.",
            "At-Risk": "Use reactivation campaigns, personalized discounts, or special return offers."
        }

        segment_summary["Business_Recommendation"] = segment_summary["Segment"].map(recommendation_map)

        return segment_summary

    def save_segment_outputs(self, clustered_df: pd.DataFrame, segment_summary: pd.DataFrame):
        """
        Saves customer segmentation results and summary table as CSV files.
        """

        clustered_df.to_csv(
            os.path.join(self.output_dir, "customer_segments.csv"),
            index=False
        )

        segment_summary.to_csv(
            os.path.join(self.output_dir, "segment_summary.csv"),
            index=False
        )

    def plot_segment_distribution(self, clustered_df: pd.DataFrame):
        """
        Creates a bar chart showing how many customers belong to each segment.
        """

        segment_counts = clustered_df["Segment"].value_counts()

        plt.figure(figsize=(8, 5))
        segment_counts.plot(kind="bar")
        plt.title("Customer Segment Distribution")
        plt.xlabel("Customer Segment")
        plt.ylabel("Number of Customers")
        plt.xticks(rotation=45)
        plt.tight_layout()
        plt.savefig(
            os.path.join(self.output_dir, "customer_segment_distribution.png"),
            dpi=150
        )
        plt.close()

    def plot_average_monetary_value(self, clustered_df: pd.DataFrame):
        """
        Creates a bar chart showing the average monetary value per segment.
        """

        avg_monetary = clustered_df.groupby("Segment")["Monetary"].mean().sort_values(ascending=False)

        plt.figure(figsize=(8, 5))
        avg_monetary.plot(kind="bar")
        plt.title("Average Monetary Value by Customer Segment")
        plt.xlabel("Customer Segment")
        plt.ylabel("Average Monetary Value")
        plt.xticks(rotation=45)
        plt.tight_layout()
        plt.savefig(
            os.path.join(self.output_dir, "average_monetary_by_segment.png"),
            dpi=150
        )
        plt.close()

    def run(self, clustered_df: pd.DataFrame) -> pd.DataFrame:
        """
        Runs the full customer segmentation insight pipeline.
        """

        segment_summary = self.create_segment_summary(clustered_df)

        self.save_segment_outputs(clustered_df, segment_summary)
        self.plot_segment_distribution(clustered_df)
        self.plot_average_monetary_value(clustered_df)

        print("\nCustomer Segment Summary:")
        print(segment_summary)

        return segment_summary
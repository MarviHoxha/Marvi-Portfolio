import os
import pandas as pd
import matplotlib.pyplot as plt


class ProductInsight:
    """
    Creates business insight outputs for product-line performance.
    This class helps identify what MVP should advertise based on
    product-line sales, quantity ordered, and country-level demand.
    """

    def __init__(self, output_dir: str = "outputs"):
        self.output_dir = output_dir
        os.makedirs(self.output_dir, exist_ok=True)

    def create_productline_summary(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Creates a product-line level summary.
        """

        productline_summary = df.groupby("PRODUCTLINE").agg(
            Total_Sales=("SALES", "sum"),
            Total_Quantity=("QUANTITYORDERED", "sum"),
            Total_Orders=("ORDERNUMBER", "nunique"),
            Total_Customers=("CUSTOMERNAME", "nunique"),
            Avg_Sales=("SALES", "mean")
        ).round(2).reset_index()

        productline_summary = productline_summary.sort_values(
            by="Total_Sales",
            ascending=False
        )

        return productline_summary

    def create_productline_country_matrix(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Creates a country-productline sales matrix.
        This helps identify product-market fit.
        """

        matrix = df.pivot_table(
            index="COUNTRY",
            columns="PRODUCTLINE",
            values="SALES",
            aggfunc="sum",
            fill_value=0
        ).round(2)

        return matrix

    def create_best_productline_by_country(self, matrix: pd.DataFrame) -> pd.DataFrame:
        """
        Finds the best performing product line in each country.
        """

        best_productline = matrix.idxmax(axis=1)
        best_sales = matrix.max(axis=1)

        result = pd.DataFrame({
            "COUNTRY": matrix.index,
            "Best_ProductLine": best_productline.values,
            "Sales": best_sales.values
        }).sort_values(by="Sales", ascending=False)

        return result

    def save_product_outputs(
        self,
        productline_summary: pd.DataFrame,
        matrix: pd.DataFrame,
        best_productline_by_country: pd.DataFrame
    ):
        """
        Saves product insight outputs as CSV files.
        """

        productline_summary.to_csv(
            os.path.join(self.output_dir, "productline_summary.csv"),
            index=False
        )

        matrix.to_csv(
            os.path.join(self.output_dir, "productline_country_matrix.csv")
        )

        best_productline_by_country.to_csv(
            os.path.join(self.output_dir, "best_productline_by_country.csv"),
            index=False
        )

    def plot_sales_by_productline(self, productline_summary: pd.DataFrame):
        """
        Creates a bar chart showing total sales by product line.
        """

        plt.figure(figsize=(10, 5))
        plt.bar(
            productline_summary["PRODUCTLINE"],
            productline_summary["Total_Sales"]
        )
        plt.title("Total Sales by Product Line")
        plt.xlabel("Product Line")
        plt.ylabel("Total Sales")
        plt.xticks(rotation=45)
        plt.tight_layout()
        plt.savefig(
            os.path.join(self.output_dir, "sales_by_productline.png"),
            dpi=150
        )
        plt.close()

    def plot_quantity_by_productline(self, productline_summary: pd.DataFrame):
        """
        Creates a bar chart showing total quantity ordered by product line.
        """

        quantity_summary = productline_summary.sort_values(
            by="Total_Quantity",
            ascending=False
        )

        plt.figure(figsize=(10, 5))
        plt.bar(
            quantity_summary["PRODUCTLINE"],
            quantity_summary["Total_Quantity"]
        )
        plt.title("Total Quantity Ordered by Product Line")
        plt.xlabel("Product Line")
        plt.ylabel("Total Quantity Ordered")
        plt.xticks(rotation=45)
        plt.tight_layout()
        plt.savefig(
            os.path.join(self.output_dir, "quantity_by_productline.png"),
            dpi=150
        )
        plt.close()

    def plot_productline_country_heatmap(self, matrix: pd.DataFrame, top_n: int = 10):
        """
        Creates a heatmap-style chart for product-line sales by country.
        Uses the top countries by total sales to keep the chart readable.
        """

        top_countries = matrix.sum(axis=1).sort_values(ascending=False).head(top_n).index
        matrix_top = matrix.loc[top_countries]

        plt.figure(figsize=(12, 6))
        plt.imshow(matrix_top.values, aspect="auto")

        plt.xticks(
            ticks=range(len(matrix_top.columns)),
            labels=matrix_top.columns,
            rotation=45,
            ha="right"
        )
        plt.yticks(
            ticks=range(len(matrix_top.index)),
            labels=matrix_top.index
        )

        plt.colorbar(label="Total Sales")
        plt.title("Product-Line Sales by Country")
        plt.xlabel("Product Line")
        plt.ylabel("Country")
        plt.tight_layout()
        plt.savefig(
            os.path.join(self.output_dir, "productline_country_heatmap.png"),
            dpi=150
        )
        plt.close()

    def run(self, df: pd.DataFrame):
        """
        Runs the full product-line insight pipeline.
        """

        productline_summary = self.create_productline_summary(df)
        matrix = self.create_productline_country_matrix(df)
        best_productline_by_country = self.create_best_productline_by_country(matrix)

        self.save_product_outputs(
            productline_summary,
            matrix,
            best_productline_by_country
        )

        self.plot_sales_by_productline(productline_summary)
        self.plot_quantity_by_productline(productline_summary)
        self.plot_productline_country_heatmap(matrix)

        print("\nProduct-Line Summary:")
        print(productline_summary)

        print("\nBest Product Line by Country:")
        print(best_productline_by_country.head(10))

        return productline_summary, matrix, best_productline_by_country
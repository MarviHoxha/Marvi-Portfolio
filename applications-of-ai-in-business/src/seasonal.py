import matplotlib.pyplot as plt
import pandas as pd


def _add_time_columns(df: pd.DataFrame) -> pd.DataFrame:
    """Creates date-based columns for seasonal analysis"""
    seasonal = df.copy()
    seasonal["ORDERDATE"] = pd.to_datetime(seasonal["ORDERDATE"])
    seasonal["Year"] = seasonal["ORDERDATE"].dt.year
    seasonal["Quarter"] = seasonal["ORDERDATE"].dt.quarter
    seasonal["Month"] = seasonal["ORDERDATE"].dt.month
    seasonal["Month_Name"] = seasonal["ORDERDATE"].dt.month_name()
    return seasonal


def analyse_monthly_sales(df: pd.DataFrame) -> pd.DataFrame:
    """Summarises sales by calendar month """

    seasonal = _add_time_columns(df)

    monthly = seasonal.groupby(["Month", "Month_Name"]).agg(
        Total_Revenue=("SALES", "sum"),
        Avg_Order_Value=("SALES", "mean"),
        Units_Sold=("QUANTITYORDERED", "sum"),
        Num_Orders=("ORDERNUMBER", "nunique")
    ).reset_index()

    monthly["Revenue_Share_%"] = (
        monthly["Total_Revenue"] / monthly["Total_Revenue"].sum() * 100
    ).round(1)

    return monthly.sort_values("Month").drop(columns="Month")


def analyse_quarterly_sales(df: pd.DataFrame) -> pd.DataFrame:
    """Summarises sales by quarter"""
    seasonal = _add_time_columns(df)

    quarterly = seasonal.groupby("Quarter").agg(
        Total_Revenue=("SALES", "sum"),
        Avg_Order_Value=("SALES", "mean"),
        Units_Sold=("QUANTITYORDERED", "sum"),
        Num_Orders=("ORDERNUMBER", "nunique")
    )

    quarterly["Revenue_Share_%"] = (
        quarterly["Total_Revenue"] / quarterly["Total_Revenue"].sum() * 100
    ).round(1)

    return quarterly


def analyse_yearly_sales(df: pd.DataFrame) -> pd.DataFrame:
    """Summarises sales by year"""
    seasonal = _add_time_columns(df)

    return seasonal.groupby("Year").agg(
        Total_Revenue=("SALES", "sum"),
        Avg_Order_Value=("SALES", "mean"),
        Units_Sold=("QUANTITYORDERED", "sum"),
        Num_Orders=("ORDERNUMBER", "nunique")
    ).round(2)


def analyse_product_line_seasonality(df: pd.DataFrame) -> pd.DataFrame:
    """ Shows revenue per product line in each quarter"""
    seasonal = _add_time_columns(df)

    return seasonal.pivot_table(
        index="Quarter",
        columns="PRODUCTLINE",
        values="SALES",
        aggfunc="sum",
        fill_value=0
    ).round(2)


def find_peak_months(df: pd.DataFrame, top_n: int = 3) -> pd.DataFrame:
    """Returns the strongest months by total revenue"""
    monthly = analyse_monthly_sales(df)
    return monthly.sort_values("Total_Revenue", ascending=False).head(top_n)


def plot_monthly_sales(df: pd.DataFrame, save_path: str = None) -> None:
    """Plots total revenue by calendar month"""
    monthly = analyse_monthly_sales(df)

    fig, ax = plt.subplots(figsize=(10, 5))
    ax.bar(monthly["Month_Name"], monthly["Total_Revenue"], color="#2C6E9B")
    ax.set_title("Monthly Sales Revenue")
    ax.set_xlabel("Month")
    ax.set_ylabel("Total Revenue")
    ax.tick_params(axis="x", rotation=45)
    ax.grid(axis="y", alpha=0.3)
    plt.tight_layout()

    if save_path:
        plt.savefig(save_path, dpi=150, bbox_inches="tight")

    plt.show()


def plot_quarterly_sales(df: pd.DataFrame, save_path: str = None) -> None:
    """Plots total revenue by quarter"""
    quarterly = analyse_quarterly_sales(df)

    fig, ax = plt.subplots(figsize=(8, 5))
    ax.bar(
        [f"Q{quarter}" for quarter in quarterly.index],
        quarterly["Total_Revenue"],
        color="#4C956C"
    )
    ax.set_title("Quarterly Sales Revenue")
    ax.set_xlabel("Quarter")
    ax.set_ylabel("Total Revenue")
    ax.grid(axis="y", alpha=0.3)
    plt.tight_layout()

    if save_path:
        plt.savefig(save_path, dpi=150, bbox_inches="tight")

    plt.close()

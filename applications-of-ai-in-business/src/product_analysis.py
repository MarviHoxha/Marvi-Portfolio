# src/product_analysis.py

import pandas as pd


def analyse_product_lines(df: pd.DataFrame) -> pd.DataFrame:
    """
    Computes descriptive performance metrics for each product line.

    IMPORTANT: This must receive the *unscaled* DataFrame (the first
    return value from preprocess_data), so that revenue figures are
    real dollar amounts, not standardised z-scores.

    Args:
        df: Preprocessed (unscaled) sales DataFrame.

    Returns:
        A summary DataFrame with one row per product line, sorted by
        total revenue (highest first).
    """
    # Aggregate key metrics per product line
    summary = df.groupby("PRODUCTLINE").agg(
        Total_Revenue=("SALES", "sum"),          # total money generated
        Avg_Order_Value=("SALES", "mean"),       # average revenue per order line
        Units_Sold=("QUANTITYORDERED", "sum"),   # total units shifted
        Num_Orders=("ORDERNUMBER", "nunique"),   # distinct orders
        Num_Customers=("CUSTOMERNAME", "nunique")  # distinct customers buying it
    )

    # Add each line's share of total revenue (as a percentage)
    summary["Revenue_Share_%"] = (
        summary["Total_Revenue"] / summary["Total_Revenue"].sum() * 100
    ).round(1)

    # Sort from highest to lowest revenue
    summary = summary.sort_values("Total_Revenue", ascending=False)

    return summary


def analyse_pricing(df: pd.DataFrame) -> pd.DataFrame:
    """
    Analyses pricing behaviour per product line by comparing the actual
    selling price against the manufacturer's suggested retail price (MSRP).

    NOTE: We compute the actual unit price as SALES / QUANTITYORDERED
    rather than using PRICEEACH, because PRICEEACH is capped at 100 in
    this dataset and understates higher-priced items.

    Args:
        df: Preprocessed (unscaled) sales DataFrame.

    Returns:
        A DataFrame showing average actual price, average MSRP, and the
        average discount/markup percentage per product line.
    """
    # Work on a copy so we don't modify the original DataFrame
    pricing = df.copy()

    # Real per-unit price actually charged
    pricing["Actual_Unit_Price"] = pricing["SALES"] / pricing["QUANTITYORDERED"]

    # How the selling price compares to MSRP (negative = sold below MSRP)
    pricing["Markup_vs_MSRP_%"] = (
        (pricing["Actual_Unit_Price"] - pricing["MSRP"]) / pricing["MSRP"] * 100
    )

    result = pricing.groupby("PRODUCTLINE").agg(
        Avg_Actual_Price=("Actual_Unit_Price", "mean"),
        Avg_MSRP=("MSRP", "mean"),
        Avg_Markup_vs_MSRP_pct=("Markup_vs_MSRP_%", "mean")
    ).round(2).sort_values("Avg_Markup_vs_MSRP_pct", ascending=False)

    return result
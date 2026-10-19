import os
from pathlib import Path

import matplotlib.dates as mdates
import matplotlib.pyplot as plt
import pandas as pd
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score


def forecast_country_sales(df: pd.DataFrame):

    OUTPUT_DIR = Path("forecast_results")
    HISTORY_START = "2003-01-01"
    HISTORY_END = "2005-05-01"
    FORECAST_MONTHS = 6

    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # Remove stale linear-regression outputs so the folder matches this run.
    for old_plot in OUTPUT_DIR.glob("*_forecast.png"):
        old_plot.unlink()

    for old_file in ["country_forecasts.csv", "forecast_metrics.csv"]:
        old_path = OUTPUT_DIR / old_file
        if old_path.exists():
            old_path.unlink()

    # Keep only relevant columns
    df = df[["COUNTRY", "ORDERDATE", "SALES"]].copy()
    df["Month"] = df["ORDERDATE"].dt.to_period("M").dt.to_timestamp()

    # Monthly aggregation, then expand each country onto the same timeline.
    monthly_sales = (
        df.groupby(["COUNTRY", "Month"])["SALES"]
        .sum()
        .reset_index()
    )

    history_months = pd.date_range(
        start=HISTORY_START,
        end=HISTORY_END,
        freq="MS"
    )
    countries = sorted(df["COUNTRY"].unique())
    full_index = pd.MultiIndex.from_product(
        [countries, history_months],
        names=["COUNTRY", "Month"]
    )

    monthly_sales = (
        monthly_sales
        .set_index(["COUNTRY", "Month"])
        .reindex(full_index, fill_value=0)
        .reset_index()
    )

    all_forecasts = []
    metrics = []

    for country in countries:

        country_data = monthly_sales[
            monthly_sales["COUNTRY"] == country
        ].copy()

        country_data = country_data.sort_values("Month")

        # Create timeline feature
        country_data["TIME_INDEX"] = range(len(country_data))
        country_data["MONTH_ID"] = country_data["Month"].dt.month

        # Multiple regression
        X = country_data[["TIME_INDEX", "MONTH_ID"]]
        y = country_data["SALES"]

        model = LinearRegression()
        model.fit(X, y)

        predictions = model.predict(X)

        mse = mean_squared_error(y, predictions)
        rmse = mse ** 0.5
        mae = mean_absolute_error(y, predictions)
        r2 = r2_score(y, predictions) if y.var() > 0 else 0
        active_sales_months = int((country_data["SALES"] > 0).sum())

        metrics.append({
            "COUNTRY": country,
            "HistoricalMonths": len(country_data),
            "ActiveSalesMonths": active_sales_months,
            "ZeroSalesMonths": int((country_data["SALES"] == 0).sum()),
            "FirstHistoricalMonth": country_data["Month"].min(),
            "LastHistoricalMonth": country_data["Month"].max(),
            "TotalHistoricalSales": country_data["SALES"].sum(),
            "AverageMonthlySales": country_data["SALES"].mean(),
            "MSE": mse,
            "RMSE": rmse,
            "MAE": mae,
            "R2": r2
        })

        # Future months
        last_time = country_data["TIME_INDEX"].max()
        last_month = country_data["Month"].max()
        future_months = pd.date_range(
            start=last_month + pd.DateOffset(months=1),
            periods=FORECAST_MONTHS,
            freq="MS"
        )

        future_df = pd.DataFrame({
            "ForecastMonth": future_months,
            "TIME_INDEX": range(last_time + 1, last_time + FORECAST_MONTHS + 1)
        })
        future_df["MONTH_ID"] = future_df["ForecastMonth"].dt.month
        future_df["YEAR_ID"] = future_df["ForecastMonth"].dt.year

        future_df["ForecastedSales"] = model.predict(
            future_df[["TIME_INDEX", "MONTH_ID"]]
        )

        # Prevent impossible sales values
        future_df["ForecastedSales"] = future_df["ForecastedSales"].clip(lower=0)
        future_df["COUNTRY"] = country

        all_forecasts.append(future_df)

        # Visualization
        fig, ax = plt.subplots(figsize=(12, 6))

        ax.bar(
            country_data["Month"],
            y,
            width=20,
            color="#4C78A8",
            alpha=0.75,
            label="Actual Sales"
        )

        ax.plot(
            future_df["ForecastMonth"],
            future_df["ForecastedSales"],
            marker="o",
            linestyle="--",
            linewidth=2.5,
            color="#F58518",
            label="Forecast"
        )

        ax.axvline(
            last_month,
            color="#666666",
            linestyle=":",
            linewidth=1.5
        )
        ax.axvspan(
            future_months.min(),
            future_months.max(),
            color="#F58518",
            alpha=0.08
        )

        ax.set_title(f"{country} Sales Forecast", fontsize=16, pad=14)
        ax.text(
            0.01,
            0.97,
            f"History: {len(country_data)} months | Active sales months: {active_sales_months} | R2: {r2:.3f}",
            transform=ax.transAxes,
            va="top",
            ha="left",
            fontsize=10,
            bbox={
                "boxstyle": "round,pad=0.35",
                "facecolor": "white",
                "edgecolor": "#DDDDDD",
                "alpha": 0.9
            }
        )

        ax.set_xlabel("Month")
        ax.set_ylabel("Sales")
        ax.set_xlim(
            history_months.min() - pd.DateOffset(days=15),
            future_months.max() + pd.DateOffset(days=15)
        )
        ax.xaxis.set_major_locator(mdates.MonthLocator(interval=3))
        ax.xaxis.set_major_formatter(mdates.DateFormatter("%Y-%m"))
        ax.grid(axis="y", alpha=0.25)
        ax.spines["top"].set_visible(False)
        ax.spines["right"].set_visible(False)

        ax.legend(loc="upper right")
        fig.autofmt_xdate(rotation=45)
        fig.tight_layout()

        fig.savefig(OUTPUT_DIR / f"{country}_forecast.png")
        plt.close(fig)

    # Save forecasts
    if all_forecasts:

        forecast_df = pd.concat(
            all_forecasts,
            ignore_index=True
        )

        forecast_df.to_csv(
            OUTPUT_DIR / "country_forecasts.csv",
            index=False
        )

        metrics_df = pd.DataFrame(metrics)

        metrics_df.to_csv(
            OUTPUT_DIR / "forecast_metrics.csv",
            index=False
        )

        print("Forecasting completed.")
        print("Results saved in:", OUTPUT_DIR)

    else:
        print("No country had enough data.")

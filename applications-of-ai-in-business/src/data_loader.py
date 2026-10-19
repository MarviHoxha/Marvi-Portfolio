# src/data_loader.py

import pandas as pd
from sklearn.preprocessing import StandardScaler


def load_data(filepath: str) -> pd.DataFrame:
    """
    Loads the sales dataset from a CSV file.
    Args:
        filepath: Path to the CSV file.
    Returns:
        A cleaned pandas DataFrame.
    """
    df = pd.read_csv(filepath, encoding="cp1252")
    return df


def preprocess_data(df: pd.DataFrame) -> tuple[pd.DataFrame, pd.DataFrame]:

    # Keep only orders that actually generated revenue
    df = df[df["STATUS"] == "Shipped"]

    # 1. DROP USELESS COLUMNS (Feature Selection)
    # These columns don't help with analysis (contact details, addresses)
    df = df.drop(columns=[
        "PHONE", "ADDRESSLINE1", "ADDRESSLINE2", "POSTALCODE",
        "CONTACTLASTNAME", "CONTACTFIRSTNAME",
        "STATUS", "STATE", "TERRITORY",
        "MONTH_ID", "QTR_ID", "YEAR_ID"             # these are redundant with ORDERDATE, as they can get extracted 
    ])



    # 2. FIX DATA TYPES
    # Convert date text into a real date Python understands
    df["ORDERDATE"] = pd.to_datetime(df["ORDERDATE"])

    # 3. HANDLE MISSING VALUES
    # Check how many missing values exist per column
    print("Missing values:\n", df.isnull().sum())
    df = df.dropna() # Drop any rows that have missing values

    # 4. REMOVE DUPLICATES
    df = df.drop_duplicates()

    # 5. ENCODING
    # Convert DEALSIZE text into numbers (Small=0, Medium=1, Large=2)
    # ML models need numbers, not text
    deal_size_order = ["Small", "Medium", "Large"]
    df["DEALSIZE"] = pd.Categorical(df["DEALSIZE"],
                                    categories=deal_size_order,
                                    ordered=True).codes


    # 6. SCALING
    # Make numeric columns comparable by putting them on the same scale
    # e.g. SALES ranges 0-10000 but QUANTITYORDERED ranges 0-100 — unfair for ML
    # 7. SCALING (only for ML — keep originals for reporting)
    scaler = StandardScaler()
    numeric_cols = ["QUANTITYORDERED", "PRICEEACH", "SALES", "MSRP"]
    df_scaled = df.copy()
    df_scaled[numeric_cols] = scaler.fit_transform(df[numeric_cols])

    return df, df_scaled   # returning TWO things as a tuple
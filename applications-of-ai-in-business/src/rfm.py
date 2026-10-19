# src/rfm.py

import pandas as pd
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
import matplotlib.pyplot as plt


def build_rfm_table(df: pd.DataFrame) -> pd.DataFrame:
    """
    Builds an RFM (Recency, Frequency, Monetary) table per customer.
    Args:
        df: Preprocessed sales DataFrame (must contain ORDERDATE,
            CUSTOMERNAME, ORDERNUMBER, SALES).
    Returns:
        DataFrame with one row per customer and columns: Recency,
        Frequency, Monetary.
    """
    # Define a reference date: "Today" is one day after the last order in the dataset
    # (so the most recent customer has Recency = 1, not 0)
    reference_date = df["ORDERDATE"].max() + pd.Timedelta(days=1)

    rfm = df.groupby("CUSTOMERNAME").agg( # group customers of same name together
        Recency=("ORDERDATE", lambda x: (reference_date - x.max()).days), # for each customer, take "today" minus their latest order, get the number of days
        Frequency=("ORDERNUMBER", "nunique"),   # count how many unique orders they placed (nunique removes duplicate order numbers from multi-line orders)
        Monetary=("SALES", "sum") # sum up all their sales
    ).reset_index()

    return rfm


def find_optimal_k(rfm_scaled, k_range=range(2, 11)):
    """
    Uses the Elbow Method to help identify the optimal number of clusters.
    Args:
        rfm_scaled: Scaled RFM values (NumPy array or DataFrame).
        k_range: Range of k values to test (default: 2 to 10).
    Returns:
        Dictionary mapping k -> inertia value.
    """
    inertias = {} # Inertia = total squared distance from each point to its cluster's centre. Lower = tighter clusters.
    # We loop through k=2 to k=10 and store each inertia
    for k in k_range:                   # 42:reproducible results (same output every run)
        kmeans = KMeans(n_clusters=k, random_state=42, n_init=10) # runs k-Means 10 times with different starting points and keeps the best (k-Means is sensitive to initialization)
        kmeans.fit(rfm_scaled)
        inertias[k] = kmeans.inertia_ # the metric we want to plot
    return inertias


def plot_elbow(inertias: dict, save_path: str = None):
    """
    Plots the Elbow Method graph to visualize the optimal k.
    We look for the "elbow", the point where extra clusters stop giving meaningful improvement.
    """
    fig, ax = plt.subplots(figsize=(8, 5))
    ax.plot(list(inertias.keys()), list(inertias.values()),
            marker='o', linewidth=2, color='#2C6E9B')
    ax.set_xlabel("Number of Clusters (k)")
    ax.set_ylabel("Inertia")
    ax.set_title("Elbow Method for Optimal k")
    ax.grid(alpha=0.3)
    if save_path:
        plt.savefig(save_path, dpi=150, bbox_inches='tight')
    plt.close(fig)

def run_rfm_clustering(df: pd.DataFrame, n_clusters: int = 3) -> pd.DataFrame:
    """
    Runs the full RFM clustering pipeline:
    1. Build RFM table
    2. Scale features
    3. Run k-Means clustering
    4. Assign business-friendly labels to clusters
    Args:
        df: Preprocessed sales DataFrame.
        n_clusters: Number of clusters (determined via Elbow Method).
    Returns:
        RFM DataFrame with added 'Cluster' and 'Segment' columns.
    """
    # 1. Build the RFM table
    rfm = build_rfm_table(df)

    # 2. Scale the features so R, F, M contribute equally
    scaler = StandardScaler()
    rfm_scaled = scaler.fit_transform(rfm[["Recency", "Frequency", "Monetary"]])

    # 3. Run k-Means clustering
    kmeans = KMeans(n_clusters=n_clusters, random_state=42, n_init=10)
    rfm["Cluster"] = kmeans.fit_predict(rfm_scaled)

    # 4. Assign business-friendly labels
    rfm["Segment"] = rfm["Cluster"].map(label_clusters(rfm))

    return rfm


def label_clusters(rfm: pd.DataFrame) -> dict:
    """
    Ranks clusters by overall customer value (R, F, M) and assigns
    one distinct business label per cluster, from best to worst.
    """
    # Calculate the mean R, F, M for each cluster
    cluster_means = rfm.groupby("Cluster")[["Recency", "Frequency", "Monetary"]].mean()

    # Build a value score:
    # - higher Monetary = better
    # - higher Frequency = better
    # - LOWER Recency = better (bought recently)
    cluster_means["Score"] = (
        cluster_means["Monetary"].rank()
        + cluster_means["Frequency"].rank()
        + cluster_means["Recency"].rank(ascending=False)
    )

    # Order clusters from most valuable to least valuable
    ranked = cluster_means.sort_values("Score", ascending=False).index.tolist()

    # Assign distinct labels (best → worst)
    segment_names = ["VIP", "Regular", "At-Risk"]
    labels = {cluster_id: segment_names[i] for i, cluster_id in enumerate(ranked)}

    return labels
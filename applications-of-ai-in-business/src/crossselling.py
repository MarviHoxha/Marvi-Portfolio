import matplotlib.pyplot as plt
import pandas as pd
from mlxtend.frequent_patterns import apriori, association_rules


RULE_COLUMNS = ["antecedents", "consequents", "support", "confidence", "lift"]


def prepare_basket(df: pd.DataFrame, item_column: str = "PRODUCTLINE") -> pd.DataFrame:

    """Converts order-line data into a basket format for Apriori.
    Each row represents one order, each column represents one product/product line."""

    basket = (
        df.groupby(["ORDERNUMBER", item_column])
        .size()
        .unstack(fill_value=0)
    )

    basket = basket > 0
    return basket

def run_apriori(
        df: pd.DataFrame,
        item_column: str = "PRODUCTLINE",
        min_support: float = 0.03,
        min_confidence: float = 0.3
) -> pd.DataFrame:

    """ Runs Apriori association rule learning for cross-selling analysis."""

    basket = prepare_basket(df, item_column)

    frequent_itemsets = apriori(
        basket,
        min_support=min_support,
        use_colnames=True
    )

    if frequent_itemsets.empty:
        print("No frequent itemsets found. Try lowering min_support.")
        return pd.DataFrame(columns=RULE_COLUMNS)

    rules = association_rules(
        frequent_itemsets,
        metric="confidence",
        min_threshold=min_confidence
    )

    if rules.empty:
        print("No association rules found. Try lowering min_confidence.")
        return pd.DataFrame(columns=RULE_COLUMNS)

    rules = rules.sort_values(
        by=["lift", "confidence", "support"],
        ascending=False
    )

    return rules[RULE_COLUMNS]


def print_top_rules(rules: pd.DataFrame, top_n: int = 10) -> None:

    """Prints the strongest cross-selling rules. """

    if rules.empty:
        print("No rules to display.")
        return

    for _, row in rules.head(top_n).iterrows():
        antecedents = ", ".join(list(row["antecedents"]))
        consequents = ", ".join(list(row["consequents"]))

        print(
            f"If customer buys [{antecedents}], "
            f"they may also buy [{consequents}] "
            f"(support={row['support']:.3f}, "
            f"confidence={row['confidence']:.3f}, "
            f"lift={row['lift']:.3f})"
        )


def keep_strongest_single_item_rules(rules: pd.DataFrame) -> pd.DataFrame:
    """Keeps one strongest rule for each product-line pair."""

    if rules.empty:
        return pd.DataFrame(columns=RULE_COLUMNS)

    simple_rules = rules[
        (rules["antecedents"].apply(len) == 1) &
        (rules["consequents"].apply(len) == 1)
    ].copy()

    if simple_rules.empty:
        return pd.DataFrame(columns=RULE_COLUMNS)

    simple_rules["pair"] = simple_rules.apply(
        lambda row: tuple(sorted([
            list(row["antecedents"])[0],
            list(row["consequents"])[0]
        ])),
        axis=1
    )

    simple_rules = (
        simple_rules
        .sort_values("lift", ascending=False)
        .drop_duplicates(subset="pair")
        .drop(columns="pair")
    )

    return simple_rules[simple_rules["lift"] > 1].copy()


def format_rules_for_export(rules: pd.DataFrame) -> pd.DataFrame:
    """Converts Apriori frozensets into readable CSV output."""

    if rules.empty:
        return pd.DataFrame(columns=[
            "Antecedent",
            "Consequent",
            "Support",
            "Confidence",
            "Lift"
        ])

    formatted = rules.copy()
    formatted["Antecedent"] = formatted["antecedents"].apply(
        lambda items: ", ".join(sorted(items))
    )
    formatted["Consequent"] = formatted["consequents"].apply(
        lambda items: ", ".join(sorted(items))
    )

    return formatted[[
        "Antecedent",
        "Consequent",
        "support",
        "confidence",
        "lift"
    ]].rename(columns={
        "support": "Support",
        "confidence": "Confidence",
        "lift": "Lift"
    })


def plot_top_rules_by_lift(rules: pd.DataFrame, top_n: int = 10, save_path: str = None) -> None:

    """Visualizes the strongest association rules based on lift."""

    if rules.empty:
        print("No rules to plot.")
        return

    top_rules = rules.sort_values("lift", ascending=False).head(top_n).copy()

    top_rules["rule"] = top_rules.apply(
        lambda row: f"{', '.join(list(row['antecedents']))} -> {', '.join(list(row['consequents']))}",
        axis=1
    )

    plt.figure(figsize=(12, 6))
    plt.barh(top_rules["rule"], top_rules["lift"])
    plt.xlabel("Lift")
    plt.ylabel("Association Rule")
    plt.title(f"Top {top_n} Cross-Selling Rules by Lift")
    plt.gca().invert_yaxis()
    plt.tight_layout()

    if save_path:
        plt.savefig(save_path, dpi=150, bbox_inches="tight")
        plt.close()
    else:
        plt.show()

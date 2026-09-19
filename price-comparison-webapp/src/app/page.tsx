"use client";

import { useState, useMemo, type FormEvent } from "react";
import { ProductCard } from "@/components/ProductCard";
import type { SearchResponse } from "@/lib/scrapers/api";

type ViewMode = "list" | "grid";

export default function Home() {
  const [query, setQuery] = useState("");
  const [view, setView] = useState<ViewMode>("grid");
  const [data, setData] = useState<SearchResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const bestPrice = useMemo(() => {
    const prices = (data?.results ?? []).map((r) => r.price).filter((p): p is number => p !== null);
    return prices.length ? Math.min(...prices) : null;
  }, [data]);

  async function handleSearch(e: FormEvent) {
    e.preventDefault();
    const trimmed = query.trim();
    if (!trimmed || loading) return;

    setLoading(true);
    setError(null);
    setData(null);

    try {
      const res = await fetch(`/api/search?q=${encodeURIComponent(trimmed)}`);
      if (!res.ok) {
        const body = await res.json().catch(() => null);
        throw new Error(body?.error ?? `Search failed (${res.status})`);
      }
      const json: SearchResponse = await res.json();
      setData(json);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
    } finally {
      setLoading(false);
    }
  }

  const failedSites = (data?.sites ?? []).filter((s) => s.error);

  return (
    <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-10 sm:py-16">
      <div className="text-center mb-8">
        <h1 className="text-3xl sm:text-4xl font-bold tracking-tight">Best Price Finder</h1>
        <p className="mt-2 text-black/60 dark:text-white/60">
          Search a product, compare live prices across retailers, and go straight to the cheapest listing.
        </p>
      </div>

      <form onSubmit={handleSearch} className="flex gap-2 max-w-2xl mx-auto">
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search for a product, e.g. iPhone 15"
          className="flex-1 rounded-lg border border-black/15 dark:border-white/15 bg-transparent px-4 py-3 outline-none focus:border-black/40 dark:focus:border-white/40"
        />
        <button
          type="submit"
          disabled={loading || !query.trim()}
          className="rounded-lg bg-foreground text-background px-5 py-3 font-medium disabled:opacity-40"
        >
          {loading ? "Searching…" : "Search"}
        </button>
      </form>

      {error && (
        <p className="mt-6 text-center text-sm text-red-600 dark:text-red-400">{error}</p>
      )}

      {data && (
        <div className="mt-10">
          <div className="flex items-center justify-between mb-4">
            <p className="text-sm text-black/60 dark:text-white/60">
              {data.results.length} result{data.results.length === 1 ? "" : "s"} for &ldquo;{data.query}&rdquo;
              {data.mock && " (mock data)"}
            </p>
            <div className="flex rounded-lg border border-black/15 dark:border-white/15 overflow-hidden">
              <button
                onClick={() => setView("grid")}
                className={`px-3 py-1.5 text-sm ${view === "grid" ? "bg-foreground text-background" : ""}`}
              >
                Grid
              </button>
              <button
                onClick={() => setView("list")}
                className={`px-3 py-1.5 text-sm ${view === "list" ? "bg-foreground text-background" : ""}`}
              >
                List
              </button>
            </div>
          </div>

          {failedSites.length > 0 && (
            <p className="mb-4 text-xs text-black/50 dark:text-white/50">
              Couldn&apos;t fetch results from: {failedSites.map((s) => s.siteName).join(", ")}
            </p>
          )}

          {data.results.length === 0 ? (
            <p className="text-center text-black/60 dark:text-white/60 py-12">No results found.</p>
          ) : view === "grid" ? (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
              {data.results.map((product) => (
                <ProductCard
                  key={product.id}
                  product={product}
                  view="grid"
                  isBestPrice={product.price !== null && product.price === bestPrice}
                />
              ))}
            </div>
          ) : (
            <div className="flex flex-col gap-3">
              {data.results.map((product) => (
                <ProductCard
                  key={product.id}
                  product={product}
                  view="list"
                  isBestPrice={product.price !== null && product.price === bestPrice}
                />
              ))}
            </div>
          )}
        </div>
      )}
    </main>
  );
}

import { NextRequest, NextResponse } from "next/server";
import { siteAdapters } from "@/lib/scrapers/sites";
import { mockSearch } from "@/lib/scrapers/mock";
import type { ProductResult, SiteSearchOutcome } from "@/lib/scrapers/types";

export const dynamic = "force-dynamic";

const PER_SITE_TIMEOUT_MS = 20000;

function withTimeout<T>(promise: Promise<T>, ms: number): Promise<T> {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(`timed out after ${ms}ms`)), ms);
    promise
      .then((value) => {
        clearTimeout(timer);
        resolve(value);
      })
      .catch((err) => {
        clearTimeout(timer);
        reject(err);
      });
  });
}

export async function GET(request: NextRequest) {
  const query = request.nextUrl.searchParams.get("q")?.trim();
  const useMock = request.nextUrl.searchParams.get("mock") === "1" || process.env.MOCK_SEARCH === "1";

  if (!query) {
    return NextResponse.json({ error: "Missing required query param 'q'" }, { status: 400 });
  }

  if (useMock) {
    const results = mockSearch(query);
    const bySite: SiteSearchOutcome[] = groupBySite(results);
    return NextResponse.json({ query, mock: true, results: sortByPrice(results), sites: bySite });
  }

  const outcomes: SiteSearchOutcome[] = await Promise.all(
    siteAdapters.map(async (adapter): Promise<SiteSearchOutcome> => {
      const start = Date.now();
      try {
        const results = await withTimeout(adapter.search(query), PER_SITE_TIMEOUT_MS);
        return { siteId: adapter.id, siteName: adapter.name, results, error: null, tookMs: Date.now() - start };
      } catch (err) {
        return {
          siteId: adapter.id,
          siteName: adapter.name,
          results: [],
          error: err instanceof Error ? err.message : "Unknown error",
          tookMs: Date.now() - start,
        };
      }
    })
  );

  const allResults = outcomes.flatMap((outcome) => outcome.results);

  return NextResponse.json({
    query,
    mock: false,
    results: sortByPrice(allResults),
    sites: outcomes,
  });
}

function sortByPrice(results: ProductResult[]): ProductResult[] {
  return [...results].sort((a, b) => {
    if (a.price === null) return 1;
    if (b.price === null) return -1;
    return a.price - b.price;
  });
}

function groupBySite(results: ProductResult[]): SiteSearchOutcome[] {
  const bySite = new Map<string, ProductResult[]>();
  for (const result of results) {
    const list = bySite.get(result.siteId) ?? [];
    list.push(result);
    bySite.set(result.siteId, list);
  }
  return Array.from(bySite.entries()).map(([siteId, siteResults]) => ({
    siteId,
    siteName: siteResults[0]?.siteName ?? siteId,
    results: siteResults,
    error: null,
    tookMs: 0,
  }));
}

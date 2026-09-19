export interface ProductResult {
  /** Stable id for React keys: `${siteId}-${index}` */
  id: string;
  title: string;
  price: number | null;
  currency: string;
  imageUrl: string | null;
  /** Direct link to the product page on the retailer's site. */
  productUrl: string;
  siteId: string;
  siteName: string;
}

export interface SiteAdapter {
  id: string;
  name: string;
  baseUrl: string;
  /** Fetch and parse search results for a query. Must not throw for "no results" (return []); may throw for real failures, which the caller isolates per-site. */
  search(query: string): Promise<ProductResult[]>;
}

export interface SiteSearchOutcome {
  siteId: string;
  siteName: string;
  results: ProductResult[];
  error: string | null;
  tookMs: number;
}

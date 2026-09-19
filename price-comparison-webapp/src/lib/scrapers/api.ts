import type { ProductResult, SiteSearchOutcome } from "./types";

export interface SearchResponse {
  query: string;
  mock: boolean;
  results: ProductResult[];
  sites: SiteSearchOutcome[];
}

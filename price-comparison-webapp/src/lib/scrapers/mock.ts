import type { ProductResult } from "./types";

const MOCK_SITES = [
  { id: "daraz", name: "Daraz.pk" },
  { id: "priceoye", name: "PriceOye.pk" },
  { id: "telemart", name: "Telemart.pk" },
  { id: "ishopping", name: "iShopping.pk" },
  { id: "shophive", name: "Shophive.com" },
];

/**
 * Deterministic fake results, used when MOCK_SEARCH=1 (e.g. no live network
 * access, or you just want to exercise the UI without hitting real sites).
 */
export function mockSearch(query: string): ProductResult[] {
  return MOCK_SITES.map((site, i) => {
    const base = 5000 + query.length * 137 + i * 431;
    return {
      id: `${site.id}-mock-0`,
      title: `${query} (from ${site.name})`,
      price: base,
      currency: "PKR",
      imageUrl: `https://picsum.photos/seed/${encodeURIComponent(site.id + query)}/300/300`,
      productUrl: `https://example.com/${site.id}/${encodeURIComponent(query)}`,
      siteId: site.id,
      siteName: site.name,
    };
  });
}

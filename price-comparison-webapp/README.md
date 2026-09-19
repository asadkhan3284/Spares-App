# Best Price Finder

A webapp that searches a product across multiple Pakistani e-commerce sites, shows
the results in list or grid view sorted by price, and links straight through to the
matching product page on the retailer's site.

Built with Next.js (App Router, TypeScript) and Playwright for live scraping — no
paid price-comparison API, no database, no accounts. Every search is live: there's
no background indexing or caching of prices.

## Running it

```bash
npm install
npm run dev
```

Open http://localhost:3000. Playwright needs a Chromium browser:

```bash
npx playwright install chromium
```

If you're running somewhere that already has a Chromium binary you want to reuse
(e.g. a sandboxed/offline environment), point at it instead of downloading one:

```bash
# .env.local
CHROMIUM_EXECUTABLE_PATH=/path/to/chrome
```

### Demo mode (no live scraping)

To exercise the full UI without hitting real sites (useful behind a restrictive
firewall, or just to see the UX), set `MOCK_SEARCH=1`:

```bash
MOCK_SEARCH=1 npm run dev
```

or pass `?mock=1` on an individual request to `/api/search`.

## How it works

```
src/lib/scrapers/
  types.ts           ProductResult + SiteAdapter interfaces
  browser.ts          shared Playwright browser/context lifecycle
  parsePrice.ts        "Rs. 45,999" -> 45999 helper
  mock.ts              fake results for MOCK_SEARCH=1
  sites/
    index.ts           the list of active adapters — add a site here
    daraz.ts            Daraz.pk (custom, SPA — waits for the product grid)
    priceoye.ts          PriceOye.pk (custom)
    magentoAdapter.ts    factory for Magento-storefront sites (shared selectors)

src/app/api/search/route.ts   fans the query out to every adapter in parallel,
                               isolates per-site errors/timeouts so one broken
                               site never breaks the others, merges + sorts by
                               price ascending

src/app/page.tsx               search box, grid/list toggle, product cards that
                               open the retailer's product page directly
                               (target="_blank")
```

A `SiteAdapter` is just:

```ts
interface SiteAdapter {
  id: string;
  name: string;
  baseUrl: string;
  search(query: string): Promise<ProductResult[]>;
}
```

`/api/search` runs every adapter in `sites/index.ts` concurrently with a 20s
per-site timeout. A site that errors or times out just contributes zero results
(reported in the response's `sites[].error`) — it never fails the whole search.

## Adding more sites

This ships with five starting adapters (Daraz, PriceOye, Telemart, iShopping,
Shophive) as a base to grow from, not a finished catalog. To add another site:

1. **Magento-based storefront** (product-item / product-item-link / price
   classes — common among smaller PK retailers): add one line to
   `sites/index.ts`:
   ```ts
   createMagentoAdapter({ id: "example", name: "Example.pk", baseUrl: "https://example.pk" }),
   ```
2. **Anything else**: copy `sites/priceoye.ts` as a template, update the search
   URL and CSS selectors for that site's result cards, and add it to the
   `siteAdapters` array in `sites/index.ts`. Nothing else needs to change —
   the API route and UI pick up new adapters automatically.

Before trusting a new adapter, actually load its search URL in a browser and
check DevTools for the real selectors — sites change their markup without
notice, and scraping is inherently best-effort (there's no contract with these
sites the way there is with a paid API).

**Known limitation**: the Daraz, PriceOye, Telemart, iShopping, and Shophive
adapters shipped here were written from general knowledge of these sites'
typical DOM structure and **have not been verified against live pages** — the
environment they were built in has no network access to these hosts. Re-check
selectors against real search-result pages before relying on this for
anything beyond local testing.

## Notes on scraping

- Uses a real headless Chromium (via Playwright), not a lightweight HTTP
  fetch, since several target sites are JS-rendered.
- No caching/indexing: every search launches fresh page loads against every
  configured site. This is simplest for an MVP, but means each search takes
  several seconds and is more likely to hit a site's rate limiting than a
  cached/scheduled approach would be.
- No affiliate links — product links go straight to the retailer's page.
- Scraping terms of service vary by site and can change; this is a
  proof-of-concept, not a guarantee of compliance with any given site's ToS.

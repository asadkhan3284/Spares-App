import type { ProductResult, SiteAdapter } from "../types";
import { withPage } from "../browser";
import { parsePrice, toAbsoluteUrl } from "../parsePrice";

/**
 * Daraz.pk is a React SPA that renders results client-side, so we wait for the
 * product grid to appear rather than parsing static HTML.
 *
 * NOTE: unverified against a live page in this environment (outbound access to
 * daraz.pk is blocked here by network policy) — selectors are best-effort based
 * on Daraz's known DOM structure and should be re-checked against a live page
 * before relying on this in production.
 */
export const darazAdapter: SiteAdapter = {
  id: "daraz",
  name: "Daraz.pk",
  baseUrl: "https://www.daraz.pk",

  async search(query: string): Promise<ProductResult[]> {
    const url = `https://www.daraz.pk/catalog/?q=${encodeURIComponent(query)}`;

    return withPage(async (page) => {
      await page.goto(url, { waitUntil: "domcontentloaded" });

      const cardSelector = '[data-qa-locator="product-item"]';
      await page.waitForSelector(cardSelector, { timeout: 15000 }).catch(() => null);

      const cards = await page.$$(cardSelector);
      const results: ProductResult[] = [];

      for (const card of cards.slice(0, 20)) {
        const title = (await card.$eval("a[title]", (el) => el.getAttribute("title")).catch(() => null))
          ?? (await card.$eval(".title--wFj93, .RfADt a", (el) => el.textContent).catch(() => null));
        const href = await card.$eval("a", (el) => el.getAttribute("href")).catch(() => null);
        const priceText = await card
          .$eval(".price--NVB62, .aBrP0", (el) => el.textContent)
          .catch(() => null);
        const imageUrl = await card
          .$eval("img", (el) => el.getAttribute("src") || el.getAttribute("data-src"))
          .catch(() => null);

        if (!title || !href) continue;

        results.push({
          id: `daraz-${results.length}`,
          title: title.trim(),
          price: parsePrice(priceText),
          currency: "PKR",
          imageUrl: imageUrl ? toAbsoluteUrl(imageUrl, "https://www.daraz.pk") : null,
          productUrl: toAbsoluteUrl(href, "https://www.daraz.pk"),
          siteId: "daraz",
          siteName: "Daraz.pk",
        });
      }

      return results;
    });
  },
};

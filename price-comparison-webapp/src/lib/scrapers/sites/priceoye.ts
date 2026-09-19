import type { ProductResult, SiteAdapter } from "../types";
import { withPage } from "../browser";
import { parsePrice, toAbsoluteUrl } from "../parsePrice";

/**
 * NOTE: unverified against a live page in this environment (outbound access to
 * priceoye.pk is blocked here by network policy) — selectors are best-effort and
 * should be re-checked against a live page before relying on this in production.
 */
export const priceoyeAdapter: SiteAdapter = {
  id: "priceoye",
  name: "PriceOye.pk",
  baseUrl: "https://priceoye.pk",

  async search(query: string): Promise<ProductResult[]> {
    const url = `https://priceoye.pk/search?q=${encodeURIComponent(query)}`;

    return withPage(async (page) => {
      await page.goto(url, { waitUntil: "domcontentloaded" });

      const cardSelector = "a.styles_productCard__link, a.product-card, .productBox";
      await page.waitForSelector(cardSelector, { timeout: 15000 }).catch(() => null);

      const cards = await page.$$(cardSelector);
      const results: ProductResult[] = [];

      for (const card of cards.slice(0, 20)) {
        const title = await card
          .$eval("h3, .productName, .name", (el) => el.textContent)
          .catch(async () => card.getAttribute("title"));
        const href = await card.getAttribute("href");
        const priceText = await card
          .$eval(".price, .currentPrice", (el) => el.textContent)
          .catch(() => null);
        const imageUrl = await card
          .$eval("img", (el) => el.getAttribute("src") || el.getAttribute("data-src"))
          .catch(() => null);

        if (!title || !href) continue;

        results.push({
          id: `priceoye-${results.length}`,
          title: title.trim(),
          price: parsePrice(priceText),
          currency: "PKR",
          imageUrl: imageUrl ? toAbsoluteUrl(imageUrl, "https://priceoye.pk") : null,
          productUrl: toAbsoluteUrl(href, "https://priceoye.pk"),
          siteId: "priceoye",
          siteName: "PriceOye.pk",
        });
      }

      return results;
    });
  },
};

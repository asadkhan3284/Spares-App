import type { ProductResult, SiteAdapter } from "../types";
import { withPage } from "../browser";
import { parsePrice, toAbsoluteUrl } from "../parsePrice";

interface MagentoSiteConfig {
  id: string;
  name: string;
  baseUrl: string;
}

/**
 * Several Pakistani retailers (Telemart, iShopping, Shophive, ...) run on Magento's
 * default catalog-search theme, which shares near-identical markup
 * (`li.product-item`, `.product-item-link`, `.price`). One adapter factory covers
 * all of them instead of duplicating near-identical code per site.
 *
 * NOTE: unverified against live pages in this environment (outbound access to these
 * hosts is blocked here by network policy) — re-check selectors against a live page
 * before relying on this in production, and split a site into its own adapter file
 * if it turns out to have deviated from the stock Magento markup.
 */
export function createMagentoAdapter(config: MagentoSiteConfig): SiteAdapter {
  return {
    id: config.id,
    name: config.name,
    baseUrl: config.baseUrl,

    async search(query: string): Promise<ProductResult[]> {
      const url = `${config.baseUrl}/catalogsearch/result/?q=${encodeURIComponent(query)}`;

      return withPage(async (page) => {
        await page.goto(url, { waitUntil: "domcontentloaded" });

        const cardSelector = "li.product-item";
        await page.waitForSelector(cardSelector, { timeout: 15000 }).catch(() => null);

        const cards = await page.$$(cardSelector);
        const results: ProductResult[] = [];

        for (const card of cards.slice(0, 20)) {
          const title = await card
            .$eval(".product-item-link", (el) => el.textContent)
            .catch(() => null);
          const href = await card
            .$eval(".product-item-link", (el) => el.getAttribute("href"))
            .catch(() => null);
          const priceText = await card
            .$eval(".price", (el) => el.textContent)
            .catch(() => null);
          const imageUrl = await card
            .$eval("img.product-image-photo", (el) => el.getAttribute("src"))
            .catch(() => null);

          if (!title || !href) continue;

          results.push({
            id: `${config.id}-${results.length}`,
            title: title.trim(),
            price: parsePrice(priceText),
            currency: "PKR",
            imageUrl: imageUrl ? toAbsoluteUrl(imageUrl, config.baseUrl) : null,
            productUrl: toAbsoluteUrl(href, config.baseUrl),
            siteId: config.id,
            siteName: config.name,
          });
        }

        return results;
      });
    },
  };
}

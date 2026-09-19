import type { SiteAdapter } from "../types";
import { darazAdapter } from "./daraz";
import { priceoyeAdapter } from "./priceoye";
import { createMagentoAdapter } from "./magentoAdapter";

/**
 * All active site adapters. Add a new site by:
 *  1. Writing a `SiteAdapter` (see daraz.ts / priceoye.ts for custom sites, or
 *     magentoAdapter.ts for Magento-based storefronts you can just configure), and
 *  2. Pushing it into this array.
 * No other code needs to change — /api/search and the registry both pick it up
 * automatically.
 */
export const siteAdapters: SiteAdapter[] = [
  darazAdapter,
  priceoyeAdapter,
  createMagentoAdapter({ id: "telemart", name: "Telemart.pk", baseUrl: "https://telemart.pk" }),
  createMagentoAdapter({ id: "ishopping", name: "iShopping.pk", baseUrl: "https://ishopping.pk" }),
  createMagentoAdapter({ id: "shophive", name: "Shophive.com", baseUrl: "https://shophive.com" }),
];

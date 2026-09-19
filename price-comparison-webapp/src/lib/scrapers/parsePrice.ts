/** Extracts a numeric price from strings like "Rs. 45,999", "PKR 12,500 - 15,000", "₨9,999.00". Returns the first number found, or null. */
export function parsePrice(raw: string | null | undefined): number | null {
  if (!raw) return null;
  const match = raw.replace(/,/g, "").match(/(\d+(?:\.\d+)?)/);
  if (!match) return null;
  const value = parseFloat(match[1]);
  return Number.isFinite(value) ? value : null;
}

export function toAbsoluteUrl(href: string | null | undefined, baseUrl: string): string {
  if (!href) return baseUrl;
  try {
    return new URL(href, baseUrl).toString();
  } catch {
    return baseUrl;
  }
}

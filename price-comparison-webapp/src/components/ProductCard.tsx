import type { ProductResult } from "@/lib/scrapers/types";

function formatPrice(price: number | null, currency: string): string {
  if (price === null) return "Price unavailable";
  return new Intl.NumberFormat("en-PK", {
    style: "currency",
    currency,
    maximumFractionDigits: 0,
  }).format(price);
}

export function ProductCard({
  product,
  view,
  isBestPrice,
}: {
  product: ProductResult;
  view: "list" | "grid";
  isBestPrice: boolean;
}) {
  if (view === "list") {
    return (
      <a
        href={product.productUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="flex items-center gap-4 rounded-lg border border-black/10 dark:border-white/10 p-3 hover:border-black/30 dark:hover:border-white/30 transition-colors"
      >
        <img
          src={product.imageUrl ?? "/placeholder-product.svg"}
          alt={product.title}
          className="h-16 w-16 rounded-md object-cover bg-black/5 dark:bg-white/5 shrink-0"
          loading="lazy"
        />
        <div className="min-w-0 flex-1">
          <p className="truncate font-medium">{product.title}</p>
          <p className="text-sm text-black/60 dark:text-white/60">{product.siteName}</p>
        </div>
        <div className="text-right shrink-0">
          <p className={`font-semibold ${isBestPrice ? "text-emerald-600 dark:text-emerald-400" : ""}`}>
            {formatPrice(product.price, product.currency)}
          </p>
          {isBestPrice && (
            <span className="text-xs font-medium text-emerald-600 dark:text-emerald-400">Best price</span>
          )}
        </div>
      </a>
    );
  }

  return (
    <a
      href={product.productUrl}
      target="_blank"
      rel="noopener noreferrer"
      className="flex flex-col rounded-lg border border-black/10 dark:border-white/10 overflow-hidden hover:border-black/30 dark:hover:border-white/30 transition-colors"
    >
      <div className="relative aspect-square bg-black/5 dark:bg-white/5">
        <img
          src={product.imageUrl ?? "/placeholder-product.svg"}
          alt={product.title}
          className="h-full w-full object-cover"
          loading="lazy"
        />
        {isBestPrice && (
          <span className="absolute top-2 left-2 rounded-full bg-emerald-600 text-white text-xs font-medium px-2 py-1">
            Best price
          </span>
        )}
      </div>
      <div className="p-3 flex flex-col gap-1">
        <p className="line-clamp-2 text-sm font-medium min-h-[2.5em]">{product.title}</p>
        <p className="text-xs text-black/60 dark:text-white/60">{product.siteName}</p>
        <p className={`font-semibold ${isBestPrice ? "text-emerald-600 dark:text-emerald-400" : ""}`}>
          {formatPrice(product.price, product.currency)}
        </p>
      </div>
    </a>
  );
}

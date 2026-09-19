import { chromium, type Browser, type Page } from "playwright";

const DEFAULT_USER_AGENT =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

let browserPromise: Promise<Browser> | null = null;

/**
 * A pre-installed browser path (set in sandboxed/offline environments that can't
 * `npx playwright install`). Left unset, Playwright uses its own managed browser.
 */
const executablePath = process.env.CHROMIUM_EXECUTABLE_PATH || undefined;

function getBrowser(): Promise<Browser> {
  if (!browserPromise) {
    browserPromise = chromium
      .launch({
        headless: true,
        executablePath,
        args: ["--no-sandbox", "--disable-dev-shm-usage"],
      })
      .catch((err) => {
        browserPromise = null;
        throw err;
      });
  }
  return browserPromise;
}

export async function withPage<T>(
  fn: (page: Page) => Promise<T>,
  timeoutMs = 20000
): Promise<T> {
  const browser = await getBrowser();
  const context = await browser.newContext({
    userAgent: DEFAULT_USER_AGENT,
    viewport: { width: 1366, height: 900 },
  });
  const page = await context.newPage();
  page.setDefaultTimeout(timeoutMs);
  page.setDefaultNavigationTimeout(timeoutMs);
  try {
    return await fn(page);
  } finally {
    await context.close();
  }
}

export async function closeBrowser(): Promise<void> {
  if (browserPromise) {
    const browser = await browserPromise;
    await browser.close();
    browserPromise = null;
  }
}

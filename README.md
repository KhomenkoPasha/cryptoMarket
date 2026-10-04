cryptoMarket
https://play.google.com/store/apps/details?id=app.khom.pavlo.crypto
- Cryptocurrency Market Capitalizations
- Bitcoin and Other Cryptocurrencies
Cryptocoins ranked by 24hr trading volume, price info, charts, market cap and news.

## Features

- **Price alerts** — push notifications when a coin crosses a target price or moves a set
  percentage over 24h. Alerts live in Room (`price_alerts`) and are checked every 15 minutes by
  WorkManager (`PriceAlertWorker`); the job only exists while at least one alert is armed.
  Android 13+ asks for the notification permission when the first alert is created, and an alert
  stays armed until a notification can actually be delivered. Alerts are part of the backup file.
- **Light and dark themes** — System / Light / Dark in Settings (`values/colors.xml` is the light
  palette, `values-night/colors.xml` the dark one).
- **Display currency** — USD, EUR, UAH, RUB or BTC. Data stays stored in USD; amounts are converted
  when shown and transaction input is converted back (`CurrencyManager`). Exchange rates come from
  CryptoCompare with Coinbase as a fallback and are cached for an hour.
- **Market mood** — Fear & Greed index (alternative.me) and BTC dominance / total market cap
  (CoinPaprika `/v1/global`) above the Rating list and in Insights. No API keys are needed.
- **Charts** — 1H / 24H / 7D / 1M / 1Y / All ranges, line (with gradient) or candlestick view, and
  a crosshair showing the date and price (OHLC for candles) under the finger.
- **Portfolio value history** — a daily snapshot of the portfolio value in Room
  (`portfolio_snapshots`), drawn as a line chart on the Portfolio tab with max drawdown and the best
  and worst day. The history is rebuilt from daily closes when transactions change, so it also
  covers the time before the app was installed. Drawdown and best/worst day are computed on a
  flow-neutral performance curve, so deposits and withdrawals don't count as gains or losses.
- **Several portfolios** — every transaction belongs to a portfolio (`holdings.portfolio_id`);
  the switcher at the top of the Portfolio tab filters the whole app (totals, favorites, widget).
  The last portfolio can't be deleted; deleting one removes its transactions.
- **Sales and fees** — transactions are Buy, Sell, Transfer in or Transfer out, each with a fee.
  Positions use the average cost method (`PositionCalculator`), which separates realized PnL
  (sales) from unrealized PnL and totals the fees paid. Selling more than is held is rejected when
  adding a transaction; imported history that does that is clamped instead of failing.
- **CSV export and import** — ⋮ menu → Export CSV (the app's own format, or Koinly's universal
  format) and Import CSV (the app's own export, Binance trade history, Coinbase transaction
  reports). Exchange rows are valued in USD only: pairs quoted in other currencies are skipped and
  counted in the preview. Rows the portfolio already has are not imported twice.
- **App lock** — Settings → App lock asks for a fingerprint, face or the device PIN / pattern /
  password (`androidx.biometric`, `BIOMETRIC_WEAK | DEVICE_CREDENTIAL`) when the app starts and when
  it returns after more than 30 seconds in the background. Turning it on or off requires
  authenticating. On Android 13+ the app switcher doesn't show a preview while it is on. If the
  device's screen lock is removed, the app lock turns itself off instead of locking you out.
  Home-screen widgets are not covered by it.

## Build requirements

- Android 8.0 (API 26) or newer
- compileSdk / targetSdk 36
- JDK 17
- Android Gradle Plugin 9.2.1
- Gradle 9.4.1 (included in the wrapper)

Run the project checks with:

```shell
./gradlew testDebugUnitTest lintDebug assembleRelease
./gradlew connectedDebugAndroidTest
```

LeakCanary is included only in debug builds for runtime leak detection.

## API keys

Add the CoinDesk key to `local.properties` (this file is ignored by Git):

```properties
COINDESK_API_KEY=your_coindesk_key
```

Legacy CryptoCompare authentication has moved to CoinDesk, so the same key is
used for `min-api.cryptocompare.com`. A separate `CRYPTOCOMPARE_API_KEY` can be
set as an override. The legacy key is sent only to that host using the
`authorization: Apikey ...` header.

## Release signing

Use the existing Google Play upload key. Do not commit the keystore or its passwords.
Add these values to the ignored `local.properties` file (use forward slashes in Windows paths):

```properties
RELEASE_STORE_FILE=C:/Users/your-user/keys/upload-key.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

Check the configuration and build the signed artifacts:

```shell
./gradlew releaseSigningStatus
./gradlew assembleRelease
./gradlew bundleRelease
```

The signed APK is written to `app/build/outputs/apk/release/`; the AAB is written to
`app/build/outputs/bundle/release/`.

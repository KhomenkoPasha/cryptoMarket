cryptoMarket
https://play.google.com/store/apps/details?id=app.khom.pavlo.crypto
- Cryptocurrency Market Capitalizations
- Bitcoin and Other Cryptocurrencies
Cryptocoins ranked by 24hr trading volume, price info, charts, market cap and news.

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

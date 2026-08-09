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

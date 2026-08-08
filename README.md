cryptoMarket
https://play.google.com/store/apps/details?id=app.khom.pavlo.crypto
- Cryptocurrency Market Capitalizations
- Bitcoin and Other Cryptocurrencies
Cryptocoins ranked by 24hr trading volume, price info, charts, market cap and news.

## API keys

Add the CoinDesk key to `local.properties` (this file is ignored by Git):

```properties
COINDESK_API_KEY=your_coindesk_key
```

Legacy CryptoCompare authentication has moved to CoinDesk, so the same key is
used for `min-api.cryptocompare.com`. A separate `CRYPTOCOMPARE_API_KEY` can be
set as an override. The legacy key is sent only to that host using the
`authorization: Apikey ...` header.

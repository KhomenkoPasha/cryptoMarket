package app.khom.pavlo.crypto.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ResponseCoinItem(@SerializedName("id")
                        @Expose var id: String?,
                            @SerializedName("name")
                        @Expose var name: String?,
                            @SerializedName("symbol")
                        @Expose var symbol: String?,
                            @SerializedName("rank")
                        @Expose var rank: String?,
                            @SerializedName("price_usd")
                        @Expose var priceUsd: String?,
                            @SerializedName("price_btc")
                        @Expose var priceBtc: String?,
                            @SerializedName("24h_volume_usd")
                        @Expose var _24hVolumeUsd: String?,
                            @SerializedName("market_cap_usd")
                        @Expose var marketCapUsd: String?,
                            @SerializedName("available_supply")
                        @Expose var availableSupply: String?,
                            @SerializedName("total_supply")
                        @Expose var totalSupply: String?,
                            @SerializedName("max_supply")
                        @Expose var maxSupply: String?,
                            @SerializedName("percent_change_1h")
                        @Expose var percentChange1h: String?,
                            @SerializedName("percent_change_24h")
                        @Expose var percentChange24h: String?,
                            @SerializedName("percent_change_7d")
                        @Expose var percentChange7d: String?,
                            @SerializedName("last_updated")
                        @Expose var lastUpdated: String?)


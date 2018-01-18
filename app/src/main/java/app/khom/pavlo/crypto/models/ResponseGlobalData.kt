package app.khom.pavlo.crypto.models


import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ResponseGlobalData(@SerializedName("total_market_cap_usd")
                            @Expose var total_market_cap_usd: String?,
                            @SerializedName("total_24h_volume_usd")
                            @Expose var total_24h_volume_usd: String?,
                            @SerializedName("bitcoin_percentage_of_market_cap")
                            @Expose var bitcoin_percentage_of_market_cap: String?,
                            @SerializedName("active_currencies")
                            @Expose var active_currencies: String?,
                            @SerializedName("price_usd")
                            @Expose var active_assets: String?,
                            @SerializedName("active_assets")
                            @Expose var priceBtc: String?,
                            @SerializedName("active_markets")
                            @Expose var active_markets: String?,
                            @SerializedName("last_updated")
                            @Expose var last_updated: String?)


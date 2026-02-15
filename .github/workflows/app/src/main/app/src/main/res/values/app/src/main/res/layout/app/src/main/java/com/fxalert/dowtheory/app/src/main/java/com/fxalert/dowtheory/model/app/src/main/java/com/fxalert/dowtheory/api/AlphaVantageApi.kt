package com.fxalert.dowtheory.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface AlphaVantageApi {
    
    @GET("query")
    suspend fun getForexIntraday(
        @Query("function") function: String = "FX_INTRADAY",
        @Query("from_symbol") fromSymbol: String = "USD",
        @Query("to_symbol") toSymbol: String = "JPY",
        @Query("interval") interval: String = "1min",
        @Query("apikey") apiKey: String,
        @Query("outputsize") outputSize: String = "full"
    ): Response<ForexIntradayResponse>
    
    companion object {
        private const val BASE_URL = "https://www.alphavantage.co/"
        
        fun create(): AlphaVantageApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AlphaVantageApi::class.java)
        }
    }
}

data class ForexIntradayResponse(
    @SerializedName("Meta Data")
    val metaData: MetaData?,
    
    @SerializedName("Time Series FX (1min)")
    val timeSeries: Map<String, TimeSeriesData>?
)

data class MetaData(
    @SerializedName("1. Information")
    val information: String,
    
    @SerializedName("2. From Symbol")
    val fromSymbol: String,
    
    @SerializedName("3. To Symbol")
    val toSymbol: String,
    
    @SerializedName("4. Last Refreshed")
    val lastRefreshed: String,
    
    @SerializedName("5. Interval")
    val interval: String,
    
    @SerializedName("6. Output Size")
    val outputSize: String,
    
    @SerializedName("7. Time Zone")
    val timeZone: String
)

data class TimeSeriesData(
    @SerializedName("1. open")
    val open: String,
    
    @SerializedName("2. high")
    val high: String,
    
    @SerializedName("3. low")
    val low: String,
    
    @SerializedName("4. close")
    val close: String
)

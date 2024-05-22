package ali.emir.merhaba.grmeengelliuygulama

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://maps.googleapis.com/"

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val directionsApiService: DirectionsApiService =
        retrofit.create(DirectionsApiService::class.java)
}

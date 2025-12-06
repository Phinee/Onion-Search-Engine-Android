package com.onionsearchengine.onionsearchengine



    import com.google.gson.annotations.SerializedName
    import retrofit2.Retrofit
    import retrofit2.converter.gson.GsonConverterFactory
    import retrofit2.http.GET
    import retrofit2.http.Header
    import retrofit2.http.POST
    import retrofit2.http.Query

data class ProvisionKeyResponse(
    val status: String,
    val api_key: String?
)


    data class SearchResponse(
        val status: String,
        val pagination: PaginationInfo,
        val results: List<SearchResult>

    )

data class PaginationInfo(
    @SerializedName("total_results")
    val totalResults: Int,
    @SerializedName("total_pages")
    val totalPages: Int
)

    data class SearchResult(
        val title: String?,
        val url: String?,
        @SerializedName("context")
        val snippet: String?
    )

    object RetrofitInstance {
        private val retrofit by lazy {
            Retrofit.Builder()
                .baseUrl("https://onionsearchengine.com/") 
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        val api: SearchApiService by lazy {
            retrofit.create(SearchApiService::class.java)
        }
    }

interface SearchApiService {
    @GET("api.php")
    suspend fun search(
        @Header("X-API-Key") apiKey: String,
        @Query("q") query: String,
        @Query("page") page: Int
    ): SearchResponse

    @POST("provision_device_key.php") 
    suspend fun provisionDeviceKey(): ProvisionKeyResponse
}

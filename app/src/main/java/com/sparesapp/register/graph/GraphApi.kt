package com.sparesapp.register.graph

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface GraphApi {

    @GET("me/drive")
    suspend fun myDrive(): Drive

    @GET("me/drives")
    suspend fun myDrives(): DriveList

    @GET("me/followedSites")
    suspend fun followedSites(): SiteList

    @GET("sites")
    suspend fun searchSites(@Query("search") query: String): SiteList

    @GET("sites/{siteId}/drives")
    suspend fun siteDrives(@Path("siteId") siteId: String): DriveList

    @GET("drives/{driveId}/root/children")
    suspend fun driveRootChildren(@Path("driveId") driveId: String): DriveItemList

    @GET("drives/{driveId}/items/{itemId}/children")
    suspend fun driveItemChildren(
        @Path("driveId") driveId: String,
        @Path("itemId") itemId: String,
    ): DriveItemList

    @GET("drives/{driveId}/items/{itemId}")
    suspend fun driveItem(
        @Path("driveId") driveId: String,
        @Path("itemId") itemId: String,
    ): DriveItem

    @Streaming
    @GET("drives/{driveId}/items/{itemId}/content")
    suspend fun downloadItemContent(
        @Path("driveId") driveId: String,
        @Path("itemId") itemId: String,
    ): ResponseBody

    @Streaming
    @GET
    suspend fun downloadFromUrl(@Url url: String): ResponseBody
}

package com.example.myapplication.repository

import android.content.Context
import android.net.Uri
import com.example.myapplication.api.UserService
import com.example.myapplication.data_model.LoginResponse
import com.example.myapplication.data_model.UserRegistrationRequest
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException

class UserRepositoryImpl(
    private val userService: UserService,
    private val context: Context,
    private val gson: Gson = Gson()
) : UserRepository {

    override suspend fun registerUser(
        request: UserRegistrationRequest,
        imageUri: Uri?
    ): Response<LoginResponse> {

        val userJsonString = gson.toJson(request)
        val userRequestBody = userJsonString.toRequestBody("application/json".toMediaTypeOrNull())

        val userPart = MultipartBody.Part.createFormData("user", null, userRequestBody)

        val imagePart: MultipartBody.Part? = uriToMultipartBodyPart(imageUri, "imageFiles")

        val parts = mutableListOf(userPart).apply {
            if (imagePart != null) {
                add(imagePart)
            }
        }

        return userService.registerUser(parts)
    }

    private fun uriToMultipartBodyPart(uri: Uri?, partName: String): MultipartBody.Part? {

        if (uri == null) return null

        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: return null

        val fileName = getFileName(uri) ?: "profile_image.jpg"

        val tempFile = File(context.cacheDir, fileName).apply {
            createNewFile()
        }

        try {
            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: IOException) {
            tempFile.delete()
            return null
        }

        val mimeType = context.contentResolver.getType(uri) ?: "image/*"

        val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())

        return MultipartBody.Part.createFormData(partName, tempFile.name, requestBody)
    }

    private fun getFileName(uri: Uri?): String? {
        if (uri == null) return null

        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex("_display_name")
                    if (nameIndex != -1) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.lastIndexOf('/')?.let { uri.path?.substring(it + 1) }
        }
        return result
    }
}
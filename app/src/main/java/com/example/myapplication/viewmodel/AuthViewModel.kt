package com.example.myapplication.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data_model.RegisterRequest
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.provider.OpenableColumns
import com.example.myapplication.API.RetrofitClient

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    val apiService = RetrofitClient.apiService

    fun register(
        registerRequest: RegisterRequest,
        imageUris: List<Uri>,
        contentResolver: ContentResolver
    ) {
        viewModelScope.launch {
            try {
                // 1. JSON 요청 객체를 RequestBody로 변환
                val userJson = Gson().toJson(registerRequest)
                val userRequestBody = userJson.toRequestBody("application/json".toMediaTypeOrNull()) // 💡 JSON 데이터를 RequestBody로 변환

                // 2. 이미지 URI 리스트를 MultipartBody.Part 리스트로 변환
                val imageParts = imageUris.map { uri ->
                    val file = getFileFromUri(contentResolver, uri) // 💡 URI -> 임시 File 객체로 변환
                    val imageRequestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull()) // 💡 File을 이미지 RequestBody로 변환

                    // 💡 Multipart 요청의 핵심: 필드 이름 ("imageFiles"), 파일 이름, 이미지 데이터 (RequestBody)
                    MultipartBody.Part.createFormData("imageFiles", file.name, imageRequestBody)
                }

                // 3. API 호출: @Part("user")와 @Part List<MultipartBody.Part>를 함께 전송
                val response = apiService.signup(
                    user = userRequestBody,
                    imageFiles = imageParts
                )

                if (response.isSuccessful) {
                    val loginResponse = response.body() // 💡 성공 응답 데이터
                } else {
                    // 회원가입 실패 (ID 중복 등)
                }

            } catch (e: Exception) {
                // 통신 오류
            }
        }
    }

    private fun getFileFromUri(contentResolver: ContentResolver, uri: Uri): File {
        val inputStream: InputStream? = contentResolver.openInputStream(uri) // 💡 URI로부터 데이터 스트림을 얻음

        // 💡 파일 이름 추출 로직 (없을 경우 임시 이름 생성)
        val filename = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        } ?: "temp_upload_${System.currentTimeMillis()}.jpg"

        // 💡 앱의 캐시 디렉토리에 임시 파일 생성
        val tempFile = File(getApplication<Application>().cacheDir, filename)

        // 💡 InputStream의 데이터를 임시 파일로 복사 (실제 파일 생성)
        inputStream?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        return tempFile // 💡 Multipart 요청에 사용될 임시 File 객체 반환
    }
}
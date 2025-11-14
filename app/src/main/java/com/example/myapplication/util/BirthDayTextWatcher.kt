// com.example.myapplication.util.BirthDayTextWatcher.kt
package com.example.myapplication.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class BirthDayTextWatcher(private val editText: EditText) : TextWatcher {

    // 💡 핵심: 재귀 호출 방지 플래그
    private var isUpdating = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable) {
        // 1. 이미 업데이트 중인 경우 무시 (재귀 방지)
        if (isUpdating) return

        val inputText = s.toString()
        // 숫자만 추출
        val cleanString = inputText.replace("[^\\d]".toRegex(), "")

        // 최대 8자리 (YYYYMMDD)로 제한
        val maxDigits = 8
        val truncatedString = if (cleanString.length > maxDigits) {
            cleanString.substring(0, maxDigits)
        } else {
            cleanString
        }

        val sb = StringBuilder(truncatedString)

        // YYYY-MM-DD 형식으로 하이픈 삽입
        if (sb.length >= 5) {
            sb.insert(4, '-')
        }

        // line 47 근처에서 문제가 발생했으므로, 이 부분을 포함하여 수정합니다.
        if (sb.length >= 8) {
            sb.insert(7, '-') // <- 이 줄 근처에서 문제가 발생했을 가능성이 높습니다.
        }

        val formattedString = sb.toString()

        // 텍스트가 변경되어야 할 경우에만 setText 호출
        if (formattedString != inputText) {
            // 2. 텍스트 변경을 시작하기 전에 플래그를 설정합니다.
            isUpdating = true

            // 3. 텍스트를 설정 (이 호출이 afterTextChanged를 다시 트리거함)
            editText.setText(formattedString)

            // 4. 텍스트 설정 후 즉시 커서 위치를 재설정
            val selectionIndex = formattedString.length.coerceAtMost(editText.length())
            editText.setSelection(selectionIndex)

            // 5. 플래그 해제
            isUpdating = false
        }
    }
}
package com.example.myapplication.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class BirthDayTextWatcher(private val editText: EditText) : TextWatcher {

    // 재귀 호출 방지 플래그
    private var isUpdating = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable) {
        // 이미 업데이트 중인 경우 무시 (재귀 방지)
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

        if (sb.length >= 8) {
            sb.insert(7, '-')
        }

        val formattedString = sb.toString()

        // 텍스트가 변경되어야 할 경우에만 처리
        if (formattedString != inputText) {

            // 텍스트 변경을 시작하기 전에 플래그를 설정합니다.
            isUpdating = true

            // 텍스트를 설정 (이 호출이 afterTextChanged를 다시 트리거함)
            editText.setText(formattedString)

            // 텍스트 설정 후 즉시 커서 위치를 재설정하고 플래그를 해제합니다.
            //    setText() 호출 직후 다음 코드가 실행되도록 합니다.
            val selectionIndex = formattedString.length.coerceAtMost(editText.length())
            editText.setSelection(selectionIndex)

            // 플래그 해제
            isUpdating = false // setText() 호출이 완료된 후 플래그를 해제합니다.
        }
    }
}
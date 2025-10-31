package com.example.myapplication.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.util.Calendar

class BirthDateTextWatcher(private val editText: EditText) : TextWatcher {

    private var currentText = ""
    private val calendar: Calendar = Calendar.getInstance()

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable) {
        val inputText = s.toString()

        // 현재 텍스트와 입력된 텍스트가 같다면 무한 루프를 방지하기 위해 종료
        if (inputText == currentText) {
            return
        }

        // 숫자 이외의 모든 문자를 제거 (하이픈 포함)
        val cleanString = inputText.replace("[^\\d]".toRegex(), "")

        // YYYYMMDD 형식으로 최대 8자리까지만 사용
        val truncatedString = if (cleanString.length >= 8) {
            cleanString.substring(0, 8)
        } else {
            cleanString
        }

        // 길이에 따라 YYYY-MM-DD 형식으로 하이픈 추가
        val formattedString = when {
            truncatedString.length >= 5 -> {
                // YYYY-MM...
                val part1 = truncatedString.substring(0, 4)
                val part2 = truncatedString.substring(4)
                if (truncatedString.length >= 7) {
                    // YYYY-MM-DD
                    val part2_1 = part2.substring(0, 2)
                    val part2_2 = part2.substring(2)
                    "$part1-$part2_1-$part2_2"
                } else {
                    "$part1-$part2"
                }
            }
            truncatedString.length > 0 -> {
                // YYYY...
                truncatedString
            }
            else -> {
                ""
            }
        }

        currentText = formattedString

        // TextWatcher를 잠시 비활성화하여 무한 루프 방지
        editText.removeTextChangedListener(this)
        // 포매팅된 텍스트로 설정
        editText.setText(formattedString)
        // 커서를 항상 텍스트의 끝으로 이동
        editText.setSelection(formattedString.length)
        // TextWatcher 다시 활성화
        editText.addTextChangedListener(this)
    }
}

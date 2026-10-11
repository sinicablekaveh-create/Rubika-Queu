#!/usr/bin/env python3
"""Guarded v3 overlay for the official, user-confirmed Bot API test screen."""
from pathlib import Path
import shutil

ROOT = Path("app/src/main/java/ir/sinicable/rubikaqueue")

def replace_once(source, before, after):
    if source.count(before) != 1:
        raise SystemExit("Unexpected official Bot API source: expected one exact anchor")
    return source.replace(before, after, 1)

api = ROOT / "OfficialBotApi.java"
s = api.read_text(encoding="utf-8")
s = replace_once(s,
    '    public JSONObject sendMessage(String chatId, String text) throws Exception {\n',
    '    public JSONObject sendMessage(String chatId, String text) throws Exception {\n'
    '        return sendMessage(chatId, text, null, false);\n'
    '    }\n\n'
    '    /** User-confirmed send with documented optional fields. */\n'
    '    public JSONObject sendMessage(String chatId, String text, String replyToId,\n'
    '                                  boolean disableNotification) throws Exception {\n')
s = replace_once(s,
    '        payload.put("text", text);\n        return call("sendMessage", payload);\n',
    '        payload.put("text", text);\n'
    '        String validatedId = BotSendOptions.validateReplyTo(replyToId);\n'
    '        if (validatedId != null) payload.put("reply_to_message_id", validatedId);\n'
    '        if (disableNotification) payload.put("disable_notification", true);\n'
    '        return call("sendMessage", payload);\n')

ui = ROOT / "OfficialBotActivity.java"
t = ui.read_text(encoding="utf-8")
t = replace_once(t, 'import android.widget.Button;\n',
    'import android.widget.Button;\nimport android.widget.CheckBox;\n')
t = replace_once(t,
    '    private EditText tokenField, chatField, textField;\n',
    '    private EditText tokenField, chatField, textField, replyToField;\n'
    '    private CheckBox silentSendBox;\n')
t = replace_once(t,
    '        textField.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4200)});\n',
    '        textField.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4200)});\n'
    '        replyToField = field(root, "شناسه پیام برای پاسخ (اختیاری)", InputType.TYPE_CLASS_TEXT);\n'
    '        replyToField.setSingleLine(true);\n'
    '        replyToField.setTextDirection(View.TEXT_DIRECTION_LTR);\n'
    '        replyToField.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(180)});\n'
    '        silentSendBox = new CheckBox(this);\n'
    '        silentSendBox.setText("ارسال بدون اعلان (disable_notification)");\n'
    '        root.addView(silentSendBox);\n')
t = replace_once(t,
    '        String text=textField.getText().toString();\n',
    '        String text=textField.getText().toString();\n'
    '        String replyTo;\n'
    '        try { replyTo = BotSendOptions.validateReplyTo(replyToField.getText().toString()); }\n'
    '        catch (IllegalArgumentException e) {\n'
    '            Toast.makeText(this,"شناسه پیام برای پاسخ معتبر نیست",Toast.LENGTH_LONG).show();\n'
    '            return;\n'
    '        }\n'
    '        boolean silent = silentSendBox.isChecked();\n')
t = replace_once(t,
    '            .setMessage("یک پیام به چت " + chat + " ارسال شود؟\\n\\n" + text)\n',
    '            .setMessage("یک پیام به چت " + chat + " ارسال شود؟\\n"\n'
    '                    + "پاسخ به پیام: " + (replyTo == null ? "خیر" : replyTo) + "\\n"\n'
    '                    + "بدون اعلان: " + (silent ? "بله" : "خیر") + "\\n\\n" + text)\n')
t = replace_once(t,
    '                JSONObject response = new OfficialBotApi(token).sendMessage(chat,text);\n',
    '                JSONObject response = new OfficialBotApi(token).sendMessage(chat,text,replyTo,silent);\n')

# Commit only after every expected anchor was found.
api.write_text(s, encoding="utf-8")
ui.write_text(t, encoding="utf-8")
shutil.copyfile("overrides/bot-options-v3/BotSendOptions.java", ROOT / "BotSendOptions.java")
print("Bot API message options v3 overlay applied")

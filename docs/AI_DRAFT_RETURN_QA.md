# AI draft return QA (1.3.1)

This patch completes the existing user-initiated AI drafting flow.

- The AI screen is launched for result (request code 104) from the main composer.
- Only **successful** HTTPS Ollama model output can be copied or returned; error messages and placeholder UI text cannot be used as a draft.
- Generated output must be nonempty and at most 4200 characters. Existing Java regression tests now cover accepted/rejected suggestions.
- Returning generated output requires an explicit action and a confirmation dialog. The main message composer asks the user for a **second confirmation** before replacing its text.
- File chooser results (101 and 103) are unaffected because AI return code 104 is handled before the existing URI requirement.
- No Rubika message is sent, scheduled, or queued by the AI screen. The personal account, contact import, retry engine and Bot API are not modified.
- The ChatGPT browser path is still a manual clipboard handoff; this is not a free direct OpenAI API integration.
- APK versionCode is 14; CI continues lintDebug, assembleDebug, APK signature verification and alignment gates.

Manual device verification recommended: send a sample prompt to a user-controlled HTTPS Ollama server; return a draft, cancel replacement, retry and accept; verify the original queue remains stopped. A live model/device is not covered by CI.

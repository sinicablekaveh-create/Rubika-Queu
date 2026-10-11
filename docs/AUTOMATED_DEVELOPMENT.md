# Supervised automatic development through GitHub Actions and online Codex

This repository includes an opt-in workflow at
`.github/workflows/ai-development.yml` to produce **draft pull requests**
from narrowly scoped development instructions. It uses the official
`openai/codex-action@v1`; it does not use a browser session from
`https://chatgpt.com`, and it does **not** run continuously.

## Setup (required once)

1. On the GitHub repository, open **Settings → Secrets and variables → Actions**
   and add an Actions repository secret named **`OPENAI_API_KEY`**. Create
   this credential using the official OpenAI Platform account, **not** the
   ChatGPT website. Never paste it into a GitHub issue, source file, Android
   text field, chat transcript, or workflow input.
2. Check GitHub **Settings → Actions → General → Workflow permissions** and
   allow Actions to create pull requests if that setting is available.
   Organization policy may prevent this.
3. Merge the reviewed PR that introduces this workflow into `main`.
   The workflow_dispatch button typically becomes available from `main`
   after that merge.

The API credential is subject to separate API billing and usage limits;
a ChatGPT subscription is not the same as API credits. Set an API budget.
The Codex Tasks plugin may show no registered environments; this separate
GitHub Actions workflow does not depend on those environments.

## Operation

1. Open **Actions → AI-Assisted Development Proposal → Run workflow** on `main`.
2. Enter one bounded task, such as "Improve Persian search term normalization
   and add a regression test. Preserve existing queue behavior."
3. The first, read-only GitHub Actions job asks online Codex to write a
   patch. It cannot modify the repository or use a GitHub write token.
4. A distinct job validates the generated diff and rejects changes outside
   the allowed assistant, discovery, tests and documentation paths. It then
   opens a **draft PR**, if the diff is syntactically valid and non-empty.
5. Read the proposed source code; once approved, mark the PR **Ready for
   review**. The Android APK build workflow intentionally skips draft PRs
   to avoid loading signing secrets alongside unreviewed generated code.
   Complete CI, Android lint, APK signature validation, and a human review
   before merging. Merge and release are never automatic.

Unsafe, invalid, overlarge, or empty patches are rejected without opening
a PR. Codex can generate a patch that fails to apply; inspect the Actions
run log and rerun with a smaller, more precise task.

## Boundaries

- No application authentication, Rubika message sending or queue core,
  GitHub workflow definitions, signing keys, Gradle scripts, bundled ZIPs,
  or binary artifacts can be modified by the automation.
- The workflow accepts manually initiated development instructions from
  repository collaborators, not unauthenticated public issues or schedules.
- Source from repositories, comments, prompts, and generated patches is
  untrusted. This workflow limits file access and applies model output as
  inert data in a separate job. It is still essential to review generated
  code before allowing privileged CI and merging.
- This workflow **writes development proposals in GitHub only**. It is
  separate from the in-app optional online AI text drafting feature:
  `docs/ONLINE_AI_GATEWAY.md` on that feature's branch/PR.

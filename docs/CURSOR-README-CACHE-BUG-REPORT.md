# Cursor bug report: Agent-edited README / markdown preview stale after restart

**Copy this to [forum.cursor.com](https://forum.cursor.com) → Bug Reports, and/or Help → Report Issue in Cursor.**

---

## Title

Markdown preview and editor show stale README after Agent edits; disk file is correct; full restart does not fix

## Environment

- **OS:** Windows 10/11 (build 26200)
- **Cursor version:** _(fill in: Help → About Cursor)_
- **VS Code comparison:** Same file opens correctly in Visual Studio / VS Code with updated content
- **Workspace:** Monorepo opened at parent folder (`minecraft_mods/`), editing `FairStructureLoot/README.md`
- **Date observed:** 2026-07-09

## Request ID

_(Chat → … menu → Copy Request ID from the Agent session that edited README.md)_

## Summary

When Cursor Agent modifies `README.md` (via write/search_replace), the file on disk updates correctly, but **Cursor’s markdown preview and sometimes the in-editor view do not show the new content**. Closing the preview tab, reloading the window, and **fully quitting and restarting Cursor** still show the old README. The same path opened in **Visual Studio** shows the current file immediately.

This forced repeated re-work across a multi-hour session (icon paths, base64 embeds, HUD triangle docs) because there was no reliable way to confirm what the README actually displayed inside Cursor.

## Steps to reproduce

1. Open a monorepo workspace (e.g. `parent_repo/`) containing `SubProject/README.md`.
2. Use Cursor Agent to edit `SubProject/README.md` (images, HTML embeds, table rows).
3. Verify on disk: file content is updated (open in Notepad, VS Code, or `git diff`).
4. In Cursor: open `SubProject/README.md` → Markdown Preview (`Ctrl+Shift+V`).
5. **Actual:** Preview shows pre-edit content (old icon, old HUD row, etc.).
6. Try: close preview → `File: Revert File` → save → `Developer: Reload Window` → quit Cursor completely → reopen.
7. **Actual:** Preview still stale in Cursor.
8. Open the same file in VS Code or Visual Studio.
9. **Expected vs actual:** Other editors show new content; Cursor does not.

## Expected behavior

- Markdown preview should refresh when the underlying file changes on disk.
- After Agent edits, preview should match disk without manual cache clearing.
- Full application restart should never restore an older in-memory snapshot over a saved file.

## Actual behavior

- Preview appears bound to a **cached render** or **stale TextDocument snapshot**, not the current file.
- Relative image paths and HTML `<img>` blocks may never update in preview even when disk content changes.
- User cannot trust Cursor preview for documentation work; must use an external editor to verify.

## Workarounds attempted (none reliable)

- Switch from `./assets/icon.png` to `FairStructureLoot/assets/icon.png` to monorepo path
- Inline base64 `data:` URIs in README (still stale in preview)
- `File: Revert File`, manual save, reload window, full restart
- Opening README in text editor before preview
- External PowerShell rewrite of README to bust OS file watcher

## Impact

- **High:** Documentation and GitHub-ready README work blocked inside Cursor
- Wasted hours re-implementing README/icon/HUD content because preview could not be trusted
- Agent sessions re-derived lost work because user could not see prior results in IDE

## Related reports (same class of bug)

- [Create/Update .md files — Assertion Failed](https://forum.cursor.com/t/create-update-md-files-unable-to-open-assertion-failed-argument-is-undefined-or-null/140018)
- [Assertion error — markdown preview editors](https://forum.cursor.com/t/assertion-error-when-using-custom-editors-incl-markdown-preview-editors/148578)
- VS Code: [Markdown Preview shows outdated file content](https://github.com/microsoft/vscode/issues/194421)

## Suggested fix

1. After Agent `write` / `search_replace` on `.md` files, fully initialize TextDocument state and invalidate markdown preview cache.
2. On preview open, always re-read from disk if file mtime changed.
3. On Windows, do not restore LF snapshots from `state.vscdb` over user-saved CRLF files on restart (if applicable).

## Attachments

- Screenshot: Cursor preview vs VS showing different README content _(attach if available)_
- Sample README with base64 icon + HUD table row that fails to refresh in Cursor only

---

**In Cursor IDE:** Help → Report Issue (paste this text + Request ID).

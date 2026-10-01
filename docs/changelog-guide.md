# Changelog instructions

`CHANGELOG.md` and GitHub release notes are user-facing. Follow these rules
every time they are written or edited:

1. **Never mention Tomato or Mihon.** They are private development
   references (design studied in `reference/`, inspiration only). Describe
   what the user sees and feels — "slide transitions", "hub with
   sub-screens", "predictive back" — never where the idea was studied.
2. **User-facing changes only.** No refactors, no dependency bumps, no test
   or CI work unless it changes behavior the user can observe.
3. **One entry per release**, newest on top, with a compare link to the
   previous tag.
4. **Check before committing:** `grep -ri "tomato\|mihon" CHANGELOG.md`
   must return nothing.

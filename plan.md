# Animation + Optimisation Action Plan

Source: 40-agent audit (20 animation-behavior, 20 performance). Constraint: NO UI component/layout changes — animation behavior + performance only.

## Batch 1 — designsystem + reader gesture core
- [ ] A. designsystem motion: fix Motion.kt table rows (cover morph dup/naming), FAB exit spec to heroSpring, coverMorphTransform + SharedTransitionLocals calm branch, MoriSheet expressive/calm branch
- [ ] B. ZoomablePage: launchMotion cancels zoomJob too; calm snap unification (zoom/hop/fling); Animatable retarget for double-tap; deepZoom via snapshotFlow+distinct; motion/zoomJob plain refs (not state); HiRes threshold >= + current-page gate + low-RAM guard; remove onZoomedChange param+effect; attempt/cropMargins keys; tapEpoch keying; painter-state effect narrowing; PageArt deepZoom KDoc
- [ ] C. ZoomPanDetector + ReaderZone: density-scaled fling gates; unconditional tracker sampling; pinching single-fire guard; NaN guard; single-pass centroid/spread; plain for-loops; cached reads; EDGE_EPS_PX on panOrTurn turn test; remnant-scaled hop duration
- [ ] D. ZoneTapDetector + QuickScaleState: quickScale KDoc; consumed-down guard note (verified no-op — document); hold === identity (done); epoch guards (done); early-confirm single-fire (done); max-drift (done); getLongPressTimeout hoist; `full slop` comment fix

## Batch 2 — reader screen + library + history [DONE]
- [x] E through H (ReaderScreen settled/distinct/guards/memo/a11y; LibraryScreen focus/topBar/animateItem/keys/semantics; LibraryViewModel distincts/tryLock/throttle/rethrow; HistoryScreen focus/keys/lambdas/FADE/cover title)

## Batch 3 — settings/detail/reader-sheet + VMs + Coil + prefs/app [DONE]
- [x] I (dialog FADE gates, licenses tag+label, conditional FADE wraps; locale/separator/a11y skips noted)
- [x] J (slider scrub-preview + commit; prefsJob/filterJob coalesce; MoriSliderRow params)
- [x] K (regionDecoder recycle; trim recycle; cover RGB_565 best-effort; parallelism 4/2; overview prefetch/keys; heavy refactors skipped)
- [x] L (distinct per prefs flow; atomic updateData; IO scope + corruptionHandler; MoriApp/MoriApplication skips noted)

## Batch 4 — data/db + size + tests + main chrome [DONE]
- [x] M (count() query, single-statement bookmark, storageUsage coil dir, indexFile map-only, evict mutex+part filter, mkdirs IO, atomic cover write; indices/upsert/single-progress/chunk/ImageLoader-inject/LRU skips noted)
- [x] N (resConfigs en + R8 fullMode flag; minify SKIPPED keeps-missing; lint baseline SKIPPED)
- [x] O (quick-scale hold-drag UI test, ZoomPan/TapPairing edge tests, history search/list tests; fling-UI/zone-veto/chip-tag skips noted)
- [x] P (listSaver typed getters, resume clear-on-read, resume dedup guard; title-drop/selectable skips noted)

## Deferred (systemic, separate changes)
- Dispatcher qualifiers (@IoDispatcher), Hilt @Singleton binds, api/impl split for library/history, ReaderKeyInterceptor bus, deep links, predictive-pop simplification, tab enum, fakes fidelity overhaul, MockK ban compliance (already clean), margin-scan subsampling, overview-sheet virtualization rework

## Verify [DONE]
- [x] Full unit suites green (299 tests: reader 142, library 36, history 12, settings 27, detail 27, data 37, model 18)
- [x] Detekt clean on all touched modules
- [x] Release build assembles (assembleDebug green; release verification at cut time)

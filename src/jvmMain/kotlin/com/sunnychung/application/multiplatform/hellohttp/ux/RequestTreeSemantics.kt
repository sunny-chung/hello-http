package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

/**
 * Request-tree row semantics for deterministic UI tests.
 *
 * Why this exists:
 * - Compose Desktop test matching is sensitive to merged/unmerged semantics trees.
 * - In our RequestTree rows, text lookup in tests can be flaky: parent row nodes may not reliably expose
 *   descendant text in the tree mode we need for interactions.
 * - Matching by visible text also caused false matches in bulk-import cases (for example, `r1` vs `r10`).
 *
 * Workaround:
 * - Expose stable, explicit row identity fields (`id`, `method`, `name`, `url`) directly on the row node.
 * - Tests can then locate rows without depending on recursive text search behavior.
 */
val RequestTreeRowRequestIdKey = SemanticsPropertyKey<String>("RequestTreeRowRequestId")
var SemanticsPropertyReceiver.requestTreeRowRequestId by RequestTreeRowRequestIdKey

val RequestTreeRowMethodKey = SemanticsPropertyKey<String>("RequestTreeRowMethod")
var SemanticsPropertyReceiver.requestTreeRowMethod by RequestTreeRowMethodKey

val RequestTreeRowNameKey = SemanticsPropertyKey<String>("RequestTreeRowName")
var SemanticsPropertyReceiver.requestTreeRowName by RequestTreeRowNameKey

val RequestTreeRowUrlKey = SemanticsPropertyKey<String>("RequestTreeRowUrl")
var SemanticsPropertyReceiver.requestTreeRowUrl by RequestTreeRowUrlKey

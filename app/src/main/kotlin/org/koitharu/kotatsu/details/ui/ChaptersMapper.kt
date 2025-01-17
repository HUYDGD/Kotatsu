package org.koitharu.kotatsu.details.ui

import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.details.data.MangaDetails
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.toListItem
import org.koitharu.kotatsu.parsers.util.mapToSet

fun MangaDetails.mapChapters(
	history: MangaHistory?,
	newCount: Int,
	branch: String?,
	bookmarks: List<Bookmark>,
): List<ChapterListItem> {
	val remoteChapters = chapters[branch].orEmpty()
	val localChapters = local?.manga?.getChapters(branch).orEmpty()
	if (remoteChapters.isEmpty() && localChapters.isEmpty()) {
		return emptyList()
	}
	val bookmarked = bookmarks.mapToSet { it.chapterId }
	val currentId = history?.chapterId ?: 0L
	val newFrom = if (newCount == 0 || remoteChapters.isEmpty()) Int.MAX_VALUE else remoteChapters.size - newCount
	val ids = buildSet(maxOf(remoteChapters.size, localChapters.size)) {
		remoteChapters.mapTo(this) { it.id }
		localChapters.mapTo(this) { it.id }
	}
	val result = ArrayList<ChapterListItem>(ids.size)
	val localMap = if (localChapters.isNotEmpty()) {
		localChapters.associateByTo(LinkedHashMap(localChapters.size)) { it.id }
	} else {
		null
	}
	var isUnread = currentId !in ids
	for (chapter in remoteChapters) {
		val local = localMap?.remove(chapter.id)
		if (chapter.id == currentId) {
			isUnread = true
		}
		result += (local ?: chapter).toListItem(
			isCurrent = chapter.id == currentId,
			isUnread = isUnread,
			isNew = isUnread && result.size >= newFrom,
			isDownloaded = local != null,
			isBookmarked = chapter.id in bookmarked,
		)
	}
	if (!localMap.isNullOrEmpty()) {
		for (chapter in localMap.values) {
			if (chapter.id == currentId) {
				isUnread = true
			}
			result += chapter.toListItem(
				isCurrent = chapter.id == currentId,
				isUnread = isUnread,
				isNew = false,
				isDownloaded = !isLocal,
				isBookmarked = chapter.id in bookmarked,
			)
		}
	}
	return result
}

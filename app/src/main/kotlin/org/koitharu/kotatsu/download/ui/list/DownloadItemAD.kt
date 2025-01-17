package org.koitharu.kotatsu.download.ui.list

import android.transition.TransitionManager
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.image.TrimTransformation
import org.koitharu.kotatsu.core.util.ext.drawableEnd
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.core.util.ext.source
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ItemDownloadBinding
import org.koitharu.kotatsu.download.ui.list.chapters.DownloadChapter
import org.koitharu.kotatsu.download.ui.list.chapters.downloadChapterAD
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.util.format

fun downloadItemAD(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	listener: DownloadItemListener,
) = adapterDelegateViewBinding<DownloadItemModel, ListModel, ItemDownloadBinding>(
	{ inflater, parent -> ItemDownloadBinding.inflate(inflater, parent, false) },
) {

	val percentPattern = context.resources.getString(R.string.percent_string_pattern)
	val expandIcon = ContextCompat.getDrawable(context, R.drawable.ic_expand_collapse)
	val chaptersAdapter = BaseListAdapter<DownloadChapter>()
		.addDelegate(ListItemType.CHAPTER, downloadChapterAD())

	val clickListener = object : View.OnClickListener, View.OnLongClickListener {
		override fun onClick(v: View) {
			when (v.id) {
				R.id.button_cancel -> listener.onCancelClick(item)
				R.id.button_resume -> listener.onResumeClick(item)
				R.id.button_pause -> listener.onPauseClick(item)
				else -> listener.onItemClick(item, v)
			}
		}

		override fun onLongClick(v: View): Boolean {
			return listener.onItemLongClick(item, v)
		}
	}
	binding.buttonCancel.setOnClickListener(clickListener)
	binding.buttonPause.setOnClickListener(clickListener)
	binding.buttonResume.setOnClickListener(clickListener)
	itemView.setOnClickListener(clickListener)
	itemView.setOnLongClickListener(clickListener)
	binding.recyclerViewChapters.addItemDecoration(DividerItemDecoration(context, RecyclerView.VERTICAL))
	binding.recyclerViewChapters.adapter = chaptersAdapter

	bind { payloads ->
		if (ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED in payloads && context.isAnimationsEnabled) {
			TransitionManager.beginDelayedTransition(binding.constraintLayout)
		}
		binding.textViewTitle.text = item.manga.title
		binding.imageViewCover.newImageRequest(lifecycleOwner, item.manga.coverUrl)?.apply {
			placeholder(R.drawable.ic_placeholder)
			fallback(R.drawable.ic_placeholder)
			error(R.drawable.ic_error_placeholder)
			allowRgb565(true)
			transformations(TrimTransformation())
			source(item.manga.source)
			enqueueWith(coil)
		}
		binding.textViewTitle.isChecked = item.isExpanded
		binding.textViewTitle.drawableEnd = if (item.isExpandable) expandIcon else null
		binding.cardDetails.isVisible = item.isExpanded
		chaptersAdapter.items = item.chapters
		when (item.workState) {
			WorkInfo.State.ENQUEUED,
			WorkInfo.State.BLOCKED -> {
				binding.textViewStatus.setText(R.string.queued)
				binding.progressBar.isIndeterminate = false
				binding.progressBar.isVisible = false
				binding.progressBar.isEnabled = true
				binding.textViewPercent.isVisible = false
				binding.textViewDetails.isVisible = false
				binding.buttonCancel.isVisible = true
				binding.buttonResume.isVisible = false
				binding.buttonPause.isVisible = false
			}

			WorkInfo.State.RUNNING -> {
				binding.textViewStatus.setText(
					if (item.isPaused) R.string.paused else R.string.manga_downloading_,
				)
				binding.progressBar.isIndeterminate = item.isIndeterminate
				binding.progressBar.isVisible = true
				binding.progressBar.max = item.max
				binding.progressBar.isEnabled = !item.isPaused
				binding.progressBar.setProgressCompat(item.progress, payloads.isNotEmpty())
				binding.textViewPercent.text = percentPattern.format((item.percent * 100f).format(1))
				binding.textViewPercent.isVisible = true
				binding.textViewDetails.textAndVisible = item.getEtaString()
				binding.buttonCancel.isVisible = true
				binding.buttonResume.isVisible = item.isPaused
				binding.buttonPause.isVisible = item.canPause
			}

			WorkInfo.State.SUCCEEDED -> {
				binding.textViewStatus.setText(R.string.download_complete)
				binding.progressBar.isIndeterminate = false
				binding.progressBar.isVisible = false
				binding.progressBar.isEnabled = true
				binding.textViewPercent.isVisible = false
				if (item.totalChapters > 0) {
					binding.textViewDetails.text = context.resources.getQuantityString(
						R.plurals.chapters,
						item.totalChapters,
						item.totalChapters,
					)
					binding.textViewDetails.isVisible = true
				} else {
					binding.textViewDetails.isVisible = false
				}
				binding.buttonCancel.isVisible = false
				binding.buttonResume.isVisible = false
				binding.buttonPause.isVisible = false
			}

			WorkInfo.State.FAILED -> {
				binding.textViewStatus.setText(R.string.error_occurred)
				binding.progressBar.isIndeterminate = false
				binding.progressBar.isVisible = false
				binding.progressBar.isEnabled = true
				binding.textViewPercent.isVisible = false
				binding.textViewDetails.textAndVisible = item.error
				binding.buttonCancel.isVisible = false
				binding.buttonResume.isVisible = false
				binding.buttonPause.isVisible = false
			}

			WorkInfo.State.CANCELLED -> {
				binding.textViewStatus.setText(R.string.canceled)
				binding.progressBar.isIndeterminate = false
				binding.progressBar.isVisible = false
				binding.progressBar.isEnabled = true
				binding.textViewPercent.isVisible = false
				binding.textViewDetails.isVisible = false
				binding.buttonCancel.isVisible = false
				binding.buttonResume.isVisible = false
				binding.buttonPause.isVisible = false
			}
		}
	}
}

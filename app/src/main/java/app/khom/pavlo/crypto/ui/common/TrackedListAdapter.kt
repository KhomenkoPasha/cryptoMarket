package app.khom.pavlo.crypto.ui.common

import androidx.recyclerview.widget.RecyclerView

/**
 * Dispatches valid range updates for lists which are mutated by the existing presenters.
 * This avoids rebinding newly inserted or removed rows through notifyDataSetChanged().
 */
abstract class TrackedListAdapter<VH : RecyclerView.ViewHolder>(
    initialItemCount: Int
) : RecyclerView.Adapter<VH>() {

    private var dispatchedItemCount = initialItemCount

    protected fun dispatchTrackedListChanges(currentItemCount: Int) {
        require(currentItemCount >= 0) { "Item count cannot be negative" }

        val previousItemCount = dispatchedItemCount
        when {
            currentItemCount > previousItemCount -> notifyItemRangeInserted(
                previousItemCount,
                currentItemCount - previousItemCount
            )
            currentItemCount < previousItemCount -> notifyItemRangeRemoved(
                currentItemCount,
                previousItemCount - currentItemCount
            )
        }

        val retainedItemCount = minOf(previousItemCount, currentItemCount)
        if (retainedItemCount > 0) notifyItemRangeChanged(0, retainedItemCount)
        dispatchedItemCount = currentItemCount
    }
}
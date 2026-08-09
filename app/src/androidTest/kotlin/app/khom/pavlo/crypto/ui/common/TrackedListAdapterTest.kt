package app.khom.pavlo.crypto.ui.common

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackedListAdapterTest {

    @Test
    fun dispatchesOnlyInsertedRemovedAndRetainedRanges() {
        val adapter = TestAdapter(initialSize = 2)
        val events = mutableListOf<String>()
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                events += "insert:$positionStart:$itemCount"
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                events += "remove:$positionStart:$itemCount"
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                events += "change:$positionStart:$itemCount"
            }
        })

        adapter.updateSize(4)
        adapter.updateSize(1)
        adapter.updateSize(0)

        assertEquals(
            listOf(
                "insert:2:2",
                "change:0:2",
                "remove:1:3",
                "change:0:1",
                "remove:0:1"
            ),
            events
        )
    }

    private class TestAdapter(initialSize: Int) :
        TrackedListAdapter<RecyclerView.ViewHolder>(initialSize) {

        private var size = initialSize

        override fun getItemCount(): Int = size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            error("Not used by this test")

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit

        fun updateSize(newSize: Int) {
            size = newSize
            dispatchTrackedListChanges(size)
        }
    }
}
package app.khom.pavlo.crypto.ui.alerts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.alerts.PriceAlertNotifier
import app.khom.pavlo.crypto.databinding.PriceAlertItemBinding
import app.khom.pavlo.crypto.model.PriceAlert
import java.text.DateFormat
import java.util.Date

class PriceAlertAdapter(
    private val onToggle: (PriceAlert, Boolean) -> Unit,
    private val onDelete: (PriceAlert) -> Unit
) : ListAdapter<PriceAlert, PriceAlertAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(PriceAlertItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: PriceAlertItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alert: PriceAlert) {
            val context = binding.root.context
            binding.alertItemSummary.text = PriceAlertNotifier.summary(context, alert)
            binding.alertItemStatus.text = when {
                alert.enabled -> context.getString(R.string.alert_status_active)
                alert.triggeredAt > 0L -> {
                    val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(alert.triggeredAt))
                    context.getString(R.string.alert_status_triggered, date)
                }
                else -> context.getString(R.string.alert_status_paused)
            }
            // Detach the listener while the state is set programmatically so binding never fires a toggle.
            binding.alertItemSwitch.setOnCheckedChangeListener(null)
            binding.alertItemSwitch.isChecked = alert.enabled
            binding.alertItemSwitch.setOnCheckedChangeListener { _, checked -> onToggle(alert, checked) }
            binding.alertItemDelete.setOnClickListener { onDelete(alert) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<PriceAlert>() {
        override fun areItemsTheSame(oldItem: PriceAlert, newItem: PriceAlert) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PriceAlert, newItem: PriceAlert) = oldItem == newItem
    }
}

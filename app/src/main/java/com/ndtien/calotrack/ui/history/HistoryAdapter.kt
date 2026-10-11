package com.ndtien.calotrack.ui.history

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ndtien.calotrack.R
import com.ndtien.calotrack.databinding.ItemHistoryDayBinding
import com.ndtien.calotrack.util.DateUtils

/**
 * Adapter list chi tiết từng ngày (item_history_day.xml).
 * Mỗi item là Pair(dateIso, totalCalories).
 * [targetCalories] do fragment cập nhật (refresh trong onResume khi user đổi mục tiêu).
 */
class HistoryAdapter(
    private val onDayClick: (String) -> Unit = {}
) : ListAdapter<Pair<String, Double>, HistoryAdapter.HistoryViewHolder>(DIFF) {

    var targetCalories: Int = 2000

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Pair<String, Double>>() {
            override fun areItemsTheSame(
                oldItem: Pair<String, Double>,
                newItem: Pair<String, Double>
            ): Boolean = oldItem.first == newItem.first

            override fun areContentsTheSame(
                oldItem: Pair<String, Double>,
                newItem: Pair<String, Double>
            ): Boolean = oldItem == newItem
        }
    }

    inner class HistoryViewHolder(
        private val binding: ItemHistoryDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Dòng ngày kiểu bản xem thử: khối ngày (thứ + ngày) | tổng + phụ đề | pill trạng thái.
         * Pill: vượt -> nền error-container/chữ error, dưới mục tiêu -> primary-container.
         */
        fun bind(date: String, total: Double) {
            val context = binding.root.context
            binding.tvHistoryDow.text = DateUtils.weekdayShort(date)
            binding.tvHistoryDom.text = DateUtils.dayOfMonth(date)
            binding.tvHistoryTotal.text =
                "${fmtCal(total)} ${context.getString(R.string.unit_kcal)}"
            binding.tvHistorySub.text = if (date == DateUtils.today()) {
                "Hôm nay"
            } else {
                DateUtils.toPill(date)
            }

            val over = total > targetCalories
            val diff = total - targetCalories
            binding.tvHistoryStatus.text = if (over) {
                context.getString(R.string.status_over, fmtCal(diff))
            } else {
                context.getString(R.string.legend_under)
            }
            // Pill đổi màu nền theo trạng thái (bg_tag_pill tint theo code)
            val (bgRes, textRes) = if (over) {
                R.color.proto_error_container to R.color.proto_error
            } else {
                R.color.proto_primary_container to R.color.proto_on_primary_container
            }
            binding.tvHistoryStatus.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, bgRes)
            )
            binding.tvHistoryStatus.setTextColor(ContextCompat.getColor(context, textRes))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryDayBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val (date, total) = getItem(position)
        holder.bind(date, total)
        holder.itemView.setOnClickListener { onDayClick(date) }
    }
}

/** Định dạng calo: bỏ phần thập phân nếu là số nguyên. */
private fun fmtCal(d: Double): String =
    if (d % 1.0 == 0.0) "%.0f".format(d) else "%.1f".format(d)

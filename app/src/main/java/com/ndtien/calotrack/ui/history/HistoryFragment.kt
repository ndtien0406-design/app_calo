package com.ndtien.calotrack.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.ndtien.calotrack.MainActivity
import com.ndtien.calotrack.ui.home.HomeViewModel
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.color.MaterialColors
import com.ndtien.calotrack.R
import com.ndtien.calotrack.data.DayTotal
import com.ndtien.calotrack.databinding.FragmentHistoryBinding
import com.ndtien.calotrack.util.DateUtils
import com.ndtien.calotrack.util.PrefsManager

/**
 * Màn hình "Lịch sử": biểu đồ cột 7 ngày gần nhất + danh sách chi tiết từng ngày.
 * Mục tiêu calo được đọc lại từ PrefsManager trong onResume (user có thể đổi
 * ở màn hình Cài đặt) rồi vẽ lại biểu đồ và trạng thái từng ngày.
 */
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var prefs: PrefsManager
    private lateinit var adapter: HistoryAdapter
    private val homeViewModel: HomeViewModel by activityViewModels()

    /** Bấm một ngày trong danh sách -> chuyển sang Trang chủ và xem ngày đó. */
    private fun openDayInHome(date: String) {
        homeViewModel.setSelectedDateManual(date)
        (activity as? MainActivity)?.showHome()
    }

    private var targetCalories: Int = 2000
    private var lastTotals: List<DayTotal> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PrefsManager(requireContext())
        targetCalories = prefs.targetCalories

        adapter = HistoryAdapter(onDayClick = ::openDayInHome).apply {
            targetCalories = this@HistoryFragment.targetCalories
        }
        binding.rvHistoryDays.adapter = adapter

        setupChart()

        viewModel.weeklyTotals.observe(viewLifecycleOwner) { totals ->
            lastTotals = totals
            updateChart(totals)
            updateTodayLine(totals)
        }

        viewModel.dayTotals.observe(viewLifecycleOwner) { days ->
            binding.tvEmptyHistory.isVisible = days.isEmpty()
            binding.rvHistoryDays.isVisible = days.isNotEmpty()
            adapter.submitList(days.map { it.date to it.total })
        }
    }

    override fun onResume() {
        super.onResume()
        val newTarget = prefs.targetCalories
        if (newTarget != targetCalories) {
            targetCalories = newTarget
            adapter.targetCalories = newTarget
            // Target đổi → trạng thái từng ngày đổi → ép rebind toàn bộ list
            adapter.notifyDataSetChanged()
        }
        updateChart(lastTotals)
    }

    /** Cấu hình khung biểu đồ (trục, legend, animation) — gọi 1 lần. */
    private fun setupChart() {
        val chart = binding.chartWeekly
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setPinchZoom(false)
        chart.setScaleEnabled(false)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                val index = value.toInt()
                val days = viewModel.last7
                return if (index in days.indices) DateUtils.weekdayShort(days[index]) else ""
            }
        }

        val leftAxis = chart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.granularity = 500f
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String =
                fmtCal(value.toDouble())
        }

        chart.axisRight.isEnabled = false

        // Màu chữ / lưới theo theme: MPAndroidChart mặc định vẽ chữ đen -> mất chữ trên nền tối
        val textColor = MaterialColors.getColor(chart, com.google.android.material.R.attr.colorOnSurfaceVariant)
        val gridColor = MaterialColors.getColor(chart, com.google.android.material.R.attr.colorOutlineVariant)
        val axisColor = MaterialColors.getColor(chart, com.google.android.material.R.attr.colorOutline)
        xAxis.textColor = textColor
        xAxis.textSize = 12f
        xAxis.axisLineColor = axisColor
        leftAxis.textColor = textColor
        leftAxis.textSize = 11f
        leftAxis.gridColor = gridColor
        leftAxis.axisLineColor = axisColor
        chart.setNoDataTextColor(textColor)

        // Tắt legend của chart, dùng hàng legend native bên dưới (kiểu bản xem thử)
        chart.legend.isEnabled = false

        chart.animateY(800)
    }

    /** Dòng "Hôm nay: X / 2.000 kcal" trên thẻ biểu đồ (kiểu bản xem thử). */
    private fun updateTodayLine(totals: List<DayTotal>) {
        val b = _binding ?: return
        val today = DateUtils.today()
        val total = totals.find { it.date == today }?.total ?: 0.0
        b.tvChartToday.text = getString(
            R.string.today_summary, fmtCal(total), fmtCal(targetCalories.toDouble())
        )
    }

    /**
     * Vẽ lại dữ liệu biểu đồ: luôn đủ 7 cột (ngày thiếu dữ liệu = 0).
     * Theo bản xem thử: BỎ đường mục tiêu; số kcal trên mỗi cột tô màu
     * đỏ [red_over] nếu vượt mục tiêu, xanh [chart_under] nếu dưới mục tiêu;
     * chú thích gồm 2 mục "Dưới mục tiêu / Vượt mục tiêu".
     */
    private fun updateChart(totals: List<DayTotal>) {
        val context = requireContext()
        val days = viewModel.last7
        val totalByDate = totals.associate { it.date to it.total }

        val colorUnder = ContextCompat.getColor(context, R.color.chart_under)
        val colorOver = ContextCompat.getColor(context, R.color.chart_over)
        // Màu số liệu theo đúng bản xem thử (có bản dark riêng)
        val labelUnder = ContextCompat.getColor(context, R.color.chart_under_label)
        val labelOver = ContextCompat.getColor(context, R.color.chart_over_label)

        val entries = days.mapIndexed { index, date ->
            BarEntry(index.toFloat(), (totalByDate[date] ?: 0.0).toFloat())
        }
        val overFlags = days.map { date -> (totalByDate[date] ?: 0.0) > targetCalories }
        val barColors = overFlags.map { over -> if (over) colorOver else colorUnder }

        val dataSet = BarDataSet(entries, getString(R.string.weekly_chart))
        dataSet.colors = barColors
        dataSet.valueTextSize = 10f
        // Màu số liệu theo từng cột: vượt mục tiêu -> đỏ, dưới -> xanh
        dataSet.setValueTextColors(overFlags.map { over -> if (over) labelOver else labelUnder })
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = fmtCal(value.toDouble())
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.7f
        val chart = binding.chartWeekly
        chart.data = barData
        chart.setFitBars(true)
        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/** Định dạng calo: bỏ phần thập phân nếu là số nguyên. */
private fun fmtCal(d: Double): String =
    if (d % 1.0 == 0.0) "%.0f".format(d) else "%.1f".format(d)

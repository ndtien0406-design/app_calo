package com.ndtien.calotrack.ui.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.ndtien.calotrack.R
import com.ndtien.calotrack.data.AppDatabase
import com.ndtien.calotrack.data.Food
import com.ndtien.calotrack.data.FoodCategories
import com.ndtien.calotrack.databinding.FragmentFoodListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Màn hình "Thực phẩm": tìm kiếm + bộ lọc + danh sách + thêm/sửa/xóa thực phẩm.
 * Lọc (nhóm món, đơn vị khẩu phần, khoảng calo) kết hợp AND với từ khóa tìm kiếm,
 * thực hiện trong bộ nhớ trên danh sách đã tải.
 * Implement [FoodDialogCallback] để refresh sau khi dialog lưu xong.
 */
class FoodListFragment : Fragment(), FoodDialogCallback, FoodFilterSheet.Listener {

    private var _binding: FragmentFoodListBinding? = null
    private val binding get() = _binding!!

    private val foodDao by lazy { AppDatabase.getInstance(requireContext()).foodDao() }
    private lateinit var adapter: FoodAdapter

    private var allFoods: List<Food> = emptyList()
    private var currentQuery: String = ""
    private var foodFilter: FoodFilter = FoodFilter()
    private var quickCategory: String? = null
    private var chipCategories: List<String> = emptyList()
    private var filterBadge: BadgeDrawable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoodListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FoodAdapter(
            onEdit = { food ->
                AddEditFoodDialog.newInstance(food.id)
                    .show(childFragmentManager, "add_edit_food")
            },
            onDelete = { food -> deleteFoodWithUndo(food) }
        )
        binding.rvFoods.adapter = adapter
        // Đường phân cách giữa các dòng kiểu bản xem thử
        binding.rvFoods.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )

        binding.etSearchFood.doOnTextChanged { text, _, _, _ ->
            currentQuery = text?.toString().orEmpty()
            applyFilter()
        }

        binding.btnFilterFoods.setOnClickListener {
            val categories = allFoods.map { it.category }.distinct().sorted()
            val units = allFoods.map { it.servingUnit }.distinct().sorted()
            FoodFilterSheet.newInstance(foodFilter, categories, units)
                .show(childFragmentManager, "filter_foods")
        }

        binding.fabAddFood.setOnClickListener {
            AddEditFoodDialog.newInstance()
                .show(childFragmentManager, "add_edit_food")
        }

        foodDao.getAllFoods().asLiveData().observe(viewLifecycleOwner) { foods ->
            allFoods = foods
            rebuildCategoryChips()
            applyFilter()
        }
    }

    override fun onFilterApplied(filter: FoodFilter) {
        foodFilter = filter
        applyFilter()
        refreshBadge()
    }

    /** Kết hợp từ khóa tìm kiếm + bộ lọc (AND), lọc trong bộ nhớ. */
    private fun applyFilter() {
        val b = _binding ?: return
        var list = allFoods
        val q = currentQuery.trim()
        if (q.isNotEmpty()) {
            list = list.filter { it.name.contains(q, ignoreCase = true) }
        }
        val f = foodFilter
        quickCategory?.let { qc -> list = list.filter { it.category == qc } }
        if (f.categories.isNotEmpty()) list = list.filter { it.category in f.categories }
        if (f.units.isNotEmpty()) list = list.filter { it.servingUnit in f.units }
        if (f.kcalMin != null) list = list.filter { it.caloriesPerServing >= f.kcalMin }
        if (f.kcalMax != null) list = list.filter { it.caloriesPerServing <= f.kcalMax }

        adapter.submitList(list.sortedBy { it.name })
        b.rvFoods.isVisible = list.isNotEmpty()
        b.tvEmptyFoods.isVisible = list.isEmpty()
        if (list.isEmpty()) {
            val filtering = q.isNotEmpty() || activeFilterCount() > 0 || quickCategory != null
            b.tvEmptyFoods.text = getString(
                if (filtering) R.string.no_food_filter_result else R.string.empty_foods
            )
        }
    }

    private fun activeFilterCount(): Int {
        var c = 0
        if (foodFilter.categories.isNotEmpty()) c++
        if (foodFilter.units.isNotEmpty()) c++
        if (foodFilter.kcalMin != null || foodFilter.kcalMax != null) c++
        return c
    }

    private fun refreshBadge() {
        val b = _binding ?: return
        val badge = filterBadge ?: BadgeDrawable.create(requireContext()).also {
            filterBadge = it
            BadgeUtils.attachBadgeDrawable(it, b.btnFilterFoods)
        }
        val count = activeFilterCount()
        badge.number = count
        badge.isVisible = count > 0
    }

    /** Xóa ngay + Snackbar "Hoàn tác" (chèn lại đúng bản ghi, giữ nguyên id). */
    private fun deleteFoodWithUndo(food: Food) {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { foodDao.delete(food) }
            val b = _binding ?: return@launch
            Snackbar.make(b.root, R.string.food_deleted, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        withContext(Dispatchers.IO) { foodDao.insert(food) }
                    }
                }
                .show()
        }
    }

    /** Dựng lại hàng chip nhóm món khi dữ liệu đổi (chỉ khi danh sách nhóm thật sự khác). */
    private fun rebuildCategoryChips() {
        val b = _binding ?: return
        val cats = FoodCategories.ALL + allFoods.map { it.category }
            .filter { it !in FoodCategories.ALL }.distinct().sorted()
        val present = cats.filter { c -> allFoods.any { it.category == c } }
        if (present == chipCategories) return
        chipCategories = present
        if (quickCategory != null && quickCategory !in present) quickCategory = null

        val group = b.chipQuickCategory
        group.setOnCheckedStateChangeListener(null)
        group.removeAllViews()
        fun addChip(label: String, value: String?) {
            group.addView(Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                id = View.generateViewId()
                text = label
                isCheckable = true
                isChecked = value == quickCategory
                tag = value
            })
        }
        addChip(getString(R.string.filter_all), null)
        present.forEach { addChip(it, it) }
        group.setOnCheckedStateChangeListener { g, ids ->
            val chip = ids.firstOrNull()?.let { g.findViewById<Chip>(it) }
            quickCategory = chip?.tag as? String
            applyFilter()
        }
    }

    override fun onFoodSaved() {
        // Flow getAllFoods tự phát lại khi DB đổi -> applyFilter chạy qua observer.
        Snackbar.make(binding.root, R.string.food_saved, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

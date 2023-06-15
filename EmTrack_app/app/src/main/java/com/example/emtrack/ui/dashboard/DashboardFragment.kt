package com.example.emtrack.ui.dashboard

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.emtrack.R
import com.example.emtrack.SavedData
import com.example.emtrack.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF
import com.google.gson.Gson
import java.io.File
import java.io.FileReader

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private lateinit var mTextEmVal: TextView
    private lateinit var mTextTotTime: TextView
    private lateinit var secPerMode: MutableList<Long>
    private lateinit var co2PerMode: MutableList<Float>

    private val MAX_X_VALUE = 8
    private val GROUPS = 2
    private val GROUP_1_LABEL = "Orders"
    private val GROUP_2_LABEL = ""
    private val BAR_SPACE = 0.1f
    private val BAR_WIDTH = 0.8f
    private var chart: BarChart? = null
    private var pieChartEmMode: PieChart? = null
    private var pieChartTimeFull: PieChart? = null
    private var pieChartTimeEmMode: PieChart? = null
    protected var tfRegular: Typeface? = null
    protected var tfLight: Typeface? = null

    private var classes = mutableListOf<String>("Still", "Walking", "Run", "Bike", "Car", "Bus", "Train", "Subway")
    private val emPerMode: List<Float> = listOf(
        0.0f,
        0.0f,
        0.0f,
        0.0f,
        (137.0f / 60.0f),
        (25.0f / 90.0f),
        (24.0f / 36.0f),
        (24.0f / 36.0f)
    ) // TODO: update with better values

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        pieChartEmMode = binding.pieChartEm // this should show emission per class
        pieChartTimeFull = binding.pieChartFull // this should show time in all classes except still
        pieChartTimeEmMode = binding.pieChartEmMode //this should show time in emissive classes like car and bus


        return root
    }

    override fun onResume() {
        super.onResume()

        val gson = Gson()
        val file = File(requireContext().filesDir, "data.json")
        val reader = FileReader(file)

        val jsonString = reader.readText()
        val dataList = gson.fromJson(jsonString, Array<SavedData>::class.java).toList()

        val totCo2 = calculateTotalCO2(dataList, emPerMode)
        Log.d("DashboardFragment", "Total CO2: $totCo2")

        co2PerMode = calculateCO2PerMode(dataList, emPerMode)
        Log.d("DashboardFragment", "CO2 Per mode: $co2PerMode")

        val totSec = calculateTotalSec(dataList)
        Log.d("DashboardFragment", "Total Sec: $totSec")

        secPerMode = calculateSecPerMode(dataList).toMutableList()
        Log.d("DashboardFragment", "secPerMode: $secPerMode")

        mTextEmVal = requireView().findViewById(R.id.text_num_CO2)
        mTextEmVal.text = resources.getString(R.string.em_value, totCo2)

        val co2PerEmMode = co2PerMode.filterIndexed { index, _ -> index > 3 }
        val scaledCo2PerEmMode = scaleAndConvert(co2PerEmMode,1000) // Scale by a thousand to preserve precision when converting to Long
        val classesEmmissive = classes.filterIndexed { index, _ -> index > 3 }
        displayData(scaledCo2PerEmMode, classesEmmissive, pieChartEmMode)

        val secPerModeWOStill = secPerMode.filterIndexed { index, _ -> index != 0 }
        val classesWOStill = classes.filterIndexed { index, _ -> index != 0 }
        displayData(secPerModeWOStill, classesWOStill, pieChartTimeFull)

        val secPerModeEmissive = secPerMode.filterIndexed { index, _ -> index > 3}
        displayData(secPerModeEmissive, classesEmmissive, pieChartTimeEmMode)

        mTextTotTime = requireView().findViewById(R.id.statsTime)
        mTextTotTime.text = resources.getString(R.string.total_time, totSec/(60*60*24), totSec%(60*60*24)/(60*60), totSec%(60*60)/60)
    }

    // Calculate accumulated CO2 emission from time per mode in dataList
    private fun calculateTotalCO2(dataList: List<SavedData>, emList: List<Float>): Float {
        var co2 = 0f
        for (i in dataList.indices) {
            co2 += dataList[i].secPerMode * emList[i]
        }
        return co2
    }

    private fun calculateCO2PerMode(dataList: List<SavedData>, emList: List<Float>): MutableList<Float>{
        var co2PerMode = MutableList(dataList.size) {0f}
        for (i in dataList.indices) {
            co2PerMode[i] = dataList[i].secPerMode * emPerMode[i]
        }
        return co2PerMode
    }
    private fun calculateTotalSec(dataList: List<SavedData>): Long {
        var totalSec = 0L
        for (i in dataList.indices) {
            totalSec += dataList[i].secPerMode
        }
        return totalSec
    }
    private fun calculateSecPerMode(dataList: List<SavedData>): MutableList<Long> {
        var returnList: MutableList<Long> = MutableList (dataList.size) {0}
        for (i in dataList.indices) {
            returnList[i] = dataList[i].secPerMode
        }
        return returnList
    }
    private fun scaleAndConvert(list: List<Float>, scaleFactor: Int): List<Long> {
        var returnList: MutableList<Long> = MutableList (list.size) {0}
        for (i in list.indices) {
            returnList[i] = (list[i] * scaleFactor).toLong()
        }
        return returnList.toList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun setPieChartData(emData: List<Long>, labels: List<String>, pChart: PieChart?) {
        val entries: ArrayList<PieEntry> = ArrayList()

        var secSum = 0L
        for (i in emData.indices) {
            secSum += emData[i]
        }

        // NOTE: The order of the entries when being added to the entries array determines their position around the center of
        // the chart.
        val colors: ArrayList<Int> = ArrayList()
        colors.add(ColorTemplate.rgb(getString(R.color.poor_yellow)))
        colors.add(ColorTemplate.rgb(getString(R.color.soft_green)))
        colors.add(ColorTemplate.rgb(getString(R.color.sophisticated_blue)))
        colors.add(ColorTemplate.rgb(getString(R.color.rich_blue)))
        colors.add(ColorTemplate.rgb(getString(R.color.light_soft_green)))
        colors.add(ColorTemplate.rgb(getString(R.color.contrast_blue)))
        colors.add(ColorTemplate.rgb(getString(R.color.dark_sophisticated_blue)))
        colors.add(ColorTemplate.rgb(getString(R.color.light_poor_yellow)))
        for (i in emData.indices){
            try {
                if(emData[i] == 0L) continue
                entries.add(
                    PieEntry(
                        emData[i].toFloat(),
                        labels[i]
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.setDrawIcons(false)
        dataSet.sliceSpace = 3f
        dataSet.iconsOffset = MPPointF(0F, 40F)
        dataSet.selectionShift = 10f


        //dataSet.colors = colors
        //dataSet.setSelectionShift(0f);
        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter())
        data.setValueTextSize(11f)
        data.setValueTextColor(Color.WHITE)
        data.setValueTypeface(tfLight)
        data.setDrawValues(false)
        pChart!!.data = data

        pChart.setDrawHoleEnabled(false)
        pChart.description.isEnabled = false
        pChart.animateY(1000, Easing.EaseInOutQuad)

        // undo all highlights
        pChart.highlightValues(null)

        // Draw chart
        pChart.invalidate()
    }
    private fun displayData(emData: List<Long>, labels: List<String>, pChart: PieChart?){
        setPieChartData(emData, labels, pChart)
    }
}
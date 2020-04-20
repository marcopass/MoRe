package com.example.moneyrecordv3;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SliceValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.view.ColumnChartView;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PieChartView;

public class FragmentSummary extends Fragment {

    View view;
    private SharedViewModel sharedVM; // shared view model to pass data between fragments
    myDbAdapter helper; // helper to query the transaction database

    // Balance object definitions
    TextView balanceMonthTitleTV, balanceMonthTV, balanceYearTitleTV, balanceYearTV, balanceTotalTV;

    // Month summary object definitions
    Spinner monthSP, yearSP;
    TableLayout monthSummaryTL;
    PieChartView monthPieChart;
    TextView monthSummaryCashTV, monthSummaryCardTV, monthSummaryAverageTV;
    String[] categories = {"", "", "", "", "", ""}; // array of categories that will be shown in month summary
    String[] specialCategories = {"Stipendio", "Affitto", "Bollette"}; // array of categories that will be shown in column chart for special transactions

    // Month trend object definitions
    TableLayout monthTrendTL;
    Button scrollLeftBT, scrollRightBT;
    ColumnChartView monthTrendColumnChart, monthTrendSpecialColumnChart;
    int monthTrendStart = 0; // 0 = last month in column chart is current month, this variable is modified using scroll left and right buttons

    // Food trend object definitions
    ColumnChartView foodTrendColumnChart;
    Button scrollLeftBT2, scrollRightBT2;

    // Custom trend object definitions
    ColumnChartView customTrendColumnChart;
    Button scrollLeftBT3, scrollRightBT3;
    Spinner categorySP;

    public FragmentSummary() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.summary_fragment, container, false);

        sharedVM = ViewModelProviders.of(getActivity()).get(SharedViewModel.class);
        sharedVM.getPressed().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                String year = String.valueOf(yearSP.getSelectedItemPosition() + currentYear);
                String month = String.format("%02d", monthSP.getSelectedItemPosition() + 1);
                displayMonthlySummary(year, month);
            }
        });

        helper = new myDbAdapter(getContext()); // create new helper to query database

        // Balance
        balanceMonthTitleTV = view.findViewById(R.id.balance_month_title_text_view);
        balanceMonthTV = view.findViewById(R.id.balance_month_text_view);
        balanceYearTitleTV = view.findViewById(R.id.balance_year_title_text_view);
        balanceYearTV = view.findViewById(R.id.balance_year_text_view);
        balanceTotalTV = view.findViewById(R.id.balance_total_text_view);

        // Month Summary Pie Chart
        monthSP = view.findViewById(R.id.month_spinner);
        yearSP = view.findViewById(R.id.year_spinner);
        monthSummaryTL = view.findViewById(R.id.month_summary_table_layout);
        monthPieChart = view.findViewById(R.id.month_summary_pie_chart);
        monthSummaryCashTV = view.findViewById(R.id.cash_percentage_text_view);
        monthSummaryCardTV = view.findViewById(R.id.card_percentage_text_view);
        monthSummaryAverageTV = view.findViewById(R.id.day_average_text_view);

        // Month Trend Column Chart
        monthTrendTL = view.findViewById(R.id.month_trend_table_layout);
        monthTrendColumnChart = view.findViewById(R.id.month_trend_column_chart);
        monthTrendSpecialColumnChart = view.findViewById(R.id.month_trend_column_chart_special);
        scrollLeftBT = view.findViewById(R.id.scroll_left_button);
        scrollRightBT = view.findViewById(R.id.scroll_right_button);

        // Food Trend Column Chart
        foodTrendColumnChart = view.findViewById(R.id.food_trend_column_chart);
        scrollLeftBT2 = view.findViewById(R.id.scroll_left_button_2);
        scrollRightBT2 = view.findViewById(R.id.scroll_right_button_2);

        // Custom Trend Column Chart
        customTrendColumnChart = view.findViewById(R.id.custom_trend_column_chart);
        scrollLeftBT3 = view.findViewById(R.id.scroll_left_button_3);
        scrollRightBT3 = view.findViewById(R.id.scroll_right_button_3);
        categorySP = view.findViewById(R.id.cateogry_spinner);

        // Get current year and month
        Calendar calendar = Calendar.getInstance();
        final int month = calendar.get(Calendar.MONTH);
        final int year = calendar.get(Calendar.YEAR);

        // Init and set array adapter for month spinner
        List<String> monthList = new ArrayList<>(Arrays.asList(Config.MONTH_ARRAY));
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.spinner_item, monthList);
        arrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        monthSP.setAdapter(arrayAdapter);
        monthSP.setSelection(month);

        // Init and set array adapter for year spinner
        String[] yearArray = new String[year-2019+1];
        for (int y = 0; y < yearArray.length; y++) {
            yearArray[y] = String.valueOf(year-y).substring(2,4);
        }
        List<String> yearList = new ArrayList<>(Arrays.asList(yearArray));
        ArrayAdapter<String> arrayAdapter2 = new ArrayAdapter<>(getContext(), R.layout.spinner_item, yearList);
        arrayAdapter2.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        yearSP.setAdapter(arrayAdapter2);
        yearSP.setSelection(0);

        // Init and set array adapter for category spinner
        String[] categories = helper.getAllCategories().replace("_", " ").split("\n");
        List<String> categoryList = new ArrayList<>(Arrays.asList(categories));
        ArrayAdapter<String> arrayAdapter3 = new ArrayAdapter<>(getContext(), R.layout.spinner_item, categoryList);
        arrayAdapter3.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        categorySP.setAdapter(arrayAdapter3);
        categorySP.setSelection(0);

        monthSP.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                /* onItemSelected is called before and after the view is inflated.
                This causes onItemSelected to be called twice when switching between fragments.
                In order to solve this problem, run user commands only after the view has been inflated.
                 */
                if (view != null) {
                    String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR) - yearSP.getSelectedItemPosition());
                    displayMonthlySummary(year, String.format("%02d", position + 1));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        yearSP.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view != null) {
                    String month = String.format("%02d", monthSP.getSelectedItemPosition() + 1);
                    int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                    displayMonthlySummary(String.valueOf(currentYear - position), month);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        categorySP.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view != null) {
                    String[] categories = helper.getAllCategories().replace("_", " ").split("\n");
                    displayCustomTrend(monthTrendStart, categories[position]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        scrollLeftBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollLeft();
            }
        });
        scrollRightBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollRight();
            }
        });
        scrollLeftBT2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollLeft();
            }
        });
        scrollRightBT2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollRight();
            }
        });
        scrollLeftBT3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollLeft();
            }
        });
        scrollRightBT3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollRight();
            }
        });

        return view;
    }

    private void displayBalance(String year, String month) {
        // MONTH Balance
        balanceMonthTitleTV.setText(Config.inverseMap(month) + " " + year.substring(2,4));
        String[] TPMPC = helper.getTotalPerMonthPerCategory(year, month, true).split("\n");
        float revenueMonth = 0f;
        for (int cat = 0; cat < TPMPC.length; cat++) {
            if (TPMPC[cat].contains("Stipendio")) {
                revenueMonth = Math.abs(Float.valueOf((TPMPC[cat].split(" "))[1]));
                break;
            }
        }
        float profitMonth = -Float.valueOf((TPMPC[0].split(" "))[1]);
        float expenseMonth = revenueMonth - profitMonth;

        String outputString = String.format(Locale.ENGLISH, "%.0f", revenueMonth) + "€ - "
                + String.format(Locale.ENGLISH, "%.0f", expenseMonth) + "€ = "
                + String.format(Locale.ENGLISH, "%.0f", profitMonth) + "€"
                + " (" + String.format(Locale.ENGLISH, "%.0f", (expenseMonth + profitMonth != 0) ? (profitMonth / revenueMonth * 100) : 0) + "%)";

        Spannable spannable = new SpannableString(outputString);
        int firstOcc = outputString.indexOf("€");
        int secondOcc = outputString.indexOf("€", firstOcc+1);
        spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorPrimary)), 0, firstOcc+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), outputString.indexOf("-")+2, secondOcc+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(getResources().getColor((profitMonth > 0) ? R.color.colorPrimary : R.color.colorAccent)), outputString.indexOf("=")+2, outputString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), outputString.indexOf("=")+2, outputString.indexOf("(")-1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        balanceMonthTV.setText(spannable, TextView.BufferType.SPANNABLE);

        // YEAR Balance
        balanceYearTitleTV.setText(year);
        String[] TARY = helper.getTotalAndRevenueYear(year).split("\n");
        float revenueYear = 0f;
        for (int cat = 0; cat < TARY.length; cat++) {
            if (TARY[cat].contains("Stipendio")) {
                revenueYear = Math.abs(Float.valueOf((TARY[cat].split(" "))[1]));
                break;
            }
        }
        float profitYear = -Float.valueOf((TARY[0].split(" "))[1]);
        float expenseYear = revenueYear - profitYear;

        outputString = String.format(Locale.ENGLISH, "%.0f", revenueYear) + "€ - "
                + String.format(Locale.ENGLISH, "%.0f", expenseYear) + "€ = "
                + String.format(Locale.ENGLISH, "%.0f", profitYear) + "€"
                + " (" + String.format(Locale.ENGLISH, "%.0f", profitYear / revenueYear * 100) + "%)";

        spannable = new SpannableString(outputString);
        firstOcc = outputString.indexOf("€");
        secondOcc = outputString.indexOf("€", firstOcc+1);
        spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorPrimary)), 0, firstOcc+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), outputString.indexOf("-")+2, secondOcc+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(getResources().getColor((profitYear > 0) ? R.color.colorPrimary : R.color.colorAccent)), outputString.indexOf("=")+2, outputString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), outputString.indexOf("=")+2, outputString.indexOf("(")-1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        balanceYearTV.setText(spannable, TextView.BufferType.SPANNABLE);

        // TOTAL Balance
        String[] TARA = helper.getTotalAndRevenueAll().split("\n");
        float revenueTotal = 0f;
        for (int cat = 0; cat < TARA.length; cat++) {
            if (TARA[cat].contains("Stipendio")) {
                revenueTotal = Math.abs(Float.valueOf((TARA[cat].split(" "))[1]));
                break;
            }
        }
        float profitTotal = -Float.valueOf((TARA[0].split(" "))[1]);
        float expenseTotal = revenueTotal - profitTotal;

        outputString = String.format(Locale.ENGLISH, "%.0f", revenueTotal) + "€ - "
                + String.format(Locale.ENGLISH, "%.0f", expenseTotal) + "€ = "
                + String.format(Locale.ENGLISH, "%.0f", profitTotal) + "€"
                + " (" + String.format(Locale.ENGLISH, "%.0f", profitTotal / revenueTotal * 100) + "%)";

        spannable = new SpannableString(outputString);
        firstOcc = outputString.indexOf("€");
        secondOcc = outputString.indexOf("€", firstOcc+1);
        spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorPrimary)), 0, firstOcc+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), outputString.indexOf("-")+2, secondOcc+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(getResources().getColor((profitTotal > 0) ? R.color.colorPrimary : R.color.colorAccent)), outputString.indexOf("=")+2, outputString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), outputString.indexOf("=")+2, outputString.indexOf("(")-1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        balanceTotalTV.setText(spannable, TextView.BufferType.SPANNABLE);
    }

    private void displayMonthlySummary(String year, String month) {
        monthSummaryTL.removeAllViews(); // clean table layout

        String[] TPMPC = helper.getTotalPerMonthPerCategory(year, month, false).split("\n"); // get TOTAL PER MONTH PER CATEGORY from db
        Float TPM = Float.valueOf((TPMPC[0].split(" "))[1]);
        List<SliceValue> sliceValues = new ArrayList<>(); // define list for pie chart slices

        float density = getContext().getResources().getDisplayMetrics().density;
        TableRow row;
        TableRow.LayoutParams lp;
        int textColor;
        TextView subcategoryTV;
        TextView valueTV;
        TextView legendTV;

        categories = new String[] {"","","","","",""};

        if (TPM == 0) {
            // If no data is present (i.e. first day of month) draw a gray empty piechart
            SliceValue sliceValue = new SliceValue(100, Color.parseColor("#a6a6a6"));
            sliceValue.setLabel("No Data Yet");
            sliceValues.add(sliceValue);
            monthSummaryCashTV.setText("");
            monthSummaryCardTV.setText("");
            monthSummaryAverageTV.setText("");
        } else {
            // Insert table rows for other categories and generate pie chart data
            Float valueAltro = 0f;
            for (int c = 0; c < 7; c++) {
                if (c >= TPMPC.length) {
                    break;
                }
                String[] fields = TPMPC[c].split(" ");
                String category = "";
                Float value = 0f;
                Float percentage = 0f;
                if (c < 6) {
                    category = fields[0];
                    value = Float.valueOf(fields[1]);
                    percentage = value / TPM * 100;

                    valueAltro += value;
                } else {
                    category = "Altro";
                    value = 2*TPM - valueAltro;
                    percentage = value / TPM * 100;
                }

                row = new TableRow(getContext());
                lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
                row.setLayoutParams(lp);
                if (c % 2 == 0) {
                    textColor = Color.parseColor("#000000");
                } else {
                    textColor = Color.parseColor("#666666");
                }

                subcategoryTV = new TextView(getContext());
                subcategoryTV.setWidth((int) (Config.COL_SUBCATEGORY_WIDTH_DP * density));
                subcategoryTV.setText(category.replace("_", " "));
                subcategoryTV.setTextSize(Config.TEXT_SIZE);
                subcategoryTV.setTextColor(textColor);
                subcategoryTV.setEllipsize(TextUtils.TruncateAt.END);
                subcategoryTV.setMaxLines(1);

                valueTV = new TextView(getContext());
                valueTV.setWidth((int) (Config.COL_VALUE_WIDTH_DP * density));
                valueTV.setText(String.format(Locale.ENGLISH,"%.2f", value) + "€");
                valueTV.setTextSize(Config.TEXT_SIZE);
                valueTV.setTextColor(textColor);
                valueTV.setGravity(Gravity.RIGHT);
                valueTV.setTypeface(Typeface.DEFAULT_BOLD);
                valueTV.setPadding(0, 0, 10, 0);

                legendTV = new TextView(getContext());
                legendTV.setWidth((int) (Config.TEXT_SIZE * density * 1.0));
                legendTV.setHeight((int) (Config.TEXT_SIZE * density * 1.0));
                if (c > 0 ) { legendTV.setBackgroundColor(Color.parseColor(Config.MY_CHART_COLORS[c-1])); }

                row.addView(subcategoryTV);
                row.addView(valueTV);
                row.addView(legendTV);
                monthSummaryTL.addView(row, c);

                // Generate Pie Chart Data
                if (c > 0 && value > 0) {
                    categories[c-1] = category;
                    SliceValue sliceValue = new SliceValue(value, Color.parseColor(Config.MY_CHART_COLORS[c - 1]));
                    if (percentage > 12) {
                        sliceValue.setLabel(String.format("%.1f", percentage) + "%");
                    } else {
                        sliceValue.setLabel("");
                    }
                    sliceValues.add(sliceValue);
                }
            }

            // get total per month per method
            String[] TPMPM = helper.getTotalPerMonthPerMethod(year, month).split("\n"); // get TOTAL PER MONTH PER METHOD from db
            String cashValue = "";
            String cardValue = "";
            if (TPMPM.length > 1) {
                cashValue = String.format(Locale.ENGLISH, "%.0f", (Float.valueOf((TPMPM[0].split(" "))[1]) / TPM * 100)) + "%";
                cardValue = String.format(Locale.ENGLISH, "%.0f", (Float.valueOf((TPMPM[1].split(" "))[1]) / TPM * 100)) + "%";
            } else {
                switch ((TPMPM[0].split(" "))[0]) {
                    case "Cash":
                        cashValue = "100%";
                        cardValue = "0%";
                        break;
                    case "Card":
                        cashValue = "0%";
                        cardValue = "100%";
                        break;
                    default:
                        break;
                }
            }
            monthSummaryCashTV.setText(cashValue);
            monthSummaryCardTV.setText(cardValue);

            // compute daily average
            Calendar calendar = Calendar.getInstance(); // current date
            YearMonth yearMonth = YearMonth.of(calendar.get(Calendar.YEAR), Integer.valueOf(month)); // current year, month selected by user
            String averageValue = "";
            if (calendar.get(Calendar.MONTH) + 1 == Integer.valueOf(month)) {
                averageValue = String.format(Locale.ENGLISH, "%.2f", TPM / calendar.get(Calendar.DAY_OF_MONTH)) + "€";
            } else {
                averageValue = String.format(Locale.ENGLISH, "%.2f", TPM / yearMonth.lengthOfMonth()) + "€";
            }
            monthSummaryAverageTV.setText(averageValue);
        }

        displayMonthTrend(monthTrendStart, categories);
        displayMonthTrendSpecial(monthTrendStart, specialCategories);
        displayBalance(year, month);

        // Display Pie Chart
        PieChartData pieChartData = new PieChartData(sliceValues);
        pieChartData.setHasLabels(true);
        pieChartData.setHasCenterCircle(false);
        monthPieChart.setCircleFillRatio(1.0f);
        monthPieChart.setPieChartData(pieChartData);
        monthPieChart.setInteractive(false);
    }

    private void displayMonthTrend(int start, String[] categories) {
        //monthTrendTL.removeAllViews();

        List<Column> columns = new ArrayList<>();
        List<SubcolumnValue> values;
        int numberOfColumns = 12;
        List<AxisValue> axisValuesXBottom = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH) + 1; // range 1 - 12
        int currentYear = calendar.get(Calendar.YEAR);
        int firstMonth = (currentMonth - start - 11) % 12 + 12;
        int firstYear = currentYear;

        if (currentMonth - start - 12 < 0) {
            firstYear = currentYear - 1 - (12 - currentMonth + start - 1)/12;
        }
        String year = String.valueOf(firstYear);

        for (int col = 0; col < numberOfColumns; col++) {
            values = new ArrayList<SubcolumnValue>();

            // compute month and year
            String month = String.format("%02d", (firstMonth+col-1)%12+1);
            int m1 = (firstMonth+col-2)%12+1;
            int m2 = (firstMonth+col-1)%12+1;

            if (m2 - m1 == -11) {
                year = String.valueOf(firstYear+1);
            }

            String[] TPMPC = helper.getTotalPerMonthPerCategory(year, month, false).split("\n");
            Float TPM = Float.valueOf((TPMPC[0].split(" "))[1]);

            float lastValue = TPM;

            for (int cat = 0; cat < categories.length; cat++) {
                float subcolumnValue = 0f;
                if (!categories[cat].isEmpty()) {
                    for (int c = 0; c < TPMPC.length; c++) {
                        if (TPMPC[c].contains(categories[cat])) {
                            subcolumnValue = Float.valueOf((TPMPC[c].split(" "))[1]);
                            break;
                        }
                    }
                    values.add(new SubcolumnValue(subcolumnValue, Color.parseColor(Config.MY_CHART_COLORS[cat])));
                    lastValue -= subcolumnValue;
                }
            }

            values.add(new SubcolumnValue(lastValue, Color.parseColor("#a6a6a6")));

            axisValuesXBottom.add(new AxisValue(col, (Config.inverseMap(month) + " " + year.substring(2,4)).toCharArray()));

            Column column = new Column(values);
            columns.add(column);

        }

        ColumnChartData columnChartData = new ColumnChartData(columns);
        columnChartData.setStacked(true);

        Axis axisXBottom = new Axis(axisValuesXBottom);
        axisXBottom.setHasTiltedLabels(true);
        axisXBottom.setMaxLabelChars(4);
        axisXBottom.setTextSize(10);
        axisXBottom.setName("Month");
        axisXBottom.setTextColor(getResources().getColor(R.color.colorText));
        columnChartData.setAxisXBottom(axisXBottom);

        Axis axisYLeft = new Axis().setHasLines(true);
        axisYLeft.setTextSize(10);
        axisYLeft.setName("Amount");
        axisYLeft.setTextColor(getResources().getColor(R.color.colorText));
        axisYLeft.setLineColor(getResources().getColor(R.color.colorText));
        columnChartData.setAxisYLeft(axisYLeft);

        monthTrendColumnChart.setColumnChartData(columnChartData);
        monthTrendColumnChart.setInteractive(false);
    }

    private void displayMonthTrendSpecial(int start, String[] categories) {
        //monthTrendTL.removeAllViews();

        List<Column> columns = new ArrayList<>();
        List<SubcolumnValue> values;
        int numberOfColumns = 12;
        List<AxisValue> axisValuesXBottom = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH) + 1; // range 1 - 12
        int currentYear = calendar.get(Calendar.YEAR);
        int firstMonth = (currentMonth - start - 11) % 12 + 12;
        int firstYear = currentYear;

        if (currentMonth - start - 12 < 0) {
            firstYear = currentYear - 1 - (12 - currentMonth + start - 1)/12;
        }
        String year = String.valueOf(firstYear);

        for (int col = 0; col < numberOfColumns; col++) {
            values = new ArrayList<SubcolumnValue>();

            // compute month and year
            String month = String.format("%02d", (firstMonth+col-1)%12+1);
            int m1 = (firstMonth+col-2)%12+1;
            int m2 = (firstMonth+col-1)%12+1;

            if (m2 - m1 == -11) {
                year = String.valueOf(firstYear+1);
            }

            String[] TPMPC = helper.getTotalPerMonthPerCategory(year, month, true).split("\n");
            Float TPM = Float.valueOf((TPMPC[0].split(" "))[1]);

            float lastValue = TPM;

            for (int cat = 0; cat < categories.length; cat++) {
                float subcolumnValue = 0f;
                if (!categories[cat].isEmpty()) {
                    for (int c = 0; c < TPMPC.length; c++) {
                        if (TPMPC[c].contains(categories[cat])) {
                            subcolumnValue = Float.valueOf((TPMPC[c].split(" "))[1]);
                            break;
                        }
                    }
                    values.add(new SubcolumnValue(subcolumnValue, Color.parseColor(Config.MY_CHART_COLORS_2[cat])));
                    lastValue -= subcolumnValue;
                }
            }

            values.add(new SubcolumnValue(lastValue, Color.parseColor("#a6a6a6")));

            axisValuesXBottom.add(new AxisValue(col, (Config.inverseMap(month) + " " + year.substring(2,4)).toCharArray()));

            Column column = new Column(values);
            columns.add(column);

        }

        ColumnChartData columnChartData = new ColumnChartData(columns);
        columnChartData.setStacked(true);

        Axis axisXBottom = new Axis(axisValuesXBottom);
        axisXBottom.setHasTiltedLabels(true);
        axisXBottom.setMaxLabelChars(4);
        axisXBottom.setTextSize(10);
        axisXBottom.setName("Month");
        axisXBottom.setTextColor(getResources().getColor(R.color.colorText));
        columnChartData.setAxisXBottom(axisXBottom);

        Axis axisYLeft = new Axis().setHasLines(true);
        axisYLeft.setTextSize(10);
        axisYLeft.setName("Amount");
        axisYLeft.setTextColor(getResources().getColor(R.color.colorText));
        axisYLeft.setLineColor(getResources().getColor(R.color.colorText));
        columnChartData.setAxisYLeft(axisYLeft);

        monthTrendSpecialColumnChart.setColumnChartData(columnChartData);
        monthTrendSpecialColumnChart.setInteractive(false);
    }

    private void displayFoodTrend(int start) {
        List<Column> columns = new ArrayList<>();
        List<SubcolumnValue> values;
        int numberOfColumns = 12;
        List<AxisValue> axisValuesXBottom = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH) + 1; // range 1 - 12
        int currentYear = calendar.get(Calendar.YEAR);
        int firstMonth = (currentMonth - start - 11) % 12 + 12;
        int firstYear = currentYear;

        if (currentMonth - start - 12 < 0) {
            firstYear = currentYear - 1 - (12 - currentMonth + start - 1)/12;
        }
        String year = String.valueOf(firstYear);

        for (int col = 0; col < numberOfColumns; col++) {
            values = new ArrayList<SubcolumnValue>();

            // compute month and year
            String month = String.format("%02d", (firstMonth+col-1)%12+1);
            int m1 = (firstMonth+col-2)%12+1;
            int m2 = (firstMonth+col-1)%12+1;

            if (m2 - m1 == -11) {
                year = String.valueOf(firstYear+1);
            }

            String[] TPMPC = helper.getTotalPerMonthPerCategory(year, month, true).split("\n");
            String[] subcategories1 = {"Mercato", "Eurospin", "Carrefour", "Coop", "Pam"};
            String[] subcategories2 = {"caffé", "pizza", "giapponese", "kebab"};

            axisValuesXBottom.add(new AxisValue(col, (Config.inverseMap(month) + " " + year.substring(2,4)).toCharArray()));

            Column column = new Column(values);
            columns.add(column);

        }

        ColumnChartData columnChartData = new ColumnChartData(columns);
        columnChartData.setStacked(true);

        Axis axisXBottom = new Axis(axisValuesXBottom);
        axisXBottom.setHasTiltedLabels(true);
        axisXBottom.setMaxLabelChars(4);
        axisXBottom.setTextSize(10);
        axisXBottom.setName("Month");
        axisXBottom.setTextColor(getResources().getColor(R.color.colorText));
        columnChartData.setAxisXBottom(axisXBottom);

        Axis axisYLeft = new Axis().setHasLines(true);
        axisYLeft.setTextSize(10);
        axisYLeft.setName("Amount");
        axisYLeft.setTextColor(getResources().getColor(R.color.colorText));
        axisYLeft.setLineColor(getResources().getColor(R.color.colorText));
        columnChartData.setAxisYLeft(axisYLeft);

        foodTrendColumnChart.setColumnChartData(columnChartData);
        foodTrendColumnChart.setInteractive(false);
    }

    private void displayCustomTrend(int start, String category) {
        category = category.replace(" ", "_");

        List<Column> columns = new ArrayList<>();
        List<SubcolumnValue> values;
        int numberOfColumns = 12;
        List<AxisValue> axisValuesXBottom = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH) + 1; // range 1 - 12
        int currentYear = calendar.get(Calendar.YEAR);
        int firstMonth = (currentMonth - start - 11) % 12 + 12;
        int firstYear = currentYear;

        if (currentMonth - start - 12 < 0) {
            firstYear = currentYear - 1 - (12 - currentMonth + start - 1)/12;
        }
        String year = String.valueOf(firstYear);

        for (int col = 0; col < numberOfColumns; col++) {
            values = new ArrayList<SubcolumnValue>();

            // compute month and year
            String month = String.format("%02d", (firstMonth+col-1)%12+1);
            int m1 = (firstMonth+col-2)%12+1;
            int m2 = (firstMonth+col-1)%12+1;

            if (m2 - m1 == -11) {
                year = String.valueOf(firstYear+1);
            }

            float value = 0f;

            String[] TPMPC = helper.getTotalPerMonthPerCategory(year, month, true).split("\n");
            for (int cat = 0; cat < TPMPC.length; cat++) {
                if ((TPMPC[cat].split(" ")[0]).contains(category)) {
                    value = Float.valueOf((TPMPC[cat].split(" "))[1]);
                }
            }

            values.add(new SubcolumnValue(value, Color.parseColor(Config.MY_CHART_COLORS[0])));

            axisValuesXBottom.add(new AxisValue(col, (Config.inverseMap(month) + " " + year.substring(2,4)).toCharArray()));

            Column column = new Column(values);
            columns.add(column);

        }

        ColumnChartData columnChartData = new ColumnChartData(columns);
        columnChartData.setStacked(true);

        Axis axisXBottom = new Axis(axisValuesXBottom);
        axisXBottom.setHasTiltedLabels(true);
        axisXBottom.setMaxLabelChars(4);
        axisXBottom.setTextSize(10);
        axisXBottom.setName("Month");
        axisXBottom.setTextColor(getResources().getColor(R.color.colorText));
        columnChartData.setAxisXBottom(axisXBottom);

        Axis axisYLeft = new Axis().setHasLines(true);
        axisYLeft.setTextSize(10);
        axisYLeft.setName("Amount");
        axisYLeft.setTextColor(getResources().getColor(R.color.colorText));
        axisYLeft.setLineColor(getResources().getColor(R.color.colorText));
        columnChartData.setAxisYLeft(axisYLeft);

        customTrendColumnChart.setColumnChartData(columnChartData);
        customTrendColumnChart.setInteractive(false);
    }

    private void scrollLeft() {
        monthTrendStart++;
        updateDisplay();
    }

    private void scrollRight() {
        if (monthTrendStart > 0) {
            monthTrendStart--;
        } else {
            Toast.makeText(getContext(), "End reached!", Toast.LENGTH_SHORT).show();
        }
        updateDisplay();
    }

    private void updateDisplay() {
        displayMonthTrend(monthTrendStart, categories);
        displayMonthTrendSpecial(monthTrendStart, specialCategories);
        displayFoodTrend(monthTrendStart);
        displayCustomTrend(monthTrendStart, (helper.getAllCategories().split("\n"))[categorySP.getSelectedItemPosition()]);
    }
}



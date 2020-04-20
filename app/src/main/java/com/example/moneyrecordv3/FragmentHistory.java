package com.example.moneyrecordv3;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class FragmentHistory extends Fragment {

    View view;
    private SharedViewModel sharedVM;

    TableLayout tableLayout;
    TextView historyTitleTV;
    TextView maxReachedTV;
    LinearLayout historyTitleLL;

    Button searchTransBT;

    float xPos, yPos;

    myDbAdapter helper;

    final static int DEFAULT_AMPLITUDE = 100;

    public FragmentHistory() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.history_fragment, container, false);

        sharedVM = ViewModelProviders.of(getActivity()).get(SharedViewModel.class);
        sharedVM.getPressed().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean pressed) {
                showTransactions(helper.getDataOrderedByDate(), 0, DEFAULT_AMPLITUDE);
            }
        });

        tableLayout = view.findViewById(R.id.table_layout_history);
        historyTitleTV = view.findViewById(R.id.text_history_title);
        historyTitleLL = view.findViewById(R.id.layout_history_title);

        searchTransBT = view.findViewById(R.id.search_trans_button);

        helper = new myDbAdapter(getContext());

        showTransactions(helper.getDataOrderedByDate(), 0, DEFAULT_AMPLITUDE);
        historyTitleTV.setText(helper.getDataOrderedByDate().split("\n").length + " transactions in DB");

        searchTransBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.popup_window_search, null);

                // Get display dimensions
                WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                Display display = wm.getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int displayWidth = size.x;
                int displayHeight = size.y;

                int width = Math.round(displayWidth * 0.8f);
                int height = LinearLayout.LayoutParams.WRAP_CONTENT;
                boolean focusable = true;
                final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);
                popupWindow.setElevation(20);

                final EditText amountET = popupView.findViewById(R.id.search_amount_edit_text);
                final EditText dateET = popupView.findViewById(R.id.search_date_edit_text);
                final RadioButton cashRB = popupView.findViewById(R.id.search_cash_radio_button);
                final RadioButton cardRB = popupView.findViewById(R.id.search_card_radio_button);
                final EditText categoryET = popupView.findViewById(R.id.search_category_edit_text);
                final EditText commentsET = popupView.findViewById(R.id.search_comments_edit_text);
                Button submitSearchBT = popupView.findViewById(R.id.submit_search_button);

                submitSearchBT.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String amount = amountET.getText().toString();
                        String date = dateET.getText().toString();
                        String method = cashRB.isChecked() ? (cardRB.isChecked() ? "Both" : "Cash") : (cardRB.isChecked() ? "Card" : "Both");
                        String category = categoryET.getText().toString();
                        String comments = commentsET.getText().toString();

                        String searchResultsRaw = helper.customSearch(amount, date, method, category, comments);

                        String[] quickSummary = searchResultsRaw.substring(0, searchResultsRaw.indexOf("\n")).split(" ");
                        String count = quickSummary[1];
                        String sum = quickSummary[2];
                        String avg = quickSummary[3];

                        showTransactions(searchResultsRaw.substring(searchResultsRaw.indexOf("\n")+1), 0, -1);

                        LinearLayout quickSummaryLL = view.findViewById(R.id.quick_summary_layout);
                        TextView foundTransTV = view.findViewById(R.id.found_trans_number_text_view);
                        TextView quickSumTV = view.findViewById(R.id.quick_sum_text_view);
                        TextView quickTransAvgTV = view.findViewById(R.id.quick_trans_avg_text_view);

                        quickSummaryLL.setVisibility(View.VISIBLE);
                        foundTransTV.setText("Found " + count + " transactions.");
                        quickSumTV.setText("Sum: €" + String.format(Locale.ENGLISH, "%.2f", Float.valueOf(sum)));
                        quickTransAvgTV.setText("Avg: €" + String.format(Locale.ENGLISH, "%.2f", Float.valueOf(avg)));

                        popupWindow.dismiss();
                    }
                });

                popupWindow.showAsDropDown(view, displayWidth/2-width/2, Math.round(displayHeight * 0.15f));
            }
        });

        return view;
    }

    public boolean showTransactions(String transactions, int start, int amp) {
        tableLayout.removeAllViews();
        historyTitleTV.setText(helper.getDataOrderedByDate().split("\n").length + " transactions in DB");

        if (transactions.isEmpty()) {
            return false;
        }

        boolean res;

        final float density = getContext().getResources().getDisplayMetrics().density;

        String[] rows = transactions.split("\n");

        if (start > rows.length) {
            Toast.makeText(getContext(), "ERROR", Toast.LENGTH_SHORT).show();
            return false;
        }

        int end;

        if (amp == -1 || start + amp > rows.length) {
            end = rows.length;
            res = false;
        } else {
            end = start + amp;
            res = true;
        }

        for (int t = start; t < end; t++) {

            String[] fields = rows[t].split(" ");
            String id = fields[0];
            String date = fields[1].substring(8,10) + "/" + fields[1].substring(5,7) + "/" + fields[1].substring(2,4);
            final String amount = String.format(Locale.ENGLISH,"%.2f", Float.valueOf(fields[2]));
            String method = fields[3];
            String category = fields[4].replace("_", " ");
            String comments = "";
            try {
                comments = fields[5].replace("_"," ");
            } catch (Exception e) {

            }

            TableRow row = new TableRow(getContext());
            TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
            row.setLayoutParams(lp);
            int textColor;
            if (t % 2 == 0) {
                textColor = Color.parseColor("#000000");
            } else {
                textColor = Color.parseColor("#666666");
            }


            TextView idTV = new TextView(getContext());
            idTV.setWidth((int) (Config.COL_ID_WIDTH_DP * density));
            idTV.setText(id);
            idTV.setTextSize(Config.TEXT_SIZE);
            idTV.setTextColor(textColor);
            idTV.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);

            row.addView(idTV);


            TextView dateTV = new TextView(getContext());
            dateTV.setWidth((int)(Config.COL_DATE_WIDTH_DP * density));
            dateTV.setText(date);
            dateTV.setTextSize(Config.TEXT_SIZE);
            dateTV.setTextColor(textColor);
            //dateTV.setPadding((int) getResources().getDimension(R.dimen.padding), 0, 0, 0);

            TextView amountTV = new TextView(getContext());
            amountTV.setWidth((int)(Config.COL_AMOUNT_WIDTH_DP * density));
            amountTV.setText(amount+"€");
            amountTV.setTextSize(Config.TEXT_SIZE);
            amountTV.setGravity(Gravity.RIGHT);
            amountTV.setPadding(0, 0, Config.AMOUNT_PADDING_RIGHT, 0);
            amountTV.setTypeface(Typeface.DEFAULT_BOLD);
            if (amount.indexOf('-') == -1) {
                amountTV.setTextColor(getResources().getColor(R.color.colorAccent));
            } else {
                amountTV.setTextColor(getResources().getColor(R.color.colorPrimary));
            }

            LinearLayout methodLL = new LinearLayout(getContext());
            if (method.equals("Cash")) {
                methodLL.setBackground(getResources().getDrawable(R.drawable.ic_cash));
            } else {
                methodLL.setBackground(getResources().getDrawable(R.drawable.ic_card));
            }

            TextView categoryTV = new TextView(getContext());
            categoryTV.setWidth((int)(Config.COL_CATEGORY_WIDTH_DP * density));
            categoryTV.setText(category);
            categoryTV.setTextSize(Config.TEXT_SIZE);
            categoryTV.setTextColor(textColor);
            categoryTV.setPadding(Config.AMOUNT_PADDING_RIGHT, 0, 0, 0);
            categoryTV.setEllipsize(TextUtils.TruncateAt.END);
            categoryTV.setMaxLines(1);

            TextView commentsTV = new TextView(getContext());
            commentsTV.setWidth((int)(Config.COL_COMMENTS_WIDTH_DP * density));
            commentsTV.setText(comments);
            commentsTV.setTextSize(Config.TEXT_SIZE);
            commentsTV.setTextColor(textColor);
            commentsTV.setEllipsize(TextUtils.TruncateAt.END);
            commentsTV.setMaxLines(1);

            row.addView(dateTV);
            row.addView(amountTV);
            row.addView(methodLL);
            row.addView(categoryTV);
            row.addView(commentsTV);
            tableLayout.addView(row, t);

            final String idFinal = id;
            final String dateFinal = date;
            final String amountFinal = amount;
            final String methodFinal = method;
            final String categoryFinal = category;
            final String commentsFinal = comments;

            row.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        xPos = event.getRawX();
                        yPos = event.getRawY();
                    }
                    return false;
                }
            });

            row.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View popupView = inflater.inflate(R.layout.popup_window_edit_delete, null);

                    int width = LinearLayout.LayoutParams.WRAP_CONTENT;
                    int height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    boolean focusable = true;
                    final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);
                    popupWindow.setElevation(20);

                    TextView popupIdTV = popupView.findViewById(R.id.popup_id_text_view);
                    popupIdTV.setText(idFinal);

                    Button popupEditBT = popupView.findViewById(R.id.edit_trans_button);
                    popupEditBT.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String[] payload = {"0", idFinal, amountFinal, dateFinal, methodFinal, categoryFinal, commentsFinal};
                            sharedVM.setFragment(payload);
                            popupWindow.dismiss();
                        }
                    });

                    Button popupDeleteBT = popupView.findViewById(R.id.delete_trans_button);
                    popupDeleteBT.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            helper.deleteById(idFinal);
                            Toast.makeText(getContext(), "Transaction " + idFinal + " has been deleted.", Toast.LENGTH_SHORT).show();
                            showTransactions(helper.getDataOrderedByDate(), 0, DEFAULT_AMPLITUDE);
                            sharedVM.setPressed(true);
                            popupWindow.dismiss();
                        }
                    });

                    // Get display dimensions
                    WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                    Display display = wm.getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    int displayWidth = size.x;
                    int displayHeight = size.y;

                    // Get fragment view dimensions
                    int viewWidth = view.getMeasuredWidth();
                    int viewHeight = view.getMeasuredHeight();

                    int xOffset = 100;
                    int yOffset = 100;

                    popupWindow.showAsDropDown(view, Math.round(xPos + xOffset), Math.round(yPos - displayHeight + viewHeight - yOffset));

                    Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                    return false;
                }
            });
        }

        Toast.makeText(getContext(), "Ready!", Toast.LENGTH_SHORT).show();

        return res;
    }

}

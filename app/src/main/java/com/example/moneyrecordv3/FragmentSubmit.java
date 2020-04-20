package com.example.moneyrecordv3;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FragmentSubmit extends Fragment {

    View view;
    private SharedViewModel sharedVM;

    EditText amountET, dateET, categoryET, commentsET;
    RadioButton cashRB, cardRB;
    TextView responseTV;
    Button submitBT, exitEditBT;

    ProgressBar progressBar;

    String amount, date, method, category, comments;
    long dateMillis;
    float amountNmb;

    DateFormat df = new SimpleDateFormat("dd/MM/yy");

    myDbAdapter helper;

    boolean editMode = false;
    String editId;

    public FragmentSubmit() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.submit_fragment, container, false);

        sharedVM = ViewModelProviders.of(getActivity()).get(SharedViewModel.class);

        amountET = view.findViewById(R.id.amount_edit_text);
        dateET = view.findViewById(R.id.date_edit_text);
        cashRB = view.findViewById(R.id.cash_radio_button);
        cardRB = view.findViewById(R.id.card_radio_button);
        categoryET = view.findViewById(R.id.category_edit_text);
        commentsET = view.findViewById(R.id.comments_edit_text);
        responseTV = view.findViewById(R.id.response_text_view);
        submitBT = view.findViewById(R.id.submit_button);
        exitEditBT = view.findViewById(R.id.exit_edit_button);

        helper = new myDbAdapter(getContext());

        exitEditBT.setVisibility(View.GONE);

        submitBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initParams();

                if (okToGo(date, amount, category)) {
                    /* Adjust variables input by user,
                        i.e. convert date into milliseconds,
                        convert amount from string to float,
                        and other changes to category and comments
                     */
                    modifyParams();

                    // Insert transactions in existing database
                    insertTransaction();

                    // If edit mode is active, delete old transaction from db
                    if (editMode) {
                        helper.deleteById(editId);
                        Toast.makeText(getContext(), "Transaction " + editId + " has been modified!", Toast.LENGTH_SHORT).show();
                    }

                    // Notify other fragments that a new transactions has been submitted
                    sharedVM.setPressed(true);

                    // Reset inout fields
                    resetFields();
                }
            }
        });

        exitEditBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFields();
            }
        });

        sharedVM.getFragment().observe(this, new Observer<String[]>() {
            @Override
            public void onChanged(@Nullable String[] payload) {
                editMode = true;

                editId = payload[1];
                amountET.setText(payload[2]);
                dateET.setText(payload[3]);
                cashRB.setChecked(payload[4].equals("Cash"));
                cardRB.setChecked(payload[4].equals("Card"));
                categoryET.setText(payload[5]);
                commentsET.setText(payload[6]);

                submitBT.setText("MODIFY");
                exitEditBT.setVisibility(View.VISIBLE);
            }
        });

        return view;
    }

    public void initParams() {
        amount = amountET.getText().toString();

        date = dateET.getText().toString();
        if (date.equals("") || date.equals("Today")) {
            date = df.format(Calendar.getInstance().getTime());
        }

        if (cashRB.isChecked()) {
            method = "Cash";
        } else {
            method = "Card";
        }

        category = categoryET.getText().toString();

        comments = commentsET.getText().toString();
    }

    public void insertTransaction() {
        helper.insertData(date, amountNmb, method, category, comments);
    }

    public void modifyParams() {
        // If trans is submitted between 00:00 and 03:00, save it as "yesterday"
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (dateET.getText().toString().equals("") && currentHour < 4) {
            date = df.format(Calendar.getInstance().getTime().getTime() - 86400000);
        }

        // Convert input date string dd*mm*yy into format YYYY-MM-DD
        date = "20" + date.substring(6,8) + "-" + date.substring(3,5) + "-" + date.substring(0,2);

        // Convert input amount string into float
        amountNmb = Float.valueOf(amount);

        // modify category and comments strings to eliminate eventual space at the end
        if (category.charAt(category.length()-1) == ' ') {
            category = category.substring(0, category.length()-1);
        }
        category = category.replace(" ", "_");

        if (!comments.isEmpty()) {
            if (comments.charAt(comments.length()-1) == ' ') {
                comments = comments.substring(0, comments.length()-1);
            }
            comments = comments.replace(" ","_");
        }
    }

    public boolean okToGo(String date, String amount, String category) {
        if (amount.isEmpty() || category.isEmpty()) {
            responseTV.setText("Empty fields!");
            responseTV.setTextColor(getResources().getColor(R.color.colorAccent));
            //Toast.makeText(getContext(), "Empty fields!", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!correctAmountFormat(amount)) { return false; }

        if (!date.isEmpty() && !correctDateFormat(date)) { return false; }

        return true;
    }

    public boolean correctAmountFormat(String amount) {
        if (amount.indexOf('.') != -1) {
            if (amount.length() > amount.indexOf('.') + 3) {
                responseTV.setText("Amount: xxxx.yy");
                responseTV.setTextColor(getResources().getColor(R.color.colorAccent));
                //Toast.makeText(getContext(), "Amount: xxxx.yy", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    public boolean correctDateFormat(String date) {
        boolean dot = date.matches("\\d{2}.\\d{2}.\\d{2}");
        boolean dash = date.matches("\\d{2}-\\d{2}-\\d{2}");
        boolean slash = date.matches("\\d{2}/\\d{2}/\\d{2}");

        if (!(dot || dash || slash)) {
            responseTV.setText("Date: dd*mm*yy");
            responseTV.setTextColor(getResources().getColor(R.color.colorAccent));
            //Toast.makeText(getContext(), "Date: dd*mm*yy", Toast.LENGTH_SHORT).show();
            return false;
        } else{
            int dd = Integer.valueOf(date.substring(0,2));
            int mm = Integer.valueOf(date.substring(3,5));
            int yy = Integer.valueOf(date.substring(6,8));

            boolean dBool = dd > 0;
            boolean mBool = (mm > 0) && (mm < 13);
            boolean yBool = yy > 0;

            switch (mm) {
                case 1:
                    dBool = dBool && (dd < 32);
                    break;
                case 2:
                    dBool = dBool && (dd < 30);
                    break;
                case 3:
                    dBool = dBool && (dd < 32);
                    break;
                case 4:
                    dBool = dBool && (dd < 31);
                    break;
                case 5:
                    dBool = dBool && (dd < 32);
                    break;
                case 6:
                    dBool = dBool && (dd < 31);
                    break;
                case 7:
                    dBool = dBool && (dd < 32);
                    break;
                case 8:
                    dBool = dBool && (dd < 32);
                    break;
                case 9:
                    dBool = dBool && (dd < 31);
                    break;
                case 10:
                    dBool = dBool && (dd < 32);
                    break;
                case 11:
                    dBool = dBool && (dd < 31);
                    break;
                case 12:
                    dBool = dBool && (dd < 32);
                    break;
            }

            if (!(dBool && mBool && yBool)) {
                responseTV.setText("Date doesn't exist!");
                responseTV.setTextColor(getResources().getColor(R.color.colorAccent));
                //Toast.makeText(getContext(), "Date doesn't exist!", Toast.LENGTH_SHORT).show();
                return false;
            }

            return true;
        }

    }

    public void resetFields() {
        amountET.setText("");
        dateET.setText("");
        categoryET.setText("");
        commentsET.setText("");
        cashRB.setChecked(true); // card automatically set to false since in radio group

        submitBT.setText("SUBMIT");
        exitEditBT.setVisibility(View.GONE);
        editMode = false;
    }



}

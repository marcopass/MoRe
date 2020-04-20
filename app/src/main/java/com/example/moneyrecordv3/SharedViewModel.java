package com.example.moneyrecordv3;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Boolean> pressed = new MutableLiveData<Boolean>();

    public void setPressed(Boolean bool) {
        pressed.setValue(bool);
    }

    public LiveData<Boolean> getPressed() {
        return pressed;
    }

    private final MutableLiveData<String[]> payload = new MutableLiveData<String[]>();

    public void setFragment(String[] pl) {
        payload.setValue(pl);
    }

    public LiveData<String[]> getFragment() {
        return payload;
    }

}

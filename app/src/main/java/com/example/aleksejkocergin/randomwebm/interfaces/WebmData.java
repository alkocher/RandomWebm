package com.example.aleksejkocergin.randomwebm.interfaces;


import com.example.aleksejkocergin.myapplication.WebmQuery;

public interface WebmData {

    void render(WebmQuery.Data data);
    void showSuccessSnackbar();
    void showErrorSnackbar();
}

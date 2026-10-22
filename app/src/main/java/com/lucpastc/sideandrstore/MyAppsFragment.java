package com.lucpastc.sideandrstore;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;

public class MyAppsFragment extends Fragment {

    private LinearLayout appsListContainer;
    private LinearLayout menuPopup;
    private File appsJsonFile;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            processAndInstallApk(uri);
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_apps, container, false);

        EditText searchBox = view.findViewById(R.id.search_box);
        Button fabButton = view.findViewById(R.id.fab_button);
        menuPopup = view.findViewById(R.id.menu_popup);
        Button btnInstallApk = view.findViewById(R.id.btn_install_apk);
        appsListContainer = view.findViewById(R.id.apps_list_container);

        fabButton.setOnClickListener(v -> {
            if (menuPopup.getVisibility() == View.VISIBLE) {
                menuPopup.setVisibility(View.GONE);
            } else {
                menuPopup.setVisibility(View.VISIBLE);
            }
        });

        btnInstallApk.setOnClickListener(v -> {
            menuPopup.setVisibility(View.GONE);
            openFilePicker();
        });

        setupJsonStorage();
        loadSavedApps("");

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadSavedApps(s.toString().toLowerCase());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void setupJsonStorage() {
        File dir = new File(requireContext().getExternalFilesDir(null), "apps");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        appsJsonFile = new File(dir, "apps.json");
        if (!appsJsonFile.exists()) {
            try {
                FileWriter writer = new FileWriter(appsJsonFile);
                writer.write("[]");
                writer.flush();
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.android.package-archive");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Selecionar APK"));
    }

    private void processAndInstallApk(Uri apkUri) {
        try {
            // Copia o APK para um arquivo temporário no cache para conseguirmos o caminho em String
            File tempFile = new File(requireContext().getCacheDir(), "temp_install.apk");
            java.io.InputStream inputStream = requireContext().getContentResolver().openInputStream(apkUri);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            inputStream.close();

            // Lê as informações usando o caminho absoluto em String do arquivo temporário
            PackageManager pm = requireContext().getPackageManager();
            PackageInfo packageInfo = pm.getPackageArchiveInfo(tempFile.getAbsolutePath(), PackageManager.GET_ACTIVITIES);

            String packageName = "unknown.package";
            String appName = "App Desconhecido";

            if (packageInfo != null) {
                packageName = packageInfo.packageName;
                if (packageInfo.applicationInfo != null) {
                    packageInfo.applicationInfo.sourceDir = tempFile.getAbsolutePath();
                    packageInfo.applicationInfo.publicSourceDir = tempFile.getAbsolutePath();
                    CharSequence label = pm.getApplicationLabel(packageInfo.applicationInfo);
                    if (label != null) {
                        appName = label.toString();
                    }
                }
            }

            // Usa o FileProvider para instalar o APK de forma segura a partir do cache
            Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    tempFile
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            // Salva os dados corretos no JSON
            addAppToJson(appName, packageName);
            loadSavedApps("");
            Toast.makeText(requireContext(), "Instalação iniciada!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Erro ao processar APK: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void addAppToJson(String name, String packageName) {
        try {
            FileInputStream fis = new FileInputStream(appsJsonFile);
            StringBuilder sb = new StringBuilder();
            int ch;
            while ((ch = fis.read()) != -1) {
                sb.append((char) ch);
            }
            fis.close();

            JSONArray jsonArray = new JSONArray(sb.toString());

            // Evita duplicar se já existir o pacote
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                if (obj.getString("package").equals(packageName)) {
                    return; // Já está registrado
                }
            }

            JSONObject newApp = new JSONObject();
            newApp.put("name", name);
            newApp.put("package", packageName);
            jsonArray.put(newApp);

            FileWriter writer = new FileWriter(appsJsonFile);
            writer.write(jsonArray.toString(4));
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSavedApps(String filter) {
        if (appsListContainer == null) return;
        appsListContainer.removeAllViews();
        try {
            FileInputStream fis = new FileInputStream(appsJsonFile);
            StringBuilder sb = new StringBuilder();
            int ch;
            while ((ch = fis.read()) != -1) {
                sb.append((char) ch);
            }
            fis.close();

            JSONArray jsonArray = new JSONArray(sb.toString());
            PackageManager pm = requireContext().getPackageManager();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String name = obj.getString("name");
                String pkg = obj.getString("package");

                if (!filter.isEmpty() && !name.toLowerCase().contains(filter) && !pkg.toLowerCase().contains(filter)) {
                    continue;
                }

                LinearLayout card = new LinearLayout(requireContext());
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setBackgroundColor(Color.parseColor("#1E1E1E"));
                card.setPadding(16, 16, 16, 16);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.bottomMargin = 16;
                card.setLayoutParams(cardParams);
                card.setGravity(android.view.Gravity.CENTER_VERTICAL);

                // Ícone Real
                ImageView iconView = new ImageView(requireContext());
                try {
                    iconView.setImageDrawable(pm.getApplicationIcon(pkg));
                } catch (Exception e) {
                    iconView.setImageResource(android.R.drawable.sym_def_app_icon);
                }
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(96, 96);
                iconParams.rightMargin = 16;
                iconView.setLayoutParams(iconParams);
                card.addView(iconView);

                LinearLayout infoLayout = new LinearLayout(requireContext());
                infoLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                infoLayout.setLayoutParams(infoParams);

                TextView nameView = new TextView(requireContext());
                nameView.setText(name);
                nameView.setTextColor(Color.WHITE);
                nameView.setTextSize(16f);
                infoLayout.addView(nameView);

                TextView pkgView = new TextView(requireContext());
                pkgView.setText(pkg);
                pkgView.setTextColor(Color.parseColor("#888888"));
                pkgView.setTextSize(12f);
                infoLayout.addView(pkgView);

                card.addView(infoLayout);

                Button openButton = new Button(requireContext());
                openButton.setText("Abrir");
                openButton.setTextColor(Color.WHITE);
                openButton.setOnClickListener(v -> {
                    Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
                    if (launchIntent != null) {
                        startActivity(launchIntent);
                    } else {
                        Toast.makeText(requireContext(), "Aplicativo não instalado ou sem intenção de launch", Toast.LENGTH_SHORT).show();
                    }
                });
                card.addView(openButton);

                appsListContainer.addView(card);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
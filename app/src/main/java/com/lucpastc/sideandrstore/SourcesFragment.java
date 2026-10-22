package com.lucpastc.sideandrstore;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class SourcesFragment extends Fragment {

    private LinearLayout sourcesListContainer;
    private File sourcesJsonFile;
    private File apksDir;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sources, container, false);

        sourcesListContainer = view.findViewById(R.id.sources_list_container);
        Button fabSourceButton = view.findViewById(R.id.fab_source_button);

        setupStorage();
        loadSavedSources();

        fabSourceButton.setOnClickListener(v -> showAddSourceDialog());

        return view;
    }

    private void setupStorage() {
        File baseDir = new File(requireContext().getExternalFilesDir(null), "sources");
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        sourcesJsonFile = new File(baseDir, "sources.json");
        if (!sourcesJsonFile.exists()) {
            try {
                FileWriter writer = new FileWriter(sourcesJsonFile);
                writer.write("[]");
                writer.flush();
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        apksDir = new File(baseDir, "apks");
        if (!apksDir.exists()) {
            apksDir.mkdirs();
        }
    }

    private void showAddSourceDialog() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_source, null);

        EditText etSourceUrl = dialogView.findViewById(R.id.et_source_url);
        Button btnSaveSource = dialogView.findViewById(R.id.btn_save_source);
        Button btnCloseDialog = dialogView.findViewById(R.id.btn_close_dialog);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());

        btnSaveSource.setOnClickListener(v -> {
            String urlStr = etSourceUrl.getText().toString().trim();
            if (!urlStr.isEmpty()) {
                saveSourceToJson(urlStr);
                loadSavedSources();
                dialog.dismiss();
                Toast.makeText(requireContext(), "Source adicionada com sucesso!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Insira uma URL válida", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void saveSourceToJson(String url) {
        try {
            FileInputStream fis = new FileInputStream(sourcesJsonFile);
            StringBuilder sb = new StringBuilder();
            int ch;
            while ((ch = fis.read()) != -1) {
                sb.append((char) ch);
            }
            fis.close();

            JSONArray jsonArray = new JSONArray(sb.toString());
            JSONObject newSource = new JSONObject();
            newSource.put("url", url);
            jsonArray.put(newSource);

            FileWriter writer = new FileWriter(sourcesJsonFile);
            writer.write(jsonArray.toString(4));
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSavedSources() {
        if (sourcesListContainer == null) return;
        sourcesListContainer.removeAllViews();
        try {
            FileInputStream fis = new FileInputStream(sourcesJsonFile);
            StringBuilder sb = new StringBuilder();
            int ch;
            while ((ch = fis.read()) != -1) {
                sb.append((char) ch);
            }
            fis.close();

            JSONArray jsonArray = new JSONArray(sb.toString());

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String url = obj.getString("url");

                LinearLayout card = new LinearLayout(requireContext());
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackgroundColor(Color.parseColor("#1E1E1E"));
                card.setPadding(20, 20, 20, 20);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.bottomMargin = 16;
                card.setLayoutParams(params);

                TextView urlView = new TextView(requireContext());
                urlView.setText(url);
                urlView.setTextColor(Color.WHITE);
                urlView.setTextSize(14);
                card.addView(urlView);

                card.setOnClickListener(v -> openSourceAppsViewer(url));

                sourcesListContainer.addView(card);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openSourceAppsViewer(String sourceUrl) {
        // Exemplo simplificado abrindo uma atividade ou carregando os apps da URL do repositório
        Toast.makeText(requireContext(), "Abrindo repositório: " + sourceUrl, Toast.LENGTH_SHORT).show();

        // Aqui você pode implementar o download do arquivo JSON da source e listar os apps em um diálogo/nova tela.
        // Ao clicar em instalar o app do repositório, executamos o download do APK para apksDir e instalamos:
        // downloadAndInstallApp("https://exemplo.com/app.apk", "Nome do App", "com.pacote.app");
    }

    private void downloadAndInstallApp(String apkUrl, String appName, String packageName) {
        new Thread(() -> {
            try {
                URL url = new URL(apkUrl);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("GET");
                c.connect();

                File outputFile = new File(apksDir, appName.replaceAll("[^a-zA-Z0-9]", "_") + ".apk");
                FileOutputStream fos = new FileOutputStream(outputFile);

                java.io.InputStream inputStream = c.getInputStream();
                byte[] buffer = new byte[1024];
                int len1;
                while ((len1 = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len1);
                }
                fos.close();
                inputStream.close();

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Download concluído! Instalando...", Toast.LENGTH_SHORT).show();

                    // Instala o APK baixado
                    Uri apkUri = androidx.core.content.FileProvider.getUriForFile(
                            requireContext(),
                            requireContext().getPackageName() + ".fileprovider",
                            outputFile
                    );

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Erro no download: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
}
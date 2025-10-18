package com.bilocan.mapsforeveryone.api;

import android.util.Log;
import com.bilocan.mapsforeveryone.api.model.TransitResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class TransitServiceImpl implements TransitService {
    private static final String TAG = "TransitServiceImpl";
    private static final int SCRIPT_TIMEOUT_SECONDS = 30; // 30 saniye timeout
    private static final int MAX_RETRIES = 2; // 2 deneme yeterli

    @Override
    public TransitResponse getTransitInfo(String origin, String destination) {
        Log.i(TAG, "Transit bilgisi istendi: origin=" + origin + ", destination=" + destination);
        long startTime = System.currentTimeMillis();
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                String scriptPath = "../demo/python_scripts/get_transit_info.py";
                File scriptFile = new File(scriptPath);

                if (!scriptFile.exists()) {
                    Log.e(TAG, "Python script bulunamadı: " + scriptPath);
                    throw new RuntimeException("Python scripti bulunamadı: " + scriptPath);
                }

                // Python modüllerini kontrol et ve yükle
                checkAndInstallPythonModules();

                // Transit bilgilerini getir
                ProcessBuilder pb = new ProcessBuilder(
                        "python",
                        scriptPath,
                        origin,
                        destination);

                // UTF-8 kodlaması ve environment değişkenlerini ayarla
                pb.environment().put("PYTHONIOENCODING", "utf-8");
                pb.environment().put("PYTHONUNBUFFERED", "1"); // Python çıktı tamponlamasını devre dışı bırak
                pb.redirectErrorStream(true);

                Log.i(TAG, "Process çalıştırılıyor: " + pb.command() + " (Deneme " + (retryCount + 1) + "/" + MAX_RETRIES + ")");

                Process process = pb.start();
                boolean completed = process.waitFor(SCRIPT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (!completed) {
                    process.destroy();
                    long executionTime = System.currentTimeMillis() - startTime;
                    Log.e(TAG, "Python script zaman aşımına uğradı (" + SCRIPT_TIMEOUT_SECONDS + " saniye). Geçen süre: " + executionTime + " ms");
                    
                    retryCount++;
                    if (retryCount < MAX_RETRIES) {
                        long delay = 1000L * (retryCount + 1); // Exponential backoff
                        Log.i(TAG, "Yeniden deneme yapılıyor... (" + delay + " ms sonra)");
                        Thread.sleep(delay);
                        continue;
                    }
                    
                    throw new RuntimeException("Python script zaman aşımına uğradı (" + 
                        SCRIPT_TIMEOUT_SECONDS + " saniye) - Maksimum deneme sayısına ulaşıldı");
                }

                // Çıktıyı oku
                String output = readProcessOutput(process);
                Log.d(TAG, "Python çıktısı: " + output);

                // JSON parse et ve TransitResponse'a dönüştür
                return parseTransitResponse(output);

            } catch (Exception e) {
                Log.e(TAG, "Hata oluştu: ", e);
                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    long delay = 1000L * (retryCount + 1); // Exponential backoff
                    Log.i(TAG, "Yeniden deneme yapılıyor... (" + delay + " ms sonra)");
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Log.e(TAG, "Thread.sleep interrupted: ", ie);
                    }
                    continue;
                }
                
                throw new RuntimeException("Maksimum deneme sayısına ulaşıldı ve hata oluştu", e);
            }
        }

        throw new RuntimeException("Maksimum deneme sayısına ulaşıldı");
    }

    private void checkAndInstallPythonModules() throws IOException, InterruptedException {
        // Python modüllerini kontrol et ve yükle
        ProcessBuilder checkModuleBuilder = new ProcessBuilder(
                "python", "-c", "import requests; print('Requests module OK')");
        Process checkProcess = checkModuleBuilder.start();
        String checkOutput = readProcessOutput(checkProcess);

        if (!checkOutput.contains("Requests module OK")) {
            Log.w(TAG, "Requests modülü bulunamadı, yükleniyor...");

            ProcessBuilder installBuilder = new ProcessBuilder(
                    "python", "-m", "pip", "install", "--no-cache-dir", "requests");
            Process installProcess = installBuilder.start();
            String installOutput = readProcessOutput(installProcess);
            Log.i(TAG, "Pip install çıktısı: " + installOutput);

            if (installProcess.waitFor() != 0) {
                Log.e(TAG, "Requests modülü yüklenemedi: " + installOutput);
                throw new RuntimeException("Requests modülü yüklenemedi");
            } else {
                Log.i(TAG, "Requests modülü başarıyla yüklendi");
            }
        } else {
            Log.i(TAG, "Requests modülü zaten yüklü");
        }
    }

    private String readProcessOutput(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        String line;
        StringBuilder output = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        // Hata çıktısını da oku
        BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
        StringBuilder errorOutput = new StringBuilder();

        while ((line = errorReader.readLine()) != null) {
            errorOutput.append(line).append("\n");
        }

        if (errorOutput.length() > 0) {
            Log.w(TAG, "Process hata çıktısı: " + errorOutput.toString());
            output.append("\nERROR: ").append(errorOutput);
        }

        return output.toString().trim();
    }

    private TransitResponse parseTransitResponse(String jsonOutput) {
        // JSON parse işlemi burada yapılacak
        // Şimdilik basit bir TransitResponse döndürüyoruz
        return new TransitResponse();
    }
} 
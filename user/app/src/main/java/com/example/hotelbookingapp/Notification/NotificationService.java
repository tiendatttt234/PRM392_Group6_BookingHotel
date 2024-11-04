package com.example.hotelbookingapp.Notification;

import android.util.Log;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class NotificationService {
    private static final String FCM_API_URL = "https://fcm.googleapis.com/v1/projects/hotelbookingapp-cac10/messages:send";
    private static final String firebaseMessagingScope = "https://www.googleapis.com/auth/firebase.messaging";

    public static void saveFCMToken(String userid) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                Log.d("FCM Token", "Token: " + token);
                // Save token to Realtime Database
                saveFcmTokenToDatabase(userid, token);
            }
        });
    }

    private static void saveFcmTokenToDatabase(String uid, String token) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Tokens");
        databaseReference.child(uid).setValue(token)
                .addOnSuccessListener(aVoid -> Log.d("RealtimeDB", "FCM token saved successfully."))
                .addOnFailureListener(e -> Log.w("RealtimeDB", "Error saving FCM token", e));
    }

    public static void sendNotification(String uid) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Tokens");

        databaseReference.child(uid).get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        String token = dataSnapshot.getValue(String.class);
                        sendFCM(token);
                    } else {
                        Log.w("RealtimeDB", "No such document");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("RealtimeDB", "Error getting FCM token", e);
                });
    }

    public static void sendFCM(String token) {
        new Thread(() -> {
            try {
                URL url = new URL(FCM_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Xây dựng nội dung JSON gửi đi
                JSONObject message = new JSONObject();
                JSONObject notification = new JSONObject();
                notification.put("title", "Hotel Booking App");
                notification.put("body", "Bạn có lịch đặt mới");


                JSONObject payload = new JSONObject();
                payload.put("token", token);
                payload.put("notification", notification);

                message.put("message", payload);

                // Gửi yêu cầu
                OutputStream os = conn.getOutputStream();
                os.write(message.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.i("Notification", "No such document");
                } else {
                    Log.i( "Notification","Failed to send notification, response code: " + responseCode);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static String getAccessToken() {
        try {
            String jsonString = "{\n" +
                    "  \"type\": \"service_account\",\n" +
                    "  \"project_id\": \"hotelbookingapp-cac10\",\n" +
                    "  \"private_key_id\": \"08dcdf77ae057ebd9a46d06d6fc51c2b378d3855\",\n" +
                    "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCijmOtaJGdY+2g\\nxAEiaxv405Kf6tGPdxF1vkJ80o4EOlZjsd3ZB1mxifvYyx5StSKIbmICz+yJ/2YM\\nDoN4VjWz7hwNWLi+6iz5teytGoPQNJmKBu265Lo9d1ztmi/gVGSGOm+xVsRwNTDY\\nvf0jPq+SMZBpwqEnoTSdY2UEj49XBBKqmvIyVqTmCajMm4lxijFMxjfUl+8QH0fn\\nmypWIZ4FdkFvuNbKNYNLc7Su8XT0S3i3wTSJDH5Ab+UDjKutN25Ai9TcEgXP84Px\\nByq1Cr2iP4r+5P6z3UB1kEzDbsUvtTdpfHrRNtYaFVguCWul5WuJwSMvB/dpyc8V\\n5QfWAXUBAgMBAAECggEAIcRl0NESxIjMRTPKD4oD5L+gau9GqmBfxF9qJM9BOIAc\\nz3dUf/+OonppEjr5WNFvM7uISRDDkFP5MQJ3zXDNFYweECiwsdnnz1R67md+X6r5\\nhQpU6liWlmub5nB7xwQI4Lg1WPeO9UcbVz66HY7pPjtlBg8r1lVenl4WDyk6K6ck\\nQN0F3OSTCj+1Gk8k0bSVCbangKLUsXQgjjpGo813krojUHRi0ilI5RaFJ8hkMmOc\\nPQrU0haH9MbXiIkSWcfasiu1/ipuJcAZ9vLANp2ZL098iy5JlFLApm+nvNSjRMYR\\nm8+tsanXLlw8qg1iCxiA7Vm9hanahyDhnC5lPbCHtQKBgQDZ5YV0ljP8fDtKDg0Q\\nYMI05TjcdK2nh0rkwQ7txqv2oTNpY1tCkJvkW50HcZaBBA7dSBF8Rv6yQnVidicT\\nEegpA/ErpzyvRYbOc1AMbPPNWNhNyD67+HYBJ+/15x7X2L5uY9jZ7MZbsGHTffNi\\ndBNbXcqrzMTH0L0n1Lgx8okc9QKBgQC++3iHoS0Zr4YgPOKmH78qsOLcmTrRVCIj\\nnONGmIffgpRVISelrLlba/DKp1T7mvsOks6mrDB7H0fLn9GQfGQjoyWdJnpAon/I\\nNvkuIuXWzbo558qLNkQKgFToCogHTQ6c3wwB759pl9qQwAzbhga79Cg+iiKTRrcu\\nrJTstzQwXQKBgHuDNBPzNIKxdPY0Ysle35cWPBYS+YCWGyjyGmFEFaQWmkrp7Age\\nao+WMrvOck6tmzNpr6evop4vN9TZPqr7oorlVia1hJuhoJmUGdMBS22iJ/JnSNBK\\nNbHQDqBoIz6c+M0gQgK9yW6d28YDhhvPyk0nLEQYLY9KTn6ugL6nSXulAoGAXI+e\\nfAWmySMj224G30LVsQgn+4icVCX1odMA6A83EyHDaHr4LzjGYUOwYVe2PXeApDCM\\nzYT4vkOL1Wmw9NkE46zHpyGl8LuROOVD4ZNyV6g/0J7BsLTtRzWpcQjhosA7C9ai\\niDGtzkyV1r/tu1t62g9cjer/FOgaEhn817l3JtUCgYB6/aAHbGJ5d/ZwE9A63Ro4\\nXZtzCmigHxeKpzaISIrrh0MN+825IbKH0x3lKXGSHM9wSg57gApkKMGNUsWcmrNE\\nXizc8c8nj698TFhi+pIJH6Mhe5vUXbz5DaNqcG5saj1obTeVDr+Viysj21Refbs5\\nUgbXLPwUaIwM1TkckxYHeg==\\n-----END PRIVATE KEY-----\\n\",\n" +
                    "  \"client_email\": \"firebase-adminsdk-254hc@hotelbookingapp-cac10.iam.gserviceaccount.com\",\n" +
                    "  \"client_id\": \"100155494974581718388\",\n" +
                    "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                    "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                    "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                    "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-254hc%40hotelbookingapp-cac10.iam.gserviceaccount.com\",\n" +
                    "  \"universe_domain\": \"googleapis.com\"\n" +
                    "}";
            InputStream stream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));
            GoogleCredentials googleCredentials = GoogleCredentials.fromStream(stream).createScoped(firebaseMessagingScope);
            googleCredentials.refresh();
            return googleCredentials.getAccessToken().getTokenValue();
        } catch (Exception e) {
            Log.e("AccessToken", "getAccessToken: " + e.getLocalizedMessage());
            return null;
        }
    }
}

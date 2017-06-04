package io.github.froger.instamaterial;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import io.github.froger.instamaterial.ui.activity.MainActivity;

public class BackgroundService extends Service {

    // ИМЯ СЕРВЕРА (url зарегистрированного нами сайта)
    // например http://l29340eb.bget.ru
    String server_name = "http://u0306965.plsk.regruhosting.ru";

    SQLiteDatabase chatDBlocal;
    HttpURLConnection conn, conn_likes;
    Cursor cursor, cursor_likes;
    Thread thr;
    ContentValues new_mess;
    Long last_time; // время последней записи в БД, отсекаем по нему что нам
    // тянуть с сервера, а что уже есть
    int last_id;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onStart(Intent intent, int startId) {
        Log.i("chat", "+ BackgroundService - запуск сервиса");

        chatDBlocal = openOrCreateDatabase("chatDBlocal.db",
                Context.MODE_PRIVATE, null);
        chatDBlocal
                .execSQL("CREATE TABLE IF NOT EXISTS chat (_id integer primary key autoincrement, author, data, text, img, count)");
        chatDBlocal
                .execSQL("CREATE TABLE IF NOT EXISTS likes (_id integer primary key autoincrement, user, post_id)");

        // создадим и покажем notification
        // это позволит стать сервису "бессмертным"
        // и будет визуально видно в трее
        Intent iN = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pI = PendingIntent.getActivity(getApplicationContext(),
                0, iN, PendingIntent.FLAG_CANCEL_CURRENT);
        Notification.Builder bI = new Notification.Builder(
                getApplicationContext());

        bI.setContentIntent(pI)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(
                        BitmapFactory.decodeResource(getApplicationContext()
                                .getResources(), R.drawable.ic_launcher))
                .setAutoCancel(true)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("работаю...");

        Notification notification = bI.build();
        startForeground(101, notification);

        startLoop();
    }

    // запуск потока, внутри которого будет происходить
    // регулярное соединение с сервером для чтения новых
    // сообщений.
    // если сообщения найдены - отправим броадкаст для обновления
    // ListView в ChatActivity
    private void startLoop() {

        thr = new Thread(new Runnable() {

            // answer = ответ на запрос
            // lnk = линк с параметрами
            String answer, lnk, answer_likes, lnk_likes;

            public void run() {

                while (true) {
                    cursor = chatDBlocal.rawQuery(
                            "SELECT * FROM chat ORDER BY data", null);

                    cursor_likes = chatDBlocal.rawQuery(
                            "SELECT * FROM likes", null);

                    // если какие-либо сообщения есть - формируем запрос
                    // по которому получим только новые сообщения
                    if (cursor.moveToLast()) {
                        last_time = cursor.getLong(cursor
                                .getColumnIndex("data"));
                        lnk = server_name + "/chat.php?action=select&data="
                                + last_time.toString();

                        // если сообщений в БД нет - формируем запрос
                        // по которому получим всё
                    } else {
                        lnk = server_name + "/chat.php?action=select";
                    }

                    if (cursor_likes.moveToLast()) {
                        last_id = cursor_likes.getInt(cursor_likes
                                .getColumnIndex("_id"));
                        lnk_likes = server_name + "/chat.php?action=select_likes&data="
                                + last_id;
                    } else {
                        lnk_likes = server_name + "/chat.php?action=select_likes";
                    }

                    cursor.close();
                    cursor_likes.close();

                    try {
                        Log.i("chat",
                                "+ BackgroundService --------------- ОТКРОЕМ СОЕДИНЕНИЕ");

                        conn = (HttpURLConnection) new URL(lnk)
                                .openConnection();
                        conn.setReadTimeout(10000);
                        conn.setConnectTimeout(15000);
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                        conn.setDoInput(true);
                        conn.connect();

                        conn_likes = (HttpURLConnection) new URL(lnk_likes)
                                .openConnection();
                        conn_likes.setReadTimeout(10000);
                        conn_likes.setConnectTimeout(15000);
                        conn_likes.setRequestMethod("POST");
                        conn_likes.setRequestProperty("User-Agent", "Mozilla/5.0");
                        conn_likes.setDoInput(true);
                        conn_likes.connect();

                    } catch (Exception e) {
                        Log.i("chat", "+ BackgroundService ошибка: " + e.getMessage());
                    }
                    // получаем ответ ---------------------------------->
                    try {
                        InputStream is = conn.getInputStream();
                        BufferedReader br = new BufferedReader(
                                new InputStreamReader(is, "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String bfr_st = null;
                        while ((bfr_st = br.readLine()) != null) {
                            sb.append(bfr_st);
                        }

                        Log.i("chat", "+ BackgroundService - полный ответ сервера:\n"
                                + sb.toString());
                        // сформируем ответ сервера в string
                        // обрежем в полученном ответе все, что находится за "]"
                        // это необходимо, т.к. json ответ приходит с мусором
                        // и если этот мусор не убрать - будет невалидным
                        answer = sb.toString();
                        answer = answer.substring(0, answer.indexOf("]") + 1);

                        is.close(); // закроем поток
                        br.close(); // закроем буфер

                        InputStream is_likes = conn_likes.getInputStream();
                        BufferedReader br_likes = new BufferedReader(
                                new InputStreamReader(is_likes, "UTF-8"));
                        StringBuilder sb_likes = new StringBuilder();
                        String bfr_st_likes = null;
                        while ((bfr_st_likes = br_likes.readLine()) != null) {
                            sb_likes.append(bfr_st_likes);
                        }

                        Log.i("chat", "+ BackgroundService - полный ответ сервера:\n"
                                + sb_likes.toString());
                        // сформируем ответ сервера в string
                        // обрежем в полученном ответе все, что находится за "]"
                        // это необходимо, т.к. json ответ приходит с мусором
                        // и если этот мусор не убрать - будет невалидным
                        answer_likes = sb_likes.toString();
                        answer_likes = answer_likes.substring(0, answer_likes.indexOf("]") + 1);

                        is_likes.close(); // закроем поток
                        br_likes.close(); // закроем буфер

                    } catch (Exception e) {
                        Log.i("chat", "+ BackgroundService ошибка: " + e.getMessage());
                    } finally {
                        conn.disconnect();
                        conn_likes.disconnect();
                        Log.i("chat",
                                "+ BackgroundService --------------- ЗАКРОЕМ СОЕДИНЕНИЕ");
                    }

                    // запишем ответ в БД ---------------------------------->
                    if (answer != null && !answer.trim().equals("")) {

                        Log.i("chat",
                                "+ BackgroundService ---------- ответ содержит JSON:");

                        try {
                            // ответ превратим в JSON массив
                            JSONArray ja = new JSONArray(answer);
                            JSONObject jo;

                            Integer i = 0;

                            while (i < ja.length()) {

                                // разберем JSON массив построчно
                                jo = ja.getJSONObject(i);

                                Log.i("chat",
                                        "=================>>> "
                                                + jo.getString("author")
                                                + " | " + jo.getLong("data")
                                                + " | " + jo.getString("text")
                                                + " | " + jo.getString("img"));

                                // создадим новое сообщение
                                new_mess = new ContentValues();
                                new_mess.put("author", jo.getString("author"));
                                new_mess.put("data", jo.getLong("data"));
                                new_mess.put("text", jo.getString("text"));
                                new_mess.put("img", jo.getString("img"));
                                new_mess.put("count", jo.getInt("count"));
                                // запишем новое сообщение в БД
                                chatDBlocal.insert("chat", null, new_mess);
                                new_mess.clear();

                                i++;
                            }
                        } catch (Exception e) {
                            // если ответ сервера не содержит валидный JSON
                            Log.i("chat",
                                    "+ BackgroundService ---------- ошибка ответа сервера:\n"
                                            + e.getMessage());
                        }
                    } else {
                        // если ответ сервера пустой
                        Log.i("chat",
                                "+ BackgroundService ---------- ответ не содержит JSON!");
                    }

                    if (answer_likes != null && !answer_likes.trim().equals("")) {

                        Log.i("chat",
                                "+ BackgroundService ---------- ответ содержит JSON:");

                        try {
                            // ответ превратим в JSON массив
                            JSONArray ja = new JSONArray(answer_likes);
                            JSONObject jo;

                            Integer i = 0;

                            while (i < ja.length()) {

                                // разберем JSON массив построчно
                                jo = ja.getJSONObject(i);

                                Log.i("chat",
                                        "=================>>> "
                                                + " | " + jo.getString("user")
                                                + " | " + jo.getInt("post_id"));
                                // создадим новое сообщение
                                new_mess = new ContentValues();
                                new_mess.put("user", jo.getString("user"));
                                new_mess.put("post_id", jo.getInt("post_id"));
                                // запишем новое сообщение в БД
                                chatDBlocal.insert("likes", null, new_mess);
                                new_mess.clear();

                                i++;
                            }
                        } catch (Exception e) {
                            // если ответ сервера не содержит валидный JSON
                            Log.i("chat",
                                    "+ BackgroundService ---------- ошибка ответа сервера:\n"
                                            + e.getMessage());
                        }
                    } else {
                        // если ответ сервера пустой
                        Log.i("chat",
                                "+ BackgroundService ---------- ответ не содержит JSON!");
                    }

                    try {
                        Thread.sleep(1500);
                    } catch (Exception e) {
                        Log.i("chat",
                                "+ BackgroundService - ошибка процесса: "
                                        + e.getMessage());
                    }
                }
            }
        }, "myThread");

        thr.setDaemon(true);
        thr.start();

    }
}
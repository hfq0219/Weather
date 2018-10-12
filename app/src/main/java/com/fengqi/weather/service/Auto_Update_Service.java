package com.fengqi.weather.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.fengqi.weather.R;
import com.fengqi.weather.json.Weather;
import com.fengqi.weather.util.HttpUtil;
import com.fengqi.weather.util.JsonUtil;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class Auto_Update_Service extends Service {
    private Weather weather;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        AlarmManager manager=(AlarmManager)getSystemService(ALARM_SERVICE);
        int anHour=2*60*60*1000;
        long triggerAtTime= SystemClock.elapsedRealtime()+anHour;
        Intent i=new Intent(this,Auto_Update_Service.class);
        PendingIntent pi=PendingIntent.getService(this,0,i,0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
        return super.onStartCommand(intent, flags, startId);
    }

    private void updateWeather(){
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString=prefs.getString("weather",null);
        if (weatherString!=null){
            weather= JsonUtil.handleWeatherResponse(weatherString);
            final String weatherId=weather.basic.id;
            String weatherUrl="http://guolin.tech/api/weather?cityid="+weatherId+
                    "&key=3ea654508e9646c485c36a0346677ee6";
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText=response.body().string();
                    Weather weather=JsonUtil.handleWeatherResponse(responseText);
                    if(weather!=null&&"ok".equals(weather.status)){
                        SharedPreferences.Editor editor=PreferenceManager.
                                getDefaultSharedPreferences(Auto_Update_Service.this).edit();
                        editor.putString("weather",responseText);
                        editor.apply();
                        Notification notification=new Notification.Builder(Auto_Update_Service.this)
                                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.logo))
                                .setSmallIcon(R.mipmap.logo)
                                .setContentTitle("天气     "+weather.basic.update.loc.split(" ")[1])
                                .setContentText(weather.now.tmp+"℃ "+weather.now.cond.txt+
                                        "     "+weather.basic.parent_city+"-"+weather.basic.city)
                                .setShowWhen(false)
                                .build();
                        startForeground(1,notification);
                    }
                }
            });
        }
    }
}

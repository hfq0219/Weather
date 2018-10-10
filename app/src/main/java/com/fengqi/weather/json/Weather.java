package com.fengqi.weather.json;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 和风天气返回的天气json数据格式：
 * {
 *     "HeWeather":[
 *         {
 *             "status": "ok",
 *             "basic": {},
 *             "aqi": {},
 *             "now": {},
 *             "suggestion": {},
 *             "daily_forecast": []
 *         }
 *     ]
 * }
 */

public class Weather {
    public String status;
    public Basic basic;
    public Aqi aqi;
    public Now now;
    public Suggestion suggestion;
    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;
}

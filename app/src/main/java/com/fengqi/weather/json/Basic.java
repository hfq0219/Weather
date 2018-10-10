package com.fengqi.weather.json;


public class Basic {
    public String admin_area;
    public String city; //地级市
    public String id; //城市对应得天气ID
    public Update update; //
    public class Update{
        public String loc; //天气更新时间
    }
}

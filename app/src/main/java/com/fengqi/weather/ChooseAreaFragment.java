package com.fengqi.weather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fengqi.weather.database.City;
import com.fengqi.weather.database.Country;
import com.fengqi.weather.database.Province;
import com.fengqi.weather.util.HttpUtil;
import com.fengqi.weather.util.JsonUtil;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTRY=2;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList=new ArrayList<>();

    private List<Province> provinceList;
    private List<City> cityList;
    private List<Country> countryList;

    private Province selectedProvince;
    private City selectedCity;

    public static int currentLevel;
    //初始化布局和控件
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.choose_area,container,false);
        titleText=view.findViewById(R.id.title_text);
        backButton=view.findViewById(R.id.back_button);
        listView=view.findViewById(R.id.list_view);
        adapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }
    //注册点击事件监听器，跳转
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel==LEVEL_PROVINCE){
                    selectedProvince=provinceList.get(position);
                    loadCities();
                }else if(currentLevel==LEVEL_CITY){
                    selectedCity=cityList.get(position);
                    loadCountries();
                }else if(currentLevel==LEVEL_COUNTRY){
                    final String weatherId=countryList.get(position).getWeatherId();
                    loadProvinces();
                    if(getActivity() instanceof Main_Activity) {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){
                        WeatherActivity activity=(WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefresh.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel==LEVEL_COUNTRY){
                    loadCities();
                }else if(currentLevel==LEVEL_CITY){
                    loadProvinces();
                }
            }
        });
        loadProvinces();
    }
    //查询全国所有的省份名字，优先查数据库，没有再从服务器查
    private void loadProvinces(){
        backButton.setVisibility(View.GONE);
        provinceList= LitePal.findAll(Province.class);
        if(provinceList.size()>0){
            dataList.clear();
            for(Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            titleText.setText("中国");
            adapter.notifyDataSetChanged();
            currentLevel=LEVEL_PROVINCE;
        }else{
            String address="http://guolin.tech/api/china";
            loadFromServer(address,"province");
        }
    }
    //查询省内所有市的名字，优先查询数据库，没有再从服务器查
    private void loadCities(){
        cityList=LitePal.where("provinceid = ?",
                String.valueOf(selectedProvince.getProvinceCode())).find(City.class);
        if(cityList.size()>0){
            dataList.clear();
            for(City city:cityList){
                dataList.add(city.getCityName());
            }
            backButton.setVisibility(View.VISIBLE);
            titleText.setText(selectedProvince.getProvinceName());
            adapter.notifyDataSetChanged();
            currentLevel=LEVEL_CITY;
        }else{
            int provinceCode=selectedProvince.getProvinceCode();
            String address="http://guolin.tech/api/china/"+provinceCode;
            loadFromServer(address,"city");
        }
    }
    //查询市内所有县的名字，优先查询数据库，没有再从服务器查
    private void loadCountries(){
        countryList=LitePal.where("cityid = ?",
                String.valueOf(selectedCity.getCityCode())).find(Country.class);
        if(countryList.size()>0){
            dataList.clear();
            for(Country country:countryList){
                dataList.add(country.getCountryName());
            }
            backButton.setVisibility(View.VISIBLE);
            titleText.setText(selectedProvince.getProvinceName()+"-"+selectedCity.getCityName());
            adapter.notifyDataSetChanged();
            currentLevel=LEVEL_COUNTRY;
        }else{
            int provinceCode=selectedProvince.getProvinceCode();
            int cityCode=selectedCity.getCityCode();
            String address="http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            loadFromServer(address,"country");
        }
    }
    //根据传入的地址和类型从服务器上查询省市县的名字
    private void loadFromServer(String address,final String type){
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"查询数据失败，请检查网络连接",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText=response.body().string();
                boolean result=false;
                if("province".equals(type)){
                    result= JsonUtil.handleProvinceResponse(responseText);
                }else if("city".equals(type)){
                    result=JsonUtil.handleCityResponse(responseText,selectedProvince.getProvinceCode());
                }else if("country".equals(type)){
                    result=JsonUtil.handleCountryResponse(responseText,selectedCity.getCityCode());
                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                loadProvinces();
                            }else if("city".equals(type)){
                                loadCities();
                            }else if("country".equals(type)){
                                loadCountries();
                            }
                        }
                    });
                }
            }
        });
    }

    private void showProgressDialog(){
        if(progressDialog==null){
            progressDialog=new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog(){
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }


}

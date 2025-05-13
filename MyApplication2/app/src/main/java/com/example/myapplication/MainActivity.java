package com.example.myapplication;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.WindowCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.qweather.sdk.bean.air.AirNowBean;
import com.qweather.sdk.bean.base.Code;
import com.qweather.sdk.bean.base.Lang;
import com.qweather.sdk.bean.warning.WarningBean;
import com.qweather.sdk.bean.weather.WeatherDailyBean;
import com.qweather.sdk.bean.weather.WeatherHourlyBean;
import com.qweather.sdk.bean.weather.WeatherNowBean;
import com.qweather.sdk.view.HeConfig;
import com.qweather.sdk.view.QWeather;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private SwipeRefreshLayout swipeRefreshLayout;
    TextView textView1, textView2, textView3, textView4, textView5;
    TextView textView_xq1, textView_xq2, textView_xq3, textView_xq4, textView_xq5, textView_xq6, textView_xq7;
    TextView textView_rq1, textView_rq2, textView_rq3, textView_rq4, textView_rq5, textView_rq6, textView_rq7;
    TextView textView_gdi1, textView_gdi2, textView_gdi3, textView_gdi4, textView_gdi5, textView_gdi6, textView_gdi7;
    TextView textView_time, textView_diq;
    ImageView ivCurrentWeatherIcon;
    ImageView ivForecastIcon1, ivForecastIcon2, ivForecastIcon3, ivForecastIcon4, ivForecastIcon5, ivForecastIcon6, ivForecastIcon7;
    TextView tvAqiValue, tvAqiCategory;
    TextView tvNoAlerts, tvAlertTitle, tvAlertText;
    LinearLayout llAirQuality, llWeatherAlerts;
    TextView expandButton;
    LinearLayout expandedDays;
    Button collapseButton;
    private com.example.myapplication.WeeklyChartView weeklyChartView;
    private Map<String, Map<String, String>> provinceAndCityMap;
    private String currentLocationId = "101250304";
    private String currentLocationName = "株洲县";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Timer timer;
    private final AtomicInteger pendingApiCallCount = new AtomicInteger(0);
    private static final int TOTAL_REFRESH_API_CALLS = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        initViews();
        initProvinceAndCityData();
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        setupSwipeRefresh();
        setupQWeatherSDK();
        if (textView_diq != null) {
            textView_diq.setOnClickListener(v -> showProvinceSelection());
        }
        startTimeDisplay();
        initialWeatherLoad();

        if (expandButton != null && expandedDays != null && collapseButton != null) {
            expandButton.setOnClickListener(v -> {
                if (expandedDays.getVisibility() == View.GONE) {
                    Animation slideIn = AnimationUtils.loadAnimation(this, R.drawable.slide_in); // Make sure R.drawable.slide_in is correct, should be R.anim.slide_in
                    expandedDays.setVisibility(View.VISIBLE);
                    expandedDays.startAnimation(slideIn);
                    expandButton.setVisibility(View.GONE);
                    collapseButton.setVisibility(View.VISIBLE);
                }
            });

            collapseButton.setOnClickListener(v -> {
                Animation slideOut = AnimationUtils.loadAnimation(this, R.drawable.slide_out); // Make sure R.drawable.slide_out is correct, should be R.anim.slide_out
                slideOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        expandedDays.setVisibility(View.GONE);
                        collapseButton.setVisibility(View.GONE);
                        expandButton.setVisibility(View.VISIBLE);
                    }
                    @Override
                    public void onAnimationStart(Animation animation) {}
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                expandedDays.startAnimation(slideOut);
            });
        }
    }

    private void initViews() {
        textView1 = findViewById(R.id.tv_weather);
        textView2 = findViewById(R.id.tv_temperature);
        textView3 = findViewById(R.id.tv_humidity);
        textView4 = findViewById(R.id.text_4);
        textView5 = findViewById(R.id.tempMax_1);
        ivCurrentWeatherIcon = findViewById(R.id.iv_current_weather_icon);
        textView_time = findViewById(R.id.Chinatime);
        textView_diq = findViewById(R.id.diqu_1);

        TextView[] temp_textView_rq_array = new TextView[7];
        ImageView[] temp_ivForecastIcon_array = new ImageView[7];
        TextView[] temp_textView_xq_array = new TextView[7];
        TextView[] temp_textView_gdi_array = new TextView[7];

        View forecastDay1_view = findViewById(R.id.forecast_day_1_include);
        View forecastDay2_view = findViewById(R.id.forecast_day_2_include);
        View forecastDay3_view = findViewById(R.id.forecast_day_3_include);
        View forecastDay4_view = findViewById(R.id.forecast_day_4_include);
        View forecastDay5_view = findViewById(R.id.forecast_day_5_include);
        View forecastDay6_view = findViewById(R.id.forecast_day_6_include);
        View forecastDay7_view = findViewById(R.id.forecast_day_7_include);

        View[] forecastDayViews_array = {
                forecastDay1_view, forecastDay2_view, forecastDay3_view,
                forecastDay4_view, forecastDay5_view, forecastDay6_view, forecastDay7_view
        };

        for (int i = 0; i < 7; i++) {
            if (forecastDayViews_array[i] != null) {
                temp_textView_rq_array[i] = forecastDayViews_array[i].findViewById(R.id.left_week_template);
                temp_ivForecastIcon_array[i] = forecastDayViews_array[i].findViewById(R.id.iv_forecast_icon_template);
                temp_textView_xq_array[i] = forecastDayViews_array[i].findViewById(R.id.weilaitq_template);
                temp_textView_gdi_array[i] = forecastDayViews_array[i].findViewById(R.id.right_week_template);
            } else {
                Log.e(TAG, "Forecast day view container " + (i + 1) + " (R.id.forecast_day_" + (i + 1) + "_include) is null in initViews!");
            }
        }

        textView_rq1 = temp_textView_rq_array[0]; textView_rq2 = temp_textView_rq_array[1]; textView_rq3 = temp_textView_rq_array[2];
        textView_rq4 = temp_textView_rq_array[3]; textView_rq5 = temp_textView_rq_array[4]; textView_rq6 = temp_textView_rq_array[5];
        textView_rq7 = temp_textView_rq_array[6];

        ivForecastIcon1 = temp_ivForecastIcon_array[0]; ivForecastIcon2 = temp_ivForecastIcon_array[1]; ivForecastIcon3 = temp_ivForecastIcon_array[2];
        ivForecastIcon4 = temp_ivForecastIcon_array[3]; ivForecastIcon5 = temp_ivForecastIcon_array[4]; ivForecastIcon6 = temp_ivForecastIcon_array[5];
        ivForecastIcon7 = temp_ivForecastIcon_array[6];

        textView_xq1 = temp_textView_xq_array[0]; textView_xq2 = temp_textView_xq_array[1]; textView_xq3 = temp_textView_xq_array[2];
        textView_xq4 = temp_textView_xq_array[3]; textView_xq5 = temp_textView_xq_array[4]; textView_xq6 = temp_textView_xq_array[5];
        textView_xq7 = temp_textView_xq_array[6];

        textView_gdi1 = temp_textView_gdi_array[0]; textView_gdi2 = temp_textView_gdi_array[1]; textView_gdi3 = temp_textView_gdi_array[2];
        textView_gdi4 = temp_textView_gdi_array[3]; textView_gdi5 = temp_textView_gdi_array[4]; textView_gdi6 = temp_textView_gdi_array[5];
        textView_gdi7 = temp_textView_gdi_array[6];

        weeklyChartView = findViewById(R.id.weekly_chart_view);
        tvAqiValue = findViewById(R.id.tv_aqi_value);
        tvAqiCategory = findViewById(R.id.tv_aqi_category);
        tvNoAlerts = findViewById(R.id.tv_no_alerts);
        tvAlertTitle = findViewById(R.id.tv_alert_title);
        tvAlertText = findViewById(R.id.tv_alert_text);
        llAirQuality = findViewById(R.id.ll_air_quality);
        llWeatherAlerts = findViewById(R.id.ll_weather_alerts);
        expandButton = findViewById(R.id.expand_button);
        expandedDays = findViewById(R.id.ll_expanded_days);

        if (expandButton != null && expandedDays != null) {
            collapseButton = new Button(this);
            collapseButton.setText("收起");
            collapseButton.setTextColor(ContextCompat.getColor(this, R.color.teal_200));
            collapseButton.setBackgroundResource(android.R.color.transparent);
            collapseButton.setVisibility(View.GONE);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, dpToPx(8), 0, dpToPx(8));
            expandedDays.addView(collapseButton, 0, params);
        } else {
            Log.e(TAG, "expandButton or expandedDays is null, cannot add collapseButton.");
        }
    }


    private void updateAirQualityUI(@Nullable AirNowBean.NowBean airData) {
        if (tvAqiValue == null || tvAqiCategory == null || llAirQuality == null) return;

        if (airData != null) {
            tvAqiValue.setText("AQI " + (airData.getAqi() != null ? airData.getAqi() : "--"));
            String category = airData.getCategory() != null ? airData.getCategory() : "未知";
            tvAqiCategory.setText(category);

            int backgroundColor = getAqiBackgroundColor(category);
            Drawable mutatedBackground = ContextCompat.getDrawable(this, R.drawable.edittext);
            if (mutatedBackground != null) {
                GradientDrawable background = (GradientDrawable) mutatedBackground.mutate();
                background.setColor(ColorUtils.setAlphaComponent(backgroundColor, 180));
                llAirQuality.setBackground(background);
            } else {
                llAirQuality.setBackgroundColor(backgroundColor);
            }

        } else {
            tvAqiValue.setText("AQI --");
            tvAqiCategory.setText("获取失败");
            llAirQuality.setBackgroundResource(R.drawable.edittext);
        }
    }

    private int getAqiBackgroundColor(String category) {
        int color = ContextCompat.getColor(this, R.color.aqi_default);
        if (category == null) return color;
        if (category.contains("优")) {
            color = ContextCompat.getColor(this, R.color.aqi_good);
        } else if (category.contains("良")) {
            color = ContextCompat.getColor(this, R.color.aqi_moderate);
        } else if (category.contains("轻度污染")) {
            color = ContextCompat.getColor(this, R.color.aqi_unhealthy_sensitive);
        } else if (category.contains("中度污染")) {
            color = ContextCompat.getColor(this, R.color.aqi_unhealthy);
        } else if (category.contains("重度污染")) {
            color = ContextCompat.getColor(this, R.color.aqi_very_unhealthy);
        } else if (category.contains("严重污染")) {
            color = ContextCompat.getColor(this, R.color.aqi_hazardous);
        }
        return color;
    }

    private void updateDailyForecastUI(List<WeatherDailyBean.DailyBean> dailyList) {
        if (dailyList == null || dailyList.isEmpty()) {
            clearForecastViews();
            return;
        }
        WeatherDailyBean.DailyBean today = dailyList.get(0);
        final String tempMax = today.getTempMax() + "°";
        final String tempMin = today.getTempMin() + "°";
        if (textView5 != null) textView5.setText(tempMax + "/" + tempMin);

        ImageView[] forecastIcons = {ivForecastIcon1, ivForecastIcon2, ivForecastIcon3, ivForecastIcon4, ivForecastIcon5, ivForecastIcon6, ivForecastIcon7};
        TextView[] dateTextViews = {textView_rq1, textView_rq2, textView_rq3, textView_rq4, textView_rq5, textView_rq6, textView_rq7};
        TextView[] weatherTextViews = {textView_xq1, textView_xq2, textView_xq3, textView_xq4, textView_xq5, textView_xq6, textView_xq7};
        TextView[] tempTextViews = {textView_gdi1, textView_gdi2, textView_gdi3, textView_gdi4, textView_gdi5, textView_gdi6, textView_gdi7};

        int daysToDisplay = Math.min(dailyList.size(), 7);
        for (int i = 0; i < daysToDisplay; i++) {
            if (dateTextViews[i] == null || weatherTextViews[i] == null || tempTextViews[i] == null || forecastIcons[i] == null) {
                Log.w(TAG, "Skipping update for forecast day " + (i+1) + " due to null view reference.");
                continue;
            }

            WeatherDailyBean.DailyBean dailyBean = dailyList.get(i);
            String fxDate = dailyBean.getFxDate();
            String formattedDate;
            String weekDay;
            try {
                SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
                Date date = sdfInput.parse(fxDate);
                SimpleDateFormat sdfOutput = new SimpleDateFormat("MM-dd", Locale.CHINA);
                formattedDate = sdfOutput.format(date);
                weekDay = getWeekDayFromDate(date);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing forecast date: " + fxDate, e);
                formattedDate = fxDate;
                weekDay = "日期";
            }
            String dateText = weekDay + "\n" + formattedDate;
            String weatherText = dailyBean.getTextDay();
            String highLowText = dailyBean.getTempMax() + "°/" + dailyBean.getTempMin() + "°";
            int forecastIconResId = getWeatherIconResource(weatherText);

            dateTextViews[i].setText(dateText);
            weatherTextViews[i].setText(weatherText);
            tempTextViews[i].setText(highLowText);
            forecastIcons[i].setImageResource(forecastIconResId);
            dateTextViews[i].setVisibility(TextView.VISIBLE);
            weatherTextViews[i].setVisibility(TextView.VISIBLE);
            tempTextViews[i].setVisibility(TextView.VISIBLE);
            forecastIcons[i].setVisibility(ImageView.VISIBLE);
        }
        for (int i = daysToDisplay; i < 7; i++) {
            if (dateTextViews[i] == null || weatherTextViews[i] == null || tempTextViews[i] == null || forecastIcons[i] == null) {
                Log.w(TAG, "Skipping hide for forecast day " + (i+1) + " due to null view reference.");
                continue;
            }
            dateTextViews[i].setVisibility(TextView.INVISIBLE);
            weatherTextViews[i].setVisibility(TextView.INVISIBLE);
            tempTextViews[i].setVisibility(TextView.INVISIBLE);
            forecastIcons[i].setVisibility(ImageView.INVISIBLE);
        }
        if (weeklyChartView != null) {
            if (daysToDisplay > 0) {
                List<WeatherDailyBean.DailyBean> chartData = dailyList.subList(0, daysToDisplay);
                weeklyChartView.setData(chartData);
            } else {
                weeklyChartView.setData(null);
            }
        }
    }

    private void clearForecastViews() {
        if (textView5 != null) textView5.setText("-°/-°");
        ImageView[] forecastIcons = {ivForecastIcon1, ivForecastIcon2, ivForecastIcon3, ivForecastIcon4, ivForecastIcon5, ivForecastIcon6, ivForecastIcon7};
        TextView[] dateTextViews = {textView_rq1, textView_rq2, textView_rq3, textView_rq4, textView_rq5, textView_rq6, textView_rq7};
        TextView[] weatherTextViews = {textView_xq1, textView_xq2, textView_xq3, textView_xq4, textView_xq5, textView_xq6, textView_xq7};
        TextView[] tempTextViews = {textView_gdi1, textView_gdi2, textView_gdi3, textView_gdi4, textView_gdi5, textView_gdi6, textView_gdi7};

        for (int i = 0; i < 7; i++) {
            if (dateTextViews[i] != null && weatherTextViews[i] != null && tempTextViews[i] != null && forecastIcons[i] != null) {
                dateTextViews[i].setText("-\n--");
                weatherTextViews[i].setText("-");
                tempTextViews[i].setText("-°/-°");
                forecastIcons[i].setImageResource(getWeatherIconResource(null));
            }
        }
        if (weeklyChartView != null) {
            weeklyChartView.setData(null);
        }
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "下拉刷新触发");
            if (isNetworkAvailable()) {
                Log.d(TAG, "开始刷新，需要完成 " + TOTAL_REFRESH_API_CALLS + " 个 API 调用");
                pendingApiCallCount.set(TOTAL_REFRESH_API_CALLS);
                fetchAllWeatherData();
            } else {
                Toast.makeText(MainActivity.this, "网络不可用，无法刷新", Toast.LENGTH_SHORT).show();
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    private void setupQWeatherSDK() {
        HeConfig.init("TG5A63RCBJ", "2e72f3dfa8a44362b4dd4187d6b25cf5");
        HeConfig.switchToDevService();
    }

    private void initialWeatherLoad() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "网络连接不可用，请检查网络设置", Toast.LENGTH_LONG).show();
            if (textView4 != null) textView4.setText("网络连接不可用");
            clearForecastViews();
            updateAirQualityUI(null);
            updateWeatherAlertsUI(null);
        } else {
            if (textView_diq != null) textView_diq.setText(currentLocationName);
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
            pendingApiCallCount.set(TOTAL_REFRESH_API_CALLS);
            fetchAllWeatherData();
        }
    }

    private void fetchAllWeatherData() {
        Log.d(TAG, "开始获取所有天气数据 (5项)，城市 ID: " + currentLocationId);
        getRealtimeWeather();
        getDailyWeatherForecast();
        getHourlyWeatherForecast();
        getAirQualityData();
        getWeatherAlerts();
    }

    private synchronized void onApiCallComplete() {
        int remaining = pendingApiCallCount.decrementAndGet();
        Log.d(TAG, "一个 API 调用完成，剩余: " + remaining);
        if (remaining <= 0) {
            handler.post(() -> {
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                    Log.d(TAG, "所有 API 调用完成，停止刷新动画");
                }
            });
        }
    }

    private void getHourlyWeatherForecast() {
        Log.d(TAG, "开始获取 24 小时预报 for " + currentLocationId);
        QWeather.getWeather24Hourly(this, currentLocationId, new QWeather.OnResultWeatherHourlyListener() {
            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "获取24小时天气预报失败: " + e.getMessage(), e);
                handler.post(() -> {
                    Toast.makeText(MainActivity.this, "获取24小时预报失败", Toast.LENGTH_SHORT).show();
                    displayHourlyWeather(new ArrayList<>());
                    onApiCallComplete();
                });
            }
            @Override
            public void onSuccess(WeatherHourlyBean weatherHourlyBean) {
                Log.i(TAG, "获取24小时预报成功 - Code: " + weatherHourlyBean.getCode());
                handler.post(() -> {
                    if (Code.OK == weatherHourlyBean.getCode()) {
                        List<WeatherHourlyBean.HourlyBean> hourlyList = weatherHourlyBean.getHourly();
                        if (hourlyList != null) {
                            displayHourlyWeather(hourlyList);
                        } else {
                            Log.w(TAG,"24小时预报数据列表为 null");
                            displayHourlyWeather(new ArrayList<>());
                        }
                    } else {
                        Log.e(TAG, "24小时预报API返回错误码: " + weatherHourlyBean.getCode());
                        Toast.makeText(MainActivity.this, "24小时预报错误:" + weatherHourlyBean.getCode(), Toast.LENGTH_SHORT).show();
                        displayHourlyWeather(new ArrayList<>());
                    }
                    onApiCallComplete();
                });
            }
        });
    }

    private void getRealtimeWeather() {
        Log.d(TAG, "开始获取实时天气 for " + currentLocationId);
        QWeather.getWeatherNow(this, currentLocationId, new QWeather.OnResultWeatherNowListener() {
            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "获取实时天气数据失败: " + e.getMessage(), e);
                handler.post(() -> {
                    if (textView4 != null) textView4.setText("获取实时天气失败");
                    if (textView1 != null) textView1.setText("-");
                    if (textView2 != null) textView2.setText("-°");
                    if (textView3 != null) textView3.setText("-%");
                    if (ivCurrentWeatherIcon != null) ivCurrentWeatherIcon.setImageResource(getWeatherIconResource(null));
                    Toast.makeText(MainActivity.this, "获取实时天气失败", Toast.LENGTH_SHORT).show();
                    onApiCallComplete();
                });
            }

            @Override
            public void onSuccess(WeatherNowBean weatherBean) {
                Log.i(TAG, "获取实时天气成功 - Code: " + weatherBean.getCode());
                handler.post(() -> {
                    if (Code.OK == weatherBean.getCode() && weatherBean.getNow() != null) {
                        WeatherNowBean.NowBaseBean now = weatherBean.getNow();
                        final String weatherText = now.getText();
                        final String temperature = now.getTemp() + "°";
                        final String humidity = now.getHumidity() + "%";
                        final String updateTime = now.getObsTime();
                        final int iconResId = getWeatherIconResource(weatherText);

                        if (textView1 != null) textView1.setText(weatherText);
                        if (textView2 != null) textView2.setText(temperature);
                        if (textView3 != null) textView3.setText(humidity);
                        if (textView4 != null) textView4.setText(formatUpdateTime(updateTime));
                        if (ivCurrentWeatherIcon != null) ivCurrentWeatherIcon.setImageResource(iconResId);
                    } else {
                        Code code = weatherBean.getCode();
                        Log.e(TAG, "实时天气API返回错误码: " + code);
                        if (textView1 != null) textView1.setText("-");
                        if (textView2 != null) textView2.setText("-°");
                        if (textView3 != null) textView3.setText("-%");
                        if (ivCurrentWeatherIcon != null) ivCurrentWeatherIcon.setImageResource(getWeatherIconResource(null));
                        Toast.makeText(MainActivity.this, "实时天气错误:" + code, Toast.LENGTH_SHORT).show();
                    }
                    onApiCallComplete();
                });
            }
        });
    }

    private void getDailyWeatherForecast() {
        Log.d(TAG, "开始获取 7 天预报 for " + currentLocationId);
        QWeather.getWeather7D(this, currentLocationId, new QWeather.OnResultWeatherDailyListener() {
            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "获取天气预报数据失败: " + e.getMessage(), e);
                handler.post(() -> {
                    Toast.makeText(MainActivity.this, "获取天气预报失败", Toast.LENGTH_SHORT).show();
                    clearForecastViews();
                    onApiCallComplete();
                });
            }

            @Override
            public void onSuccess(WeatherDailyBean weatherDailyBean) {
                Log.i(TAG, "获取7天预报成功 - Code: " + weatherDailyBean.getCode());
                handler.post(() -> {
                    if (Code.OK == weatherDailyBean.getCode()) {
                        final List<WeatherDailyBean.DailyBean> dailyList = weatherDailyBean.getDaily();
                        if (dailyList != null && !dailyList.isEmpty()) {
                            updateDailyForecastUI(dailyList);
                        } else {
                            Log.w(TAG, "7天预报数据列表为空或null");
                            Toast.makeText(MainActivity.this, "未获取到7天预报数据", Toast.LENGTH_SHORT).show();
                            clearForecastViews();
                        }
                    } else {
                        Code code = weatherDailyBean.getCode();
                        Log.e(TAG, "7天预报API返回错误码: " + code);
                        Toast.makeText(MainActivity.this, "7天预报错误:" + code, Toast.LENGTH_SHORT).show();
                        clearForecastViews();
                    }
                    onApiCallComplete();
                });
            }
        });
    }

    private void getAirQualityData() {
        Log.d(TAG, "开始获取空气质量 for " + currentLocationId);
        QWeather.getAirNow(this, currentLocationId, Lang.ZH_HANS, new QWeather.OnResultAirNowListener() {
            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "获取空气质量失败: " + e.getMessage(), e);
                handler.post(() -> {
                    Toast.makeText(MainActivity.this, "获取空气质量失败", Toast.LENGTH_SHORT).show();
                    updateAirQualityUI(null);
                    onApiCallComplete();
                });
            }

            @Override
            public void onSuccess(AirNowBean airNowBean) {
                Log.i(TAG, "获取空气质量成功 - Code: " + airNowBean.getCode());
                handler.post(() -> {
                    if (Code.OK == airNowBean.getCode() && airNowBean.getNow() != null) {
                        updateAirQualityUI(airNowBean.getNow());
                    } else {
                        Code code = airNowBean.getCode();
                        Log.e(TAG, "空气质量API返回错误码: " + code);
                        Toast.makeText(MainActivity.this, "空气质量错误:" + code, Toast.LENGTH_SHORT).show();
                        updateAirQualityUI(null);
                    }
                    onApiCallComplete();
                });
            }
        });
    }

    private void getWeatherAlerts() {
        Log.d(TAG, "开始获取天气预警 for " + currentLocationId);
        QWeather.getWarning(this, currentLocationId, new QWeather.OnResultWarningListener() {
            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "获取天气预警失败: " + e.getMessage(), e);
                handler.post(() -> {
                    Toast.makeText(MainActivity.this, "获取天气预警失败", Toast.LENGTH_SHORT).show();
                    updateWeatherAlertsUI(null);
                    onApiCallComplete();
                });
            }

            @Override
            public void onSuccess(WarningBean warningBean) {
                Log.i(TAG, "获取天气预警成功 - Code: " + warningBean.getCode());
                handler.post(() -> {
                    if (Code.OK == warningBean.getCode()) {
                        List<WarningBean.WarningBeanBase> warningList = warningBean.getWarningList();
                        updateWeatherAlertsUI(warningList);
                    } else {
                        Code code = warningBean.getCode();
                        Log.e(TAG, "天气预警API返回错误码: " + code);
                        Toast.makeText(MainActivity.this, "天气预警错误:" + code, Toast.LENGTH_SHORT).show();
                        updateWeatherAlertsUI(null);
                    }
                    onApiCallComplete();
                });
            }
        });
    }

    private void updateWeatherAlertsUI(@Nullable List<WarningBean.WarningBeanBase> warnings) {
        if (tvNoAlerts == null || tvAlertTitle == null || tvAlertText == null) return;

        if (warnings == null) {
            tvNoAlerts.setVisibility(View.VISIBLE);
            tvNoAlerts.setText("获取预警失败");
            tvAlertTitle.setVisibility(View.GONE);
            tvAlertText.setVisibility(View.GONE);
        } else if (warnings.isEmpty()) {
            tvNoAlerts.setVisibility(View.VISIBLE);
            tvNoAlerts.setText("暂无预警");
            tvAlertTitle.setVisibility(View.GONE);
            tvAlertText.setVisibility(View.GONE);
        } else {
            tvNoAlerts.setVisibility(View.GONE);
            tvAlertTitle.setVisibility(View.VISIBLE);
            tvAlertText.setVisibility(View.VISIBLE);

            WarningBean.WarningBeanBase firstWarning = warnings.get(0);
            String typeName = firstWarning.getTypeName();
            String severity = firstWarning.getSeverity();
            String textContent = firstWarning.getText();
            String titleConstructed = (typeName != null ? typeName : "")
                    + " "
                    + (severity != null ? severity : "")
                    + "预警";
            tvAlertTitle.setText(titleConstructed.trim());
            String textToDisplay = textContent != null ? textContent : "详情未知";
            tvAlertText.setText(textToDisplay);
        }
    }

    private String formatUpdateTime(String obsTime) {
        if (obsTime == null) return "未知时间";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmXXX", Locale.CHINA);
            inputFormat.setLenient(false);
            Date date = inputFormat.parse(obsTime);
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm '更新'", Locale.CHINA);
            return outputFormat.format(date);
        } catch (Exception e) {
            Log.e(TAG, "格式化更新时间失败: '" + obsTime + "'", e);
            int tIndex = obsTime.indexOf('T');
            int colonIndex = obsTime.indexOf(':', tIndex > 0 ? tIndex : 0);
            if (tIndex > 0 && colonIndex > tIndex + 1 && colonIndex + 3 <= obsTime.length()) {
                try {
                    return obsTime.substring(tIndex + 1, colonIndex + 3) + " 更新";
                } catch (Exception ignored) {}
            }
            return "时间解析失败";
        }
    }

    private int getWeatherIconResource(String weatherText) {
        if (weatherText == null) {
            return R.drawable.sun_2;
        }
        if (weatherText.contains("雷阵雨")) return R.drawable.leizyu;
        if (weatherText.contains("冰雹")) return R.drawable.bingbao;
        if (weatherText.contains("暴雨")) return R.drawable.baoyyu;
        if (weatherText.contains("大雨")) return R.drawable.dayu_1;
        if (weatherText.contains("中雨")) return R.drawable.xiaoyu_1;
        if (weatherText.contains("小雨")) return R.drawable.xiaoyu_2;
        if (weatherText.contains("雨")) return R.drawable.xiaoyu_2;
        if (weatherText.contains("大雪")) return R.drawable.zhengxue;
        if (weatherText.contains("中雪")) return R.drawable.xiuxuer_1;
        if (weatherText.contains("小雪")) return R.drawable.xue;
        if (weatherText.contains("雪")) return R.drawable.xiuxuer_1;
        if (weatherText.contains("霾")) return R.drawable.mai;
        if (weatherText.contains("沙尘")) return R.drawable.mai;
        if (weatherText.contains("雾")) return R.drawable.wu;
        if (weatherText.contains("多云")) return R.drawable.dyun_1;
        if (weatherText.contains("阴")) return R.drawable.yin;
        if (weatherText.contains("晴")) return R.drawable.sun_2;
        Log.w(TAG, "未找到匹配的天气图标: '" + weatherText + "', 使用默认图标。");
        return R.drawable.sun_2;
    }

    private void initProvinceAndCityData() {
        provinceAndCityMap = new HashMap<>();
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = getResources().openRawResource(R.raw.province_city);
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, Map<String, String>>>() {}.getType();
            provinceAndCityMap = gson.fromJson(jsonBuilder.toString(), mapType);
            if (provinceAndCityMap == null) {
                provinceAndCityMap = new HashMap<>();
                Log.e(TAG, "Gson 解析省市数据返回 null，请检查 province_city.json 文件格式或内容。");
                Toast.makeText(this, "地区数据格式错误或文件为空", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "地区数据从 JSON 加载成功，省份数量: " + provinceAndCityMap.size());
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "地区 JSON 文件 (R.raw.province_city) 未找到: " + e.getMessage(), e);
            Toast.makeText(this, "地区数据文件缺失!", Toast.LENGTH_LONG).show();
            provinceAndCityMap = Collections.emptyMap();
        } catch (IOException e) {
            Log.e(TAG, "读取地区数据文件失败: " + e.getMessage(), e);
            Toast.makeText(this, "加载地区数据失败", Toast.LENGTH_LONG).show();
            provinceAndCityMap = Collections.emptyMap();
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "解析地区 JSON 数据失败: " + e.getMessage(), e);
            Toast.makeText(this, "地区数据格式错误", Toast.LENGTH_LONG).show();
            provinceAndCityMap = Collections.emptyMap();
        } catch (Exception e) {
            Log.e(TAG, "初始化地区数据时发生未知错误: " + e.getMessage(), e);
            Toast.makeText(this, "加载地区数据时出错", Toast.LENGTH_LONG).show();
            provinceAndCityMap = Collections.emptyMap();
        } finally {
            try {
                if (reader != null) reader.close();
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "关闭省市数据文件流失败: " + e.getMessage(), e);
            }
        }
    }

    private void showProvinceSelection() {
        if (provinceAndCityMap == null || provinceAndCityMap.isEmpty()) {
            Log.w(TAG, "尝试显示省份选择时，省份数据为 null 或空。");
            Toast.makeText(this, "无法加载地区数据，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }
        final List<String> provinces = new ArrayList<>(provinceAndCityMap.keySet());
        Collections.sort(provinces);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择省份");
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, provinces);
        builder.setAdapter(adapter, (dialog, which) -> {
            String selectedProvince = provinces.get(which);
            Log.d(TAG, "已选择省份: " + selectedProvince);
            showCitySelection(selectedProvince);
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showCitySelection(final String province) {
        if (provinceAndCityMap == null || !provinceAndCityMap.containsKey(province)) {
            Log.e(TAG, "找不到所选省份的城市数据: " + province);
            Toast.makeText(this, "无法加载该省份的城市数据", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, String> cities = provinceAndCityMap.get(province);
        if (cities == null || cities.isEmpty()) {
            Log.e(TAG, "省份 " + province + " 的城市列表为空");
            Toast.makeText(this, "未找到省份 " + province + " 的城市数据", Toast.LENGTH_SHORT).show();
            return;
        }
        final List<String> cityNames = new ArrayList<>(cities.keySet());
        Collections.sort(cityNames);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择城市");
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, cityNames);
        builder.setAdapter(adapter, (dialog, which) -> {
            String selectedCity = cityNames.get(which);
            String cityId = cities.get(selectedCity);
            if (cityId == null || cityId.isEmpty()) {
                Log.e(TAG, "未找到城市 ID for: " + selectedCity + " in province " + province);
                Toast.makeText(this, "未找到城市ID: " + selectedCity, Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d(TAG, "已选择城市: " + selectedCity + ", ID: " + cityId);
            currentLocationId = cityId;
            currentLocationName = selectedCity;
            if (textView_diq != null) textView_diq.setText(currentLocationName);
            clearForecastViews();
            updateAirQualityUI(null);
            updateWeatherAlertsUI(null);
            if (textView4 != null) textView4.setText("加载中...");
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(true);
            }
            Log.d(TAG, "切换城市，开始刷新，需要完成 " + TOTAL_REFRESH_API_CALLS + " 个 API 调用");
            pendingApiCallCount.set(TOTAL_REFRESH_API_CALLS);
            fetchAllWeatherData();
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void startTimeDisplay() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> updateChineseTime());
            }
        }, 0, 60000);
    }

    private void updateChineseTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        String chinaTime = sdf.format(new Date());
        if (textView_time != null) {
            textView_time.setText(chinaTime);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        handler.removeCallbacksAndMessages(null);
    }

    private void displayHourlyWeather(List<WeatherHourlyBean.HourlyBean> hourlyList) {
        LinearLayout llHourlyWeather = findViewById(R.id.ll_hourly_weather);
        if (llHourlyWeather == null) {
            Log.e(TAG, "找不到用于显示逐小时预报的 LinearLayout (ll_hourly_weather)!");
            return;
        }
        llHourlyWeather.removeAllViews();
        if (hourlyList == null || hourlyList.isEmpty()) {
            Log.d(TAG, "逐小时预报列表为空或 null，显示无数据提示。");
            TextView tvNoData = new TextView(this);
            tvNoData.setText("暂无逐小时预报");
            tvNoData.setGravity(Gravity.CENTER);
            tvNoData.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, dpToPx(16), 0, dpToPx(16));
            llHourlyWeather.addView(tvNoData, params);
            return;
        }
        Log.d(TAG, "开始显示 " + hourlyList.size() + " 个逐小时预报项。");
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int itemWidth = screenWidth / 5;

        for (WeatherHourlyBean.HourlyBean hourly : hourlyList) {
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(itemWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            itemLayout.setLayoutParams(params);
            itemLayout.setPadding(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(8));

            TextView tvTime = new TextView(this);
            tvTime.setText(getHourFromDateTime(hourly.getFxTime()));
            tvTime.setGravity(Gravity.CENTER);
            tvTime.setTextSize(12);
            tvTime.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            itemLayout.addView(tvTime);

            ImageView ivHourlyIcon = new ImageView(this);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    dpToPx(28),
                    dpToPx(28)
            );
            iconParams.setMargins(0, dpToPx(4), 0, dpToPx(4));
            ivHourlyIcon.setLayoutParams(iconParams);
            int iconRes = getWeatherIconResource(hourly.getText());
            ivHourlyIcon.setImageResource(iconRes);
            ivHourlyIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            itemLayout.addView(ivHourlyIcon);

            TextView tvTemp = new TextView(this);
            tvTemp.setText(hourly.getTemp() + "°");
            tvTemp.setGravity(Gravity.CENTER);
            tvTemp.setTextSize(14);
            tvTemp.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTemp.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            itemLayout.addView(tvTemp);

            llHourlyWeather.addView(itemLayout);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String getHourFromDateTime(String dateTime) {
        if (dateTime == null) return "--:--";
        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmXXX", Locale.CHINA);
            sdfInput.setLenient(false);
            Date date = sdfInput.parse(dateTime);
            SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm", Locale.CHINA);
            return hourFormat.format(date);
        } catch (Exception e) {
            Log.w(TAG, "解析小时预报时间失败: " + dateTime, e);
            int tIndex = dateTime.indexOf('T');
            if (tIndex > 0 && tIndex + 6 <= dateTime.length()) {
                try { return dateTime.substring(tIndex + 1, tIndex + 6); }
                catch (Exception ignored) {}
            }
            return "--:--";
        }
    }

    private String getWeekDayFromDate(Date date) {
        if (date == null) return "";
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        Calendar today = Calendar.getInstance();
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            return "今天";
        } else if (calendar.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)) {
            return "明天";
        } else {
            SimpleDateFormat weekdayFormat = new SimpleDateFormat("E", Locale.CHINA);
            return weekdayFormat.format(date);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            Log.w(TAG, "无法获取 ConnectivityManager 服务。");
            return false;
        }
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        Log.d(TAG, "网络是否可用: " + isConnected);
        return isConnected;
    }
}
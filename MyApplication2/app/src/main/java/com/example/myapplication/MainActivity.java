package com.example.myapplication;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color; // Make sure Color is imported
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet; // Needed for WeeklyChartView constructor if inflating from XML in code
import android.util.Log;
import android.view.Gravity;
import android.view.View; // Import View
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable; // Import Nullable
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat; // For Insets later if needed
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat; // For Insets later if needed
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

// Import the custom view
import com.example.myapplication.WeeklyChartView; // Adjust package if needed

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.qweather.sdk.bean.base.Code;
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

    // --- UI Controls ---
    TextView textView1, textView2, textView3, textView4, textView5;
    TextView textView_xq1, textView_xq2, textView_xq3, textView_xq4, textView_xq5, textView_xq6, textView_xq7;
    TextView textView_rq1, textView_rq2, textView_rq3, textView_rq4, textView_rq5, textView_rq6, textView_rq7;
    TextView textView_gdi1, textView_gdi2, textView_gdi3, textView_gdi4, textView_gdi5, textView_gdi6, textView_gdi7;
    TextView textView_time, textView_diq;
    ImageView ivCurrentWeatherIcon;
    ImageView ivForecastIcon1, ivForecastIcon2, ivForecastIcon3, ivForecastIcon4, ivForecastIcon5, ivForecastIcon6, ivForecastIcon7;

    // --- Custom Chart View ---
    private WeeklyChartView weeklyChartView; // Member variable for the custom chart

    // --- Data ---
    private Map<String, Map<String, String>> provinceAndCityMap;
    private String currentLocationId = "101250304";
    private String currentLocationName = "株洲县";

    // --- Async & Timers ---
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Timer timer;
    private final AtomicInteger pendingApiCallCount = new AtomicInteger(0);
    private static final int TOTAL_REFRESH_API_CALLS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set window flags for transparent status bar BEFORE setting content view
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        // Initialize all views, including the custom chart
        initViews();
        initProvinceAndCityData();
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        setupSwipeRefresh();
        setupQWeatherSDK();
        textView_diq.setOnClickListener(v -> showProvinceSelection());
        startTimeDisplay();
        initialWeatherLoad();

        // Optional: Handle Insets to add padding below status bar
        // setupInsets();
    }

    /*
    // Optional method to handle insets and apply padding dynamically
    private void setupInsets() {
        View rootView = findViewById(android.R.id.content); // Or your main container ID
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
                InsetsCompat insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

                // Apply top padding to your main content container (e.g., the RelativeLayout)
                // to avoid overlapping with the status bar
                View contentContainer = findViewById(R.id.your_main_content_container_id); // Replace with actual ID
                 if (contentContainer != null) {
                     contentContainer.setPadding(contentContainer.getPaddingLeft(),
                                                 insets.top, // Use status bar height as top padding
                                                 contentContainer.getPaddingRight(),
                                                 contentContainer.getPaddingBottom());
                 }

                // Apply bottom padding if you also made the navigation bar transparent
                // if (contentContainer != null) {
                //     contentContainer.setPadding(contentContainer.getPaddingLeft(),
                //                                 insets.top,
                //                                 contentContainer.getPaddingRight(),
                //                                 insets.bottom); // Use navigation bar height as bottom padding
                // }


                // Consume the insets so lower views don't also react (usually)
                // return WindowInsetsCompat.CONSUMED;
                 // Or return original insets if other views need them
                 return windowInsets;
            });
        }
    }
    */


    /**
     * Initialize all views, including the custom chart.
     */
    private void initViews() {
        textView1 = findViewById(R.id.tv_weather);
        textView2 = findViewById(R.id.tv_temperature);
        textView3 = findViewById(R.id.tv_humidity);
        textView4 = findViewById(R.id.text_4);
        textView5 = findViewById(R.id.tempMax_1);
        ivCurrentWeatherIcon = findViewById(R.id.iv_current_weather_icon);
        textView_xq1 = findViewById(R.id.weilaitq_1);
        textView_xq2 = findViewById(R.id.weilaitq_2);
        textView_xq3 = findViewById(R.id.weilaitq_3);
        textView_xq4 = findViewById(R.id.weilaitq_4);
        textView_xq5 = findViewById(R.id.weilaitq_5);
        textView_xq6 = findViewById(R.id.weilaitq_6);
        textView_xq7 = findViewById(R.id.weilaitq_7);
        textView_time = findViewById(R.id.Chinatime);
        textView_diq = findViewById(R.id.diqu_1);
        textView_rq1 = findViewById(R.id.left_week1);
        textView_rq2 = findViewById(R.id.left_week2);
        textView_rq3 = findViewById(R.id.left_week3);
        textView_rq4 = findViewById(R.id.left_week4);
        textView_rq5 = findViewById(R.id.left_week5);
        textView_rq6 = findViewById(R.id.left_week6);
        textView_rq7 = findViewById(R.id.left_week7);
        textView_gdi1 = findViewById(R.id.right_week1);
        textView_gdi2 = findViewById(R.id.right_week2);
        textView_gdi3 = findViewById(R.id.right_week3);
        textView_gdi4 = findViewById(R.id.right_week4);
        textView_gdi5 = findViewById(R.id.right_week5);
        textView_gdi6 = findViewById(R.id.right_week6);
        textView_gdi7 = findViewById(R.id.right_week7);
        ivForecastIcon1 = findViewById(R.id.iv_forecast_icon_1);
        ivForecastIcon2 = findViewById(R.id.iv_forecast_icon_2);
        ivForecastIcon3 = findViewById(R.id.iv_forecast_icon_3);
        ivForecastIcon4 = findViewById(R.id.iv_forecast_icon_4);
        ivForecastIcon5 = findViewById(R.id.iv_forecast_icon_5);
        ivForecastIcon6 = findViewById(R.id.iv_forecast_icon_6);
        ivForecastIcon7 = findViewById(R.id.iv_forecast_icon_7);

        // --- Initialize the custom chart view ---
        weeklyChartView = findViewById(R.id.weekly_chart_view); // Use the ID from XML
    }

    /**
     * Update UI for daily forecast, including passing data to the chart.
     * @param dailyList List of daily forecast data.
     */
    private void updateDailyForecastUI(List<WeatherDailyBean.DailyBean> dailyList) {
        if (dailyList == null || dailyList.isEmpty()) {
            clearForecastViews(); // Clear views and chart if data is invalid
            return;
        }

        // Update today's high/low temp
        WeatherDailyBean.DailyBean today = dailyList.get(0);
        final String tempMax = today.getTempMax() + "°";
        final String tempMin = today.getTempMin() + "°";
        textView5.setText(tempMax + "/" + tempMin);

        // Prepare UI arrays
        ImageView[] forecastIcons = {ivForecastIcon1, ivForecastIcon2, ivForecastIcon3, ivForecastIcon4, ivForecastIcon5, ivForecastIcon6, ivForecastIcon7};
        TextView[] dateTextViews = {textView_rq1, textView_rq2, textView_rq3, textView_rq4, textView_rq5, textView_rq6, textView_rq7};
        TextView[] weatherTextViews = {textView_xq1, textView_xq2, textView_xq3, textView_xq4, textView_xq5, textView_xq6, textView_xq7};
        TextView[] tempTextViews = {textView_gdi1, textView_gdi2, textView_gdi3, textView_gdi4, textView_gdi5, textView_gdi6, textView_gdi7};

        int daysToDisplay = Math.min(dailyList.size(), 7); // Calculate actual days to display

        // Update forecast list items
        for (int i = 0; i < daysToDisplay; i++) {
            WeatherDailyBean.DailyBean dailyBean = dailyList.get(i);
            String fxDate = dailyBean.getFxDate();
            String formattedDate;
            String weekDay;

            try {
                SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
                Date date = sdfInput.parse(fxDate);
                SimpleDateFormat sdfOutput = new SimpleDateFormat("MM-dd", Locale.CHINA);
                formattedDate = sdfOutput.format(date);
                weekDay = getWeekDayFromDate(date); // Use helper for weekday
            } catch (Exception e) {
                Log.e(TAG, "Error parsing forecast date: " + fxDate, e);
                formattedDate = fxDate; // Fallback
                weekDay = "日期";     // Fallback
            }

            String dateText = weekDay + "\n" + formattedDate; // Weekday and date on separate lines
            String weatherText = dailyBean.getTextDay();
            String highLowText = dailyBean.getTempMax() + "°/" + dailyBean.getTempMin() + "°";
            int forecastIconResId = getWeatherIconResource(weatherText);

            // Update UI elements safely
            if (i < dateTextViews.length) {
                dateTextViews[i].setText(dateText);
                weatherTextViews[i].setText(weatherText);
                tempTextViews[i].setText(highLowText);
                forecastIcons[i].setImageResource(forecastIconResId);
                // Make views visible
                dateTextViews[i].setVisibility(TextView.VISIBLE);
                weatherTextViews[i].setVisibility(TextView.VISIBLE);
                tempTextViews[i].setVisibility(TextView.VISIBLE);
                forecastIcons[i].setVisibility(ImageView.VISIBLE);
            }
        }

        // Hide unused forecast views
        for (int i = daysToDisplay; i < 7; i++) {
            if (i < dateTextViews.length) {
                dateTextViews[i].setVisibility(TextView.INVISIBLE);
                weatherTextViews[i].setVisibility(TextView.INVISIBLE);
                tempTextViews[i].setVisibility(TextView.INVISIBLE);
                forecastIcons[i].setVisibility(ImageView.INVISIBLE);
            }
        }

        // --- Pass data to the custom chart view ---
        if (weeklyChartView != null) {
            // Pass only the relevant number of days (up to 7)
            List<WeatherDailyBean.DailyBean> chartData = dailyList.subList(0, daysToDisplay);
            weeklyChartView.setData(chartData);
        }
    }

    /**
     * Clear forecast views and the custom chart.
     */
    private void clearForecastViews() {
        // Clear today's high/low placeholder
        textView5.setText("-°/-°");

        // Prepare UI arrays
        ImageView[] forecastIcons = {ivForecastIcon1, ivForecastIcon2, ivForecastIcon3, ivForecastIcon4, ivForecastIcon5, ivForecastIcon6, ivForecastIcon7};
        TextView[] dateTextViews = {textView_rq1, textView_rq2, textView_rq3, textView_rq4, textView_rq5, textView_rq6, textView_rq7};
        TextView[] weatherTextViews = {textView_xq1, textView_xq2, textView_xq3, textView_xq4, textView_xq5, textView_xq6, textView_xq7};
        TextView[] tempTextViews = {textView_gdi1, textView_gdi2, textView_gdi3, textView_gdi4, textView_gdi5, textView_gdi6, textView_gdi7};

        // Set placeholders for forecast list
        for (int i = 0; i < 7; i++) {
            if (i < dateTextViews.length) { // Boundary check
                dateTextViews[i].setText("-\n--");
                weatherTextViews[i].setText("-");
                tempTextViews[i].setText("-°/-°");
                forecastIcons[i].setImageResource(getWeatherIconResource(null));
            }
        }

        // --- Clear the custom chart view ---
        if (weeklyChartView != null) {
            weeklyChartView.setData(null); // Pass null or an empty list to clear data
        }
    }

    // --- Rest of the methods (setupSwipeRefresh, setupQWeatherSDK, initialWeatherLoad, fetchAllWeatherData, onApiCallComplete, getHourlyWeatherForecast, getRealtimeWeather, getDailyWeatherForecast, formatUpdateTime, getWeatherIconResource, initProvinceAndCityData, showProvinceSelection, showCitySelection, startTimeDisplay, updateChineseTime, onDestroy, displayHourlyWeather, dpToPx, getHourFromDateTime, getWeekDayFromDate, isNetworkAvailable) remain the same as your provided code ---
    // ↓↓↓ Paste all those methods here ↓↓↓

    /**
     * 设置下拉刷新监听器和样式
     */
    private void setupSwipeRefresh() {
        // 设置下拉刷新的监听器
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "下拉刷新触发");
            // 检查网络是否可用
            if (isNetworkAvailable()) {
                // 网络可用，重置 API 调用计数器
                Log.d(TAG, "开始刷新，需要完成 " + TOTAL_REFRESH_API_CALLS + " 个 API 调用");
                pendingApiCallCount.set(TOTAL_REFRESH_API_CALLS); // 重置计数器
                // 获取所有天气数据 (实时、7天、24小时)
                fetchAllWeatherData();
                // 注意：setRefreshing(true) 由 SwipeRefreshLayout 自动处理
            } else {
                // 网络不可用，提示用户并立即停止刷新动画
                Toast.makeText(MainActivity.this, "网络不可用，无法刷新", Toast.LENGTH_SHORT).show();
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });

        // 设置下拉刷新指示器的颜色方案
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    /**
     * 初始化和风天气 SDK 配置
     */
    private void setupQWeatherSDK() {
        // !!! 使用你自己的 Public ID 和 Key 替换 !!!
        // 考虑将密钥存储在更安全的地方（例如 build.gradle 或 local.properties）
        HeConfig.init("TG5A63RCBJ", "2e72f3dfa8a44362b4dd4187d6b25cf5"); // Replace with your keys
        // 切换到开发版服务（发布时应切换到switchToProductionService()）
        HeConfig.switchToDevService();
    }

    /**
     * 执行首次进入应用或城市切换后的初始天气数据加载
     */
    private void initialWeatherLoad() {
        // 检查网络连接
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "网络连接不可用，请检查网络设置", Toast.LENGTH_LONG).show();
            textView4.setText("网络连接不可用"); // 在更新时间处显示网络状态
            clearForecastViews(); // Clear views if network unavailable
        } else {
            // 网络可用，设置当前地区名称
            textView_diq.setText(currentLocationName);
            // 手动启动加载指示器，模拟刷新过程
            swipeRefreshLayout.setRefreshing(true);
            // 设置 API 调用计数器，以便初始加载完成后也能停止指示器
            pendingApiCallCount.set(TOTAL_REFRESH_API_CALLS);
            // 获取所有天气数据
            fetchAllWeatherData();
        }
    }

    /**
     * 统一调用所有获取天气数据的 API
     */
    private void fetchAllWeatherData() {
        Log.d(TAG, "开始获取所有天气数据，城市 ID: " + currentLocationId);
        getRealtimeWeather();       // 获取实时天气
        getDailyWeatherForecast();  // 获取 7 天预报
        getHourlyWeatherForecast(); // 获取 24 小时预报
    }

    /**
     * 每个 API 调用完成后的回调（无论成功或失败）。
     * 递减计数器，并在计数器归零时停止刷新动画。
     * 使用 AtomicInteger 保证线程安全。
     */
    private synchronized void onApiCallComplete() {
        int remaining = pendingApiCallCount.decrementAndGet(); // 原子递减并获取新值
        Log.d(TAG, "一个 API 调用完成，剩余: " + remaining);

        // 当计数器小于等于 0 时，表示所有 API 都已返回结果
        if (remaining <= 0) {
            // 确保停止刷新动画的操作在主线程执行
            handler.post(() -> {
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false); // 停止刷新动画
                    Log.d(TAG, "所有 API 调用完成，停止刷新动画");
                    // 可以考虑只在用户手动触发刷新时显示此 Toast
                    Toast.makeText(MainActivity.this, "刷新完成", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * 获取未来 24 小时逐小时天气预报
     */
    private void getHourlyWeatherForecast() {
        Log.d(TAG, "开始获取 24 小时预报 for " + currentLocationId);
        QWeather.getWeather24Hourly(this, currentLocationId, new QWeather.OnResultWeatherHourlyListener() {
            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "获取24小时天气预报失败: " + e.getMessage(), e);
                // 确保 UI 更新和计数器操作在主线程
                handler.post(() -> {
                    Toast.makeText(MainActivity.this, "获取24小时预报失败", Toast.LENGTH_SHORT).show();
                    displayHourlyWeather(new ArrayList<>()); // 显示空状态或占位符
                    onApiCallComplete(); // 标记此 API 调用完成
                });
            }

            @Override
            public void onSuccess(WeatherHourlyBean weatherHourlyBean) {
                Log.i(TAG, "获取24小时预报成功 - Code: " + weatherHourlyBean.getCode());
                // 确保 UI 更新和计数器操作在主线程
                handler.post(() -> {
                    if (Code.OK == weatherHourlyBean.getCode()) {
                        List<WeatherHourlyBean.HourlyBean> hourlyList = weatherHourlyBean.getHourly();
                        if (hourlyList != null) {
                            displayHourlyWeather(hourlyList); // 显示逐小时预报
                        } else {
                            Log.w(TAG,"24小时预报数据列表为 null");
                            displayHourlyWeather(new ArrayList<>()); // 显示空状态
                        }
                    } else {
                        Log.e(TAG, "24小时预报API返回错误码: " + weatherHourlyBean.getCode());
                        Toast.makeText(MainActivity.this, "24小时预报错误:" + weatherHourlyBean.getCode(), Toast.LENGTH_SHORT).show();
                        displayHourlyWeather(new ArrayList<>()); // 显示空状态
                    }
                    onApiCallComplete(); // 标记此 API 调用完成
                });
            }
        });
    }

    /**
     * 获取实时天气数据
     */
    private void getRealtimeWeather() {
        Log.d(TAG, "开始获取实时天气 for " + currentLocationId);
        QWeather.getWeatherNow(this, currentLocationId, new QWeather.OnResultWeatherNowListener() {
            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "获取实时天气数据失败: " + e.getMessage(), e);
                // 确保 UI 更新和计数器操作在主线程
                handler.post(() -> {
                    textView4.setText("获取实时天气失败");
                    textView1.setText("-");
                    textView2.setText("-°");
                    textView3.setText("-%");
                    ivCurrentWeatherIcon.setImageResource(getWeatherIconResource(null));
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

                        textView1.setText(weatherText);
                        textView2.setText(temperature);
                        textView3.setText(humidity);
                        textView4.setText(formatUpdateTime(updateTime));
                        ivCurrentWeatherIcon.setImageResource(iconResId);
                    } else {
                        Code code = weatherBean.getCode();
                        Log.e(TAG, "实时天气API返回错误码: " + code);
                        textView4.setText("实时天气错误: " + code);
                        textView1.setText("-");
                        textView2.setText("-°");
                        textView3.setText("-%");
                        ivCurrentWeatherIcon.setImageResource(getWeatherIconResource(null));
                        Toast.makeText(MainActivity.this, "实时天气错误:" + code, Toast.LENGTH_SHORT).show();
                    }
                    onApiCallComplete();
                });
            }
        });
    }

    /**
     * 获取未来 7 天天气预报
     */
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
                            updateDailyForecastUI(dailyList); // This now updates the chart too
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

    /**
     * 格式化从 API 获取的更新时间字符串 (ISO 8601格式)
     * @param obsTime API 返回的原始时间字符串，例如 "2023-10-27T15:40+08:00"
     * @return 格式化后的时间字符串，例如 "15:40 更新" 或错误提示
     */
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

    /**
     * 根据天气描述文本获取对应的天气图标资源 ID
     * @param weatherText 天气描述，例如 "晴", "多云", "小雨"
     * @return 对应的 drawable 资源 ID，如果没有匹配则返回默认图标
     */
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
        if (weatherText.contains("雾")) return R.drawable.wu;
        if (weatherText.contains("沙尘")) return R.drawable.mai;
        if (weatherText.contains("多云")) return R.drawable.dyun_1;
        if (weatherText.contains("阴")) return R.drawable.yin;
        if (weatherText.contains("晴")) return R.drawable.sun_2;
        Log.w(TAG, "未找到匹配的天气图标: '" + weatherText + "', 使用默认图标。");
        return R.drawable.sun_2;
    }

    /**
     * 从 R.raw.province_city 文件加载省市数据到 provinceAndCityMap
     */
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

    /**
     * 显示省份选择对话框
     */
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

    /**
     * 根据选择的省份显示城市选择对话框
     * @param province 选中的省份名称
     */
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
            textView_diq.setText(currentLocationName);
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

    /**
     * 启动定时器，用于更新界面上的时间显示
     */
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
        }, 0, 60000); // Update every minute
    }

    /**
     * 更新界面上的中国时间显示 (格式: MM-dd HH:mm)
     */
    private void updateChineseTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        String chinaTime = sdf.format(new Date());
        if (textView_time != null) {
            textView_time.setText(chinaTime);
        }
    }

    // 活动销毁时的回调
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        handler.removeCallbacksAndMessages(null);
    }

    /**
     * 动态创建并显示逐小时天气预报视图
     * @param hourlyList 逐小时预报数据列表
     */
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
            tvNoData.setTextColor(ContextCompat.getColor(this, android.R.color.white)); // White text
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
            tvTime.setTextColor(ContextCompat.getColor(this, android.R.color.white)); // White text
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
            tvTemp.setTextColor(ContextCompat.getColor(this, android.R.color.white)); // White text
            itemLayout.addView(tvTemp);

            llHourlyWeather.addView(itemLayout);
        }
    }

    /**
     * 辅助方法：将 dp 单位转换为像素 (px)
     * @param dp dp 值
     * @return 对应的像素值
     */
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * 辅助方法：从 ISO 8601 格式的日期时间字符串中提取小时和分钟 (HH:mm)
     * @param dateTime 输入的日期时间字符串，例如 "2023-10-27T15:00+08:00"
     * @return 格式化后的 "HH:mm" 字符串，或解析失败时返回 "--:--"
     */
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

    /**
     * 辅助方法：根据 Date 对象获取对应的星期描述 ("今天", "明天", 或 "周X")
     * @param date 需要判断的日期对象
     * @return 对应的星期字符串
     */
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


    /**
     * 检查设备当前网络是否可用
     * @return true 如果网络连接可用，否则 false
     */
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

    // ↑↑↑ End of MainActivity class ↑↑↑
}
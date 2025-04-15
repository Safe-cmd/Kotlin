package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.qweather.sdk.bean.weather.WeatherDailyBean;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeeklyChartView extends View {

    private static final String TAG = "WeeklyChartView";

    private List<WeatherDailyBean.DailyBean> dailyData;
    private Paint maxTempPaint;
    private Paint minTempPaint;
    private Paint uvPaint;
    private Paint axisPaint;
    private Paint textPaint;
    private Paint pointTextPaint; // This paint will now change color dynamically
    private Paint gridPaint;
    private Paint pointPaint;

    private Path maxTempPath;
    private Path minTempPath;
    private Path uvPath;

    private float minTempOverall = 0f;
    private float maxTempOverall = 40f;
    private float uvMaxOverall = 15f;

    private final int PADDING = 90;
    private final int X_LABEL_MARGIN_TOP = 25;
    private final int Y_LABEL_MARGIN_RIGHT = 15;
    private final int POINT_RADIUS = 8;
    private final float LABEL_OFFSET_ABOVE = 28f;
    private final float LABEL_OFFSET_BELOW = 24f;
    private final float FIRST_POINT_LABEL_HORIZONTAL_OFFSET = 20f;
    private final float MIN_TEMP_LABEL_HORIZONTAL_OFFSET = 15f;

    private String[] weekDayLabels = new String[0];

    public WeeklyChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaints();
        maxTempPath = new Path();
        minTempPath = new Path();
        uvPath = new Path();
    }

    private void initPaints() {
        maxTempPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maxTempPaint.setColor(Color.parseColor("#FF5722")); // OrangeRed
        maxTempPaint.setStyle(Paint.Style.STROKE);
        maxTempPaint.setStrokeWidth(5f);

        minTempPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        minTempPaint.setColor(Color.parseColor("#03A9F4")); // Light Blue
        minTempPaint.setStyle(Paint.Style.STROKE);
        minTempPaint.setStrokeWidth(5f);

        uvPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        uvPaint.setColor(Color.MAGENTA); // Purple/Magenta
        uvPaint.setStyle(Paint.Style.STROKE);
        uvPaint.setStrokeWidth(4f);
        uvPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{10,10}, 0));

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(Color.WHITE);
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(2f);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#80FFFFFF"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{5,5}, 0));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        pointTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // pointTextPaint.setColor(Color.WHITE); // No longer set a fixed color here
        pointTextPaint.setTextSize(26f);
        pointTextPaint.setTextAlign(Paint.Align.CENTER);

        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(List<WeatherDailyBean.DailyBean> data) {
        // ... (setData remains the same) ...
        if (data == null) {
            this.dailyData = new ArrayList<>();
        } else {
            this.dailyData = data.size() <= 7 ? data : data.subList(0, 7);
        }

        if (!this.dailyData.isEmpty()) {
            calculateTemperatureRange();
            prepareWeekDayLabels();
        } else {
            weekDayLabels = new String[0];
        }
        invalidate();
    }

    private void calculateTemperatureRange() {
        // ... (calculateTemperatureRange remains the same) ...
        if (dailyData == null || dailyData.isEmpty()) {
            minTempOverall = 0f;
            maxTempOverall = 40f;
            return;
        }
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (WeatherDailyBean.DailyBean day : dailyData) {
            try {
                float dayMin = Float.parseFloat(day.getTempMin());
                float dayMax = Float.parseFloat(day.getTempMax());
                if (dayMin < min) min = dayMin;
                if (dayMax > max) max = dayMax;
            } catch (NumberFormatException e) { /* ignore */ }
        }
        minTempOverall = (float) Math.floor(min - 2);
        maxTempOverall = (float) Math.ceil(max + 2);
        if (maxTempOverall == minTempOverall) {
            maxTempOverall += 4;
            minTempOverall -= 4;
        }
        Log.d(TAG, "Calculated Temp Range: " + minTempOverall + " to " + maxTempOverall);
    }

    private void prepareWeekDayLabels() {
        // ... (prepareWeekDayLabels remains the same) ...
        if (dailyData == null || dailyData.isEmpty()) {
            weekDayLabels = new String[0];
            return;
        }
        weekDayLabels = new String[dailyData.size()];
        for (int i = 0; i < dailyData.size(); i++) {
            weekDayLabels[i] = getWeekDayLabel(dailyData.get(i).getFxDate());
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dailyData == null || dailyData.isEmpty()) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.WHITE); // Ensure "No data" text is white
            canvas.drawText("无预报数据", getWidth() / 2f, getHeight() / 2f, textPaint);
            return;
        }

        int width = getWidth();
        int height = getHeight();
        float drawWidth = width - 2 * PADDING;
        float drawHeight = height - 2 * PADDING;
        float originX = PADDING;
        float originY = height - PADDING;
        float tempRange = maxTempOverall - minTempOverall;
        if (tempRange <= 0) tempRange = 1;

        // Draw axes
        canvas.drawLine(originX, originY, originX + drawWidth, originY, axisPaint); // X
        canvas.drawLine(originX, originY, originX, originY - drawHeight, axisPaint); // Y

        // --- Draw Y-axis Labels and Grid ---
        textPaint.setColor(Color.WHITE); // Ensure axis text is white
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(30f);
        int numGridLines = 5;
        for (int i = 0; i <= numGridLines; i++) {
            float tempValue = minTempOverall + (tempRange * i / numGridLines);
            float yPos = originY - (drawHeight * i / numGridLines);
            if (i > 0) {
                canvas.drawLine(originX, yPos, originX + drawWidth, yPos, gridPaint);
            }
            canvas.drawText(String.format(Locale.getDefault(), "%.0f°", tempValue),
                    originX - Y_LABEL_MARGIN_RIGHT,
                    yPos + (textPaint.getTextSize() / 3),
                    textPaint);
        }

        // --- Draw X-axis Labels ---
        textPaint.setColor(Color.WHITE); // Ensure axis text is white
        textPaint.setTextAlign(Paint.Align.CENTER);
        int numDays = dailyData.size();
        if (numDays == 0) return;
        float xStep = (numDays > 1) ? drawWidth / (numDays - 1) : drawWidth / 2;

        for (int i = 0; i < numDays; i++) {
            float xPos = originX + i * xStep;
            if (numDays == 1) xPos = originX + drawWidth / 2;
            String label = (i < weekDayLabels.length) ? weekDayLabels[i] : "";
            canvas.drawText(label, xPos, originY + X_LABEL_MARGIN_TOP + textPaint.getTextSize(), textPaint);
        }

        // --- Prepare Paths and Draw Points/Labels ---
        maxTempPath.reset();
        minTempPath.reset();
        uvPath.reset();

        boolean firstPointMax = true;
        boolean firstPointMin = true;
        boolean firstPointUv = true;
        boolean hasValidUv = false;

        Paint.FontMetrics fm = pointTextPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;
        float textDescent = fm.descent;

        for (int i = 0; i < numDays; i++) {
            WeatherDailyBean.DailyBean day = dailyData.get(i);
            float xPos = originX + i * xStep;
            if (numDays == 1) xPos = originX + drawWidth / 2;

            float textXPosAbove = xPos;
            float textXPosBelow = xPos;
            if (i == 0 && numDays > 1) {
                textXPosAbove = xPos + FIRST_POINT_LABEL_HORIZONTAL_OFFSET;
                textXPosBelow = xPos + FIRST_POINT_LABEL_HORIZONTAL_OFFSET;
            }

            try {
                // Calculate Y positions
                float maxTemp = Float.parseFloat(day.getTempMax());
                float minTemp = Float.parseFloat(day.getTempMin());
                float yMaxPos = originY - ((maxTemp - minTempOverall) / tempRange * drawHeight);
                float yMinPos = originY - ((minTemp - minTempOverall) / tempRange * drawHeight);
                yMaxPos = Math.max(PADDING, Math.min(yMaxPos, originY));
                yMinPos = Math.max(PADDING, Math.min(yMinPos, originY));

                // Add points to paths
                if (firstPointMax) { maxTempPath.moveTo(xPos, yMaxPos); firstPointMax = false; } else { maxTempPath.lineTo(xPos, yMaxPos); }
                if (firstPointMin) { minTempPath.moveTo(xPos, yMinPos); firstPointMin = false; } else { minTempPath.lineTo(xPos, yMinPos); }

                // Draw points
                pointPaint.setColor(maxTempPaint.getColor());
                canvas.drawCircle(xPos, yMaxPos, POINT_RADIUS, pointPaint);
                pointPaint.setColor(minTempPaint.getColor());
                canvas.drawCircle(xPos, yMinPos, POINT_RADIUS, pointPaint);

                // *** Draw Text Labels with Corresponding Line Colors ***
                pointTextPaint.setTextAlign(Paint.Align.CENTER); // Reset alignment for max/UV

                // Max Temp Label (Above Point)
                pointTextPaint.setColor(maxTempPaint.getColor()); // Set color to max temp line color
                String maxTempLabel = String.format(Locale.getDefault(), "%.0f°", maxTemp);
                float yMaxText = yMaxPos - POINT_RADIUS - LABEL_OFFSET_ABOVE;
                canvas.drawText(maxTempLabel, textXPosAbove, yMaxText, pointTextPaint);

                // Min Temp Label (Below AND to the RIGHT of the point)
                pointTextPaint.setColor(minTempPaint.getColor()); // Set color to min temp line color
                pointTextPaint.setTextAlign(Paint.Align.LEFT);    // Align left for horizontal offset
                String minTempLabel = String.format(Locale.getDefault(), "%.0f°", minTemp);
                float yMinTextBaseline = yMinPos + POINT_RADIUS + LABEL_OFFSET_BELOW + textHeight - textDescent;
                float xMinText = xPos + MIN_TEMP_LABEL_HORIZONTAL_OFFSET;
                canvas.drawText(minTempLabel, xMinText, yMinTextBaseline, pointTextPaint);
                // **********************************************************

                // --- Optional: Handle UV Index ---
                String uvIndexStr = day.getUvIndex(); // <-- Replace if needed
                boolean currentUvValid = false;
                float yUvPos = Float.NaN;

                if (uvIndexStr != null && !uvIndexStr.isEmpty()) {
                    try {
                        float uvValue = Float.parseFloat(uvIndexStr);
                        if (uvValue >= 0 && uvMaxOverall > 0) {
                            yUvPos = originY - ((uvValue / uvMaxOverall) * drawHeight);
                            yUvPos = Math.max(PADDING, Math.min(yUvPos, originY));
                            if (firstPointUv) { uvPath.moveTo(xPos, yUvPos); firstPointUv = false; } else { uvPath.lineTo(xPos, yUvPos); }
                            pointPaint.setColor(uvPaint.getColor());
                            canvas.drawCircle(xPos, yUvPos, POINT_RADIUS - 2, pointPaint);
                            hasValidUv = true;
                            currentUvValid = true;
                        }
                    } catch (NumberFormatException uvEx) { /* ignore */ }
                }

                // Draw UV Text Label (Above Point)
                if(currentUvValid && !Float.isNaN(yUvPos)){
                    pointTextPaint.setColor(uvPaint.getColor());      // Set color to UV line color
                    pointTextPaint.setTextAlign(Paint.Align.CENTER);  // Reset alignment
                    String uvLabel = String.format(Locale.getDefault(), "%.0f", Float.parseFloat(uvIndexStr));
                    float yUvText = yUvPos - (POINT_RADIUS - 2) - LABEL_OFFSET_ABOVE;
                    canvas.drawText(uvLabel, textXPosAbove, yUvText, pointTextPaint);
                }
                // *************************************************

            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing temperature for drawing point/label: " + day.getFxDate());
            }
        }

        // Draw the lines
        canvas.drawPath(maxTempPath, maxTempPaint);
        canvas.drawPath(minTempPath, minTempPaint);
        if (hasValidUv) {
            canvas.drawPath(uvPath, uvPaint);
        }

        // --- Draw Legend ---
        // ... (Legend drawing code remains the same) ...
        float legendY = PADDING / 2f;
        float legendXStart = PADDING;
        float legendSpacing = 40f;
        float legendLineLength = 50f;

        textPaint.setColor(Color.WHITE); // Ensure legend text is white
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(28f);

        canvas.drawLine(legendXStart, legendY, legendXStart + legendLineLength, legendY, maxTempPaint);
        canvas.drawText("最高温", legendXStart + legendLineLength + 10, legendY + textPaint.getTextSize()/3 , textPaint);
        legendXStart += legendLineLength + 10 + textPaint.measureText("最高温") + legendSpacing;

        canvas.drawLine(legendXStart, legendY, legendXStart + legendLineLength, legendY, minTempPaint);
        canvas.drawText("最低温", legendXStart + legendLineLength + 10, legendY + textPaint.getTextSize()/3, textPaint);
        legendXStart += legendLineLength + 10 + textPaint.measureText("最低温") + legendSpacing;

        if (hasValidUv) {
            canvas.drawLine(legendXStart, legendY, legendXStart + legendLineLength, legendY, uvPaint);
            canvas.drawText("紫外线", legendXStart + legendLineLength + 10, legendY + textPaint.getTextSize()/3, textPaint);
        }
    }


    // Helper to get weekday label
    private String getWeekDayLabel(String dateString) {
        // ... (getWeekDayLabel remains the same) ...
        if (dateString == null) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
            sdf.setLenient(false);
            Date date = sdf.parse(dateString);
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
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date string for label: " + dateString, e);
            return "错误";
        }
    }
}
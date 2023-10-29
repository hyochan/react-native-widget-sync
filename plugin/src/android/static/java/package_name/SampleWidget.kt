package com.example.app

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViewsService
import android.widget.RemoteViewsService.RemoteViewsFactory

data class TodoItem(
    val id: String,
    val content: String,
    val color: String
)

fun parseJsonString(jsonString: String): List<TodoItem> {
    return try {
        Gson().fromJson(jsonString, object : TypeToken<List<TodoItem>>() {}.type)
    } catch (e: Exception) {
        println("Decoding error: ${e.message}")
        emptyList()
    }
}

fun colorFromHexString(hex: String): Int {
    val hexSanitized = hex.trim().replace("#", "")
    return try {
        Color.parseColor("#$hexSanitized")
    } catch (e: IllegalArgumentException) {
        println("Invalid color format")
        Color.BLACK
    }
}

class ListViewService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return MyRemoteViewsFactory(this.applicationContext, intent)
    }
}

class MyRemoteViewsFactory(private val context: Context, intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    private var items: List<TodoItem> = listOf()

    override fun onCreate() {
        // Initialize your data here
        // This is called when the factory is first created, not for each widget update.
        val text = getItem(context, "todos", "group.com.example.widget") ?: ""
        items = parseJsonString(text)
    }

    override fun onDataSetChanged() {
        val text = getItem(context, "todos", "group.com.example.widget") ?: ""
        items = parseJsonString(text)
    }

    override fun onDestroy() {
        // Clean up any resources here if necessary.
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        // Creating the layout for each item based on your provided XML.
        val remoteView = RemoteViews(context.packageName, R.layout.widget_item)

        val item = items[position]
        remoteView.setTextViewText(R.id.text_view, item.content)

        // Assuming you have a method to convert color string to Int color
        val color = colorFromHexString(item.color)
        remoteView.setInt(R.id.image_view_container, "setBackgroundColor", color)
        remoteView.setInt(R.id.image_view, "setBackgroundColor", color)

        val fillInIntent = Intent()
        fillInIntent.putExtra("ITEM_ID", item.id)
        remoteView.setOnClickFillInIntent(R.id.widget_item, fillInIntent)

        return remoteView
    }

    override fun getLoadingView(): RemoteViews {
        // You can return a specific view while data is loading
        return return RemoteViews(context.packageName, R.layout.dev_loading_view);
    }

    override fun getViewTypeCount(): Int {
        // If you have more than one type of view, you'll need to return the number here.
        return 1
    }

    override fun getItemId(position: Int): Long {
        // Return the stable ID of the item at position
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        // Return true if the same id always refers to the same object.
        return true
    }
}

/**
 * Implementation of App Widget functionality.
 */
class widget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    // Construct the RemoteViews object
    val sampleWidget = RemoteViews(context.packageName, R.layout.sample_widget)
    val text = getItem(context, "todos", "group.com.example.widget") ?: ""
    val items = parseJsonString(text)
    val activityIntent = Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    sampleWidget.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

    if (items.size === 0) {
        val emptyString = getItem(context, "empty-string", "group.com.example.widget") ?: "All task completed!"
        sampleWidget.setTextViewText(R.id.empty_text, emptyString)
        sampleWidget.setViewVisibility(R.id.list_view, View.GONE)
        sampleWidget.setViewVisibility(R.id.empty_view, View.VISIBLE)
    } else {
        sampleWidget.setViewVisibility(R.id.list_view, View.VISIBLE)
        sampleWidget.setViewVisibility(R.id.empty_view, View.GONE)
    }

    // Set up the intent that starts the ListViewService, which will
    // provide the views for this collection
    val intent = Intent(context, ListViewService::class.java).apply {
        // Add the app widget ID to the intent extras.
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
    }

    sampleWidget.setRemoteAdapter(R.id.list_view, intent)

    val templateIntent = Intent(context, MainActivity::class.java)
    val templatePendingIntent = PendingIntent.getActivity(context, 0, templateIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    sampleWidget.setPendingIntentTemplate(R.id.list_view, templatePendingIntent)

    // Notify the list view that the data has changed
    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list_view)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, sampleWidget)
}

internal fun getItem(
    context: Context,
    key: String,
    preferenceName: String
): String? {
    val preferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    return preferences.getString(key, null)
}

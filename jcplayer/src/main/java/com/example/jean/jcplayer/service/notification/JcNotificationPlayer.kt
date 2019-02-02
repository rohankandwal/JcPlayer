package com.example.jean.jcplayer.service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import com.example.jean.jcplayer.JcPlayerManager
import com.example.jean.jcplayer.JcPlayerManagerListener
import com.example.jean.jcplayer.R
import com.example.jean.jcplayer.general.JcStatus
import com.example.jean.jcplayer.general.PlayerUtil
import java.lang.ref.WeakReference

/**
 * This class is a Android [Service] that handles notification changes on background.
 *
 * @author Jean Carlos (Github: @jeancsanchez)
 * @date 12/07/16.
 * Jesus loves you.
 */
class JcNotificationPlayer private constructor(
    private val context: Context) : JcPlayerManagerListener {

  private var title: String? = null
  private var time = "00:00"
  private var iconResource: Int = 0
  private val notificationManager: NotificationManager by lazy {
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  private var notification: Notification? = null

  companion object {
    const val NEXT = "jcplayer.NEXT"
    const val PREVIOUS = "jcplayer.PREVIOUS"
    const val PAUSE = "jcplayer.PAUSE"
    const val PLAY = "jcplayer.PLAY"
    const val ACTION = "jcplayer.ACTION"
    const val PLAYLIST = "jcplayer.PLAYLIST"
    const val CURRENT_AUDIO = "jcplayer.CURRENT_AUDIO"
    const val CATEGORY_MUSIC = "music"

    private const val NOTIFICATION_ID = 100
    private const val NOTIFICATION_CHANNEL = "jcplayer.NOTIFICATION_CHANNEL"
    private const val NEXT_ID = 0
    private const val PREVIOUS_ID = 1
    private const val PLAY_ID = 2
    private const val PAUSE_ID = 3


    @Volatile
    private var INSTANCE: WeakReference<JcNotificationPlayer>? = null

    @JvmStatic
    fun getInstance(context: Context): WeakReference<JcNotificationPlayer> = INSTANCE ?: let {
      INSTANCE = WeakReference(JcNotificationPlayer(context))
      INSTANCE!!
    }
  }

  fun createNotificationPlayer(title: String?, iconResourceResource: Int) {
    this.title = title
    this.iconResource = iconResourceResource
    val openUi = Intent(context, context.javaClass)
    openUi.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    //        JcPlayerManager.getInstance(context, null, null).registerNotificationListener(this);

    val priority = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationCompat.PRIORITY_HIGH
    } else {
      NotificationCompat.PRIORITY_DEFAULT
    }

    notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
        .setSmallIcon(iconResourceResource)
        .setCategory(CATEGORY_MUSIC)
        .setSound(null)
        .setLargeIcon(BitmapFactory.decodeResource(context.resources, iconResourceResource))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContent(createNotificationPlayerView())
        .setContentIntent(PendingIntent.getActivity(context, NOTIFICATION_ID, openUi,
            PendingIntent.FLAG_CANCEL_CURRENT))
        .setAutoCancel(false)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(Notification.DEFAULT_LIGHTS)
        .setVibrate( LongArray(1) { 0L })
        .setPriority(priority)
        .build()

    @RequiresApi(Build.VERSION_CODES.O)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(NOTIFICATION_CHANNEL, NOTIFICATION_CHANNEL,
          NotificationManager.IMPORTANCE_DEFAULT)
      channel.enableVibration(false)
      channel.description = "no sound"
      channel.lockscreenVisibility = VISIBILITY_PUBLIC
      channel.setSound(null, null)
      notificationManager.createNotificationChannel(channel)
    }

    try {
      notification?.let { notificationManager.notify(NOTIFICATION_ID, it) }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun updateNotification() {
    createNotificationPlayer(title, iconResource)
  }

  private fun createNotificationPlayerView(): RemoteViews {
    val remoteView: RemoteViews

    if (JcPlayerManager.getInstance(context, null, null).get()?.isPaused() == true) {
      remoteView = RemoteViews(context.packageName, R.layout.notification_play)
      remoteView.setOnClickPendingIntent(R.id.btn_play_notification,
          buildPendingIntent(PLAY, PLAY_ID))
    } else {
      remoteView = RemoteViews(context.packageName, R.layout.notification_pause)
      remoteView.setOnClickPendingIntent(R.id.btn_pause_notification,
          buildPendingIntent(PAUSE, PAUSE_ID))
    }

    remoteView.setTextViewText(R.id.txt_current_music_notification, title)
    remoteView.setTextViewText(R.id.txt_duration_notification, time)
    remoteView.setImageViewResource(R.id.icon_player, iconResource)
    remoteView.setOnClickPendingIntent(R.id.btn_next_notification,
        buildPendingIntent(NEXT, NEXT_ID))
    remoteView.setOnClickPendingIntent(R.id.btn_prev_notification,
        buildPendingIntent(PREVIOUS, PREVIOUS_ID))

    return remoteView
  }

  private fun buildPendingIntent(action: String, id: Int): PendingIntent {
    val playIntent = Intent(context.applicationContext, JcPlayerNotificationReceiver::class.java)
    playIntent.putExtra(ACTION, action)

    return PendingIntent.getBroadcast(context.applicationContext, id, playIntent,
        PendingIntent.FLAG_UPDATE_CURRENT)
  }

  override fun onPreparedAudio(status: JcStatus) {

  }

  override fun onCompletedAudio() {

  }

  override fun onPaused(status: JcStatus) {
    createNotificationPlayer(title, iconResource)
  }

  override fun onStopped(status: JcStatus) {
    destroyNotificationIfExists()
  }

  override fun onContinueAudio(status: JcStatus) {}

  override fun onPlaying(status: JcStatus) {
    createNotificationPlayer(title, iconResource)
  }

  override fun onTimeChanged(status: JcStatus) {
    this.time = PlayerUtil.toTimeSongString(status.currentPosition.toInt())
    this.title = status.jcAudio.title
    createNotificationPlayer(title, iconResource)
  }


  fun destroyNotificationIfExists() {
    try {
      notificationManager.cancel(NOTIFICATION_ID)
      notificationManager.cancelAll()
    } catch (e: NullPointerException) {
      e.printStackTrace()
    }
  }

  override fun onJcpError(throwable: Throwable) {

  }
}
package com.nutriai.data.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nutriai.MainActivity
import com.nutriai.R
import com.nutriai.data.settings.ReminderSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Muestra un recordatorio. Respeta el horario 08:00–22:00 para los de agua. */
class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.success()
        val text = inputData.getString(KEY_TEXT).orEmpty()
        if (inputData.getBoolean(KEY_DAYTIME_ONLY, false)) {
            val hour = LocalTime.now().hour
            if (hour < 8 || hour >= 22) return Result.success()
        }
        Notifications.show(applicationContext, inputData.getInt(KEY_ID, 1), title, text)
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_TEXT = "text"
        const val KEY_ID = "id"
        const val KEY_DAYTIME_ONLY = "daytime_only"
    }
}

object Notifications {
    const val CHANNEL_ID = "reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Comidas, agua, peso y ayuno"
                },
            )
        }
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, id: Int, title: String, text: String) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val intent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // El usuario retiró el permiso: no se muestra nada.
        }
    }
}

/** Programa (o cancela) los recordatorios con WorkManager según la configuración. */
@Singleton
class ReminderScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    private val work get() = WorkManager.getInstance(context)

    fun apply(r: ReminderSettings) {
        schedule("meal_breakfast", r.mealsEnabled, r.breakfastMinute, "Desayuno", "¿Registras lo que desayunaste?", 11)
        schedule("meal_lunch", r.mealsEnabled, r.lunchMinute, "Almuerzo", "Toma una foto de tu almuerzo o regístralo en segundos.", 12)
        schedule("meal_dinner", r.mealsEnabled, r.dinnerMinute, "Cena", "No olvides registrar tu cena.", 13)

        if (r.waterEnabled) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(r.waterIntervalHours.toLong().coerceAtLeast(1), TimeUnit.HOURS)
                .setInputData(workDataOf(ReminderWorker.KEY_TITLE to "Hora de beber agua", ReminderWorker.KEY_TEXT to "Un vaso de agua te acerca a tu meta del día.", ReminderWorker.KEY_ID to 21, ReminderWorker.KEY_DAYTIME_ONLY to true))
                .build()
            work.enqueueUniquePeriodicWork("water", ExistingPeriodicWorkPolicy.UPDATE, request)
        } else {
            work.cancelUniqueWork("water")
        }

        if (r.weightEnabled) {
            val now = LocalDateTime.now()
            var next = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(r.weightDayOfWeek)))
                .withHour(r.weightMinute / 60).withMinute(r.weightMinute % 60).withSecond(0)
            if (!next.isAfter(now)) next = next.plusWeeks(1)
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(ReminderWorker.KEY_TITLE to "Registro de peso", ReminderWorker.KEY_TEXT to "Pésate a la misma hora para ver tu tendencia.", ReminderWorker.KEY_ID to 31))
                .build()
            work.enqueueUniquePeriodicWork("weight", ExistingPeriodicWorkPolicy.UPDATE, request)
        } else {
            work.cancelUniqueWork("weight")
        }
    }

    /** Aviso al terminar el ayuno previsto. */
    fun scheduleFastingEnd(enabled: Boolean, endEpochMs: Long) {
        if (!enabled) { cancelFasting(); return }
        val delay = (endEpochMs - System.currentTimeMillis()).coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_TITLE to "¡Ayuno completado!", ReminderWorker.KEY_TEXT to "Llegaste a tu meta de ayuno. Ya puedes abrir tu ventana de comidas.", ReminderWorker.KEY_ID to 41))
            .build()
        work.enqueueUniqueWork("fasting_end", ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelFasting() = work.cancelUniqueWork("fasting_end")

    private fun schedule(name: String, enabled: Boolean, minuteOfDay: Int, title: String, text: String, id: Int) {
        if (!enabled) { work.cancelUniqueWork(name); return }
        val now = LocalDateTime.now()
        var next = now.withHour(minuteOfDay / 60).withMinute(minuteOfDay % 60).withSecond(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_TITLE to title, ReminderWorker.KEY_TEXT to text, ReminderWorker.KEY_ID to id))
            .build()
        work.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

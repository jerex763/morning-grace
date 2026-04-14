package com.morninggrace.alarm;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u0000 \u00132\u00020\u0001:\u0001\u0013B!\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u0010\t\u001a\u00020\nJ\u0018\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u000eH\u0002J\u000e\u0010\u0010\u001a\u00020\n2\u0006\u0010\u0011\u001a\u00020\u0012R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"Lcom/morninggrace/alarm/AlarmScheduler;", "", "context", "Landroid/content/Context;", "alarmManager", "Landroid/app/AlarmManager;", "permissionChecker", "Lcom/morninggrace/alarm/AlarmPermissionChecker;", "(Landroid/content/Context;Landroid/app/AlarmManager;Lcom/morninggrace/alarm/AlarmPermissionChecker;)V", "cancel", "", "nextAlarmMillis", "", "hour", "", "minute", "schedule", "config", "Lcom/morninggrace/core/model/AlarmConfig;", "Companion", "alarm_debug"})
public final class AlarmScheduler {
    @org.jetbrains.annotations.NotNull
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull
    private final android.app.AlarmManager alarmManager = null;
    @org.jetbrains.annotations.NotNull
    private final com.morninggrace.alarm.AlarmPermissionChecker permissionChecker = null;
    public static final int ALARM_REQUEST_CODE = 1001;
    @org.jetbrains.annotations.NotNull
    public static final com.morninggrace.alarm.AlarmScheduler.Companion Companion = null;
    
    @javax.inject.Inject
    public AlarmScheduler(@dagger.hilt.android.qualifiers.ApplicationContext
    @org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    android.app.AlarmManager alarmManager, @org.jetbrains.annotations.NotNull
    com.morninggrace.alarm.AlarmPermissionChecker permissionChecker) {
        super();
    }
    
    public final void schedule(@org.jetbrains.annotations.NotNull
    com.morninggrace.core.model.AlarmConfig config) {
    }
    
    public final void cancel() {
    }
    
    private final long nextAlarmMillis(int hour, int minute) {
        return 0L;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/morninggrace/alarm/AlarmScheduler$Companion;", "", "()V", "ALARM_REQUEST_CODE", "", "alarm_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}
package com.morninggrace.alarm;

@dagger.hilt.android.AndroidEntryPoint
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH\u0016R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\b\u00a8\u0006\u000f"}, d2 = {"Lcom/morninggrace/alarm/BootReceiver;", "Landroid/content/BroadcastReceiver;", "()V", "scheduler", "Lcom/morninggrace/alarm/AlarmScheduler;", "getScheduler", "()Lcom/morninggrace/alarm/AlarmScheduler;", "setScheduler", "(Lcom/morninggrace/alarm/AlarmScheduler;)V", "onReceive", "", "context", "Landroid/content/Context;", "intent", "Landroid/content/Intent;", "alarm_debug"})
public final class BootReceiver extends android.content.BroadcastReceiver {
    @javax.inject.Inject
    public com.morninggrace.alarm.AlarmScheduler scheduler;
    
    public BootReceiver() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.morninggrace.alarm.AlarmScheduler getScheduler() {
        return null;
    }
    
    public final void setScheduler(@org.jetbrains.annotations.NotNull
    com.morninggrace.alarm.AlarmScheduler p0) {
    }
    
    @java.lang.Override
    public void onReceive(@org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    android.content.Intent intent) {
    }
}
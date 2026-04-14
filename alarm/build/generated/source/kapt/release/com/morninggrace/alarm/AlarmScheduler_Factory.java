package com.morninggrace.alarm;

import android.app.AlarmManager;
import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class AlarmScheduler_Factory implements Factory<AlarmScheduler> {
  private final Provider<Context> contextProvider;

  private final Provider<AlarmManager> alarmManagerProvider;

  private final Provider<AlarmPermissionChecker> permissionCheckerProvider;

  public AlarmScheduler_Factory(Provider<Context> contextProvider,
      Provider<AlarmManager> alarmManagerProvider,
      Provider<AlarmPermissionChecker> permissionCheckerProvider) {
    this.contextProvider = contextProvider;
    this.alarmManagerProvider = alarmManagerProvider;
    this.permissionCheckerProvider = permissionCheckerProvider;
  }

  @Override
  public AlarmScheduler get() {
    return newInstance(contextProvider.get(), alarmManagerProvider.get(), permissionCheckerProvider.get());
  }

  public static AlarmScheduler_Factory create(Provider<Context> contextProvider,
      Provider<AlarmManager> alarmManagerProvider,
      Provider<AlarmPermissionChecker> permissionCheckerProvider) {
    return new AlarmScheduler_Factory(contextProvider, alarmManagerProvider, permissionCheckerProvider);
  }

  public static AlarmScheduler newInstance(Context context, AlarmManager alarmManager,
      AlarmPermissionChecker permissionChecker) {
    return new AlarmScheduler(context, alarmManager, permissionChecker);
  }
}

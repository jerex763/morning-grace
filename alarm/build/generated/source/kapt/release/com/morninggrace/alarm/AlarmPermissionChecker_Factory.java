package com.morninggrace.alarm;

import android.app.AlarmManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
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
public final class AlarmPermissionChecker_Factory implements Factory<AlarmPermissionChecker> {
  private final Provider<AlarmManager> alarmManagerProvider;

  public AlarmPermissionChecker_Factory(Provider<AlarmManager> alarmManagerProvider) {
    this.alarmManagerProvider = alarmManagerProvider;
  }

  @Override
  public AlarmPermissionChecker get() {
    return newInstance(alarmManagerProvider.get());
  }

  public static AlarmPermissionChecker_Factory create(Provider<AlarmManager> alarmManagerProvider) {
    return new AlarmPermissionChecker_Factory(alarmManagerProvider);
  }

  public static AlarmPermissionChecker newInstance(AlarmManager alarmManager) {
    return new AlarmPermissionChecker(alarmManager);
  }
}

package com.morninggrace.alarm;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class AlarmReceiver_MembersInjector implements MembersInjector<AlarmReceiver> {
  private final Provider<AlarmScheduler> schedulerProvider;

  public AlarmReceiver_MembersInjector(Provider<AlarmScheduler> schedulerProvider) {
    this.schedulerProvider = schedulerProvider;
  }

  public static MembersInjector<AlarmReceiver> create(Provider<AlarmScheduler> schedulerProvider) {
    return new AlarmReceiver_MembersInjector(schedulerProvider);
  }

  @Override
  public void injectMembers(AlarmReceiver instance) {
    injectScheduler(instance, schedulerProvider.get());
  }

  @InjectedFieldSignature("com.morninggrace.alarm.AlarmReceiver.scheduler")
  public static void injectScheduler(AlarmReceiver instance, AlarmScheduler scheduler) {
    instance.scheduler = scheduler;
  }
}

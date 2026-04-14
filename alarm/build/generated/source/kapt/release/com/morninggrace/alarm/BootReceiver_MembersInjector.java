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
public final class BootReceiver_MembersInjector implements MembersInjector<BootReceiver> {
  private final Provider<AlarmScheduler> schedulerProvider;

  public BootReceiver_MembersInjector(Provider<AlarmScheduler> schedulerProvider) {
    this.schedulerProvider = schedulerProvider;
  }

  public static MembersInjector<BootReceiver> create(Provider<AlarmScheduler> schedulerProvider) {
    return new BootReceiver_MembersInjector(schedulerProvider);
  }

  @Override
  public void injectMembers(BootReceiver instance) {
    injectScheduler(instance, schedulerProvider.get());
  }

  @InjectedFieldSignature("com.morninggrace.alarm.BootReceiver.scheduler")
  public static void injectScheduler(BootReceiver instance, AlarmScheduler scheduler) {
    instance.scheduler = scheduler;
  }
}

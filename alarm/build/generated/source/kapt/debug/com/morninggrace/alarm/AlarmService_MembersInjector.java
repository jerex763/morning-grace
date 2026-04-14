package com.morninggrace.alarm;

import com.morninggrace.orchestrator.MorningSession;
import com.morninggrace.tts.AndroidTtsEngine;
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
public final class AlarmService_MembersInjector implements MembersInjector<AlarmService> {
  private final Provider<MorningSession> morningSessionProvider;

  private final Provider<AndroidTtsEngine> ttsEngineProvider;

  public AlarmService_MembersInjector(Provider<MorningSession> morningSessionProvider,
      Provider<AndroidTtsEngine> ttsEngineProvider) {
    this.morningSessionProvider = morningSessionProvider;
    this.ttsEngineProvider = ttsEngineProvider;
  }

  public static MembersInjector<AlarmService> create(
      Provider<MorningSession> morningSessionProvider,
      Provider<AndroidTtsEngine> ttsEngineProvider) {
    return new AlarmService_MembersInjector(morningSessionProvider, ttsEngineProvider);
  }

  @Override
  public void injectMembers(AlarmService instance) {
    injectMorningSession(instance, morningSessionProvider.get());
    injectTtsEngine(instance, ttsEngineProvider.get());
  }

  @InjectedFieldSignature("com.morninggrace.alarm.AlarmService.morningSession")
  public static void injectMorningSession(AlarmService instance, MorningSession morningSession) {
    instance.morningSession = morningSession;
  }

  @InjectedFieldSignature("com.morninggrace.alarm.AlarmService.ttsEngine")
  public static void injectTtsEngine(AlarmService instance, AndroidTtsEngine ttsEngine) {
    instance.ttsEngine = ttsEngine;
  }
}

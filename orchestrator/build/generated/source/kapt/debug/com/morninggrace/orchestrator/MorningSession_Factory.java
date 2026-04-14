package com.morninggrace.orchestrator;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class MorningSession_Factory implements Factory<MorningSession> {
  @Override
  public MorningSession get() {
    return newInstance();
  }

  public static MorningSession_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static MorningSession newInstance() {
    return new MorningSession();
  }

  private static final class InstanceHolder {
    private static final MorningSession_Factory INSTANCE = new MorningSession_Factory();
  }
}

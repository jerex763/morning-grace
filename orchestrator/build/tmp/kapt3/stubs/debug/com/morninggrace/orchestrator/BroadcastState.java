package com.morninggrace.orchestrator;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0004\u0003\u0004\u0005\u0006B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0004\u0007\b\t\n\u00a8\u0006\u000b"}, d2 = {"Lcom/morninggrace/orchestrator/BroadcastState;", "", "()V", "Broadcasting", "Failed", "Idle", "Preparing", "Lcom/morninggrace/orchestrator/BroadcastState$Broadcasting;", "Lcom/morninggrace/orchestrator/BroadcastState$Failed;", "Lcom/morninggrace/orchestrator/BroadcastState$Idle;", "Lcom/morninggrace/orchestrator/BroadcastState$Preparing;", "orchestrator_debug"})
public abstract class BroadcastState {
    
    private BroadcastState() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/morninggrace/orchestrator/BroadcastState$Broadcasting;", "Lcom/morninggrace/orchestrator/BroadcastState;", "content", "Lcom/morninggrace/orchestrator/BroadcastContent;", "(Lcom/morninggrace/orchestrator/BroadcastContent;)V", "getContent", "()Lcom/morninggrace/orchestrator/BroadcastContent;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "orchestrator_debug"})
    public static final class Broadcasting extends com.morninggrace.orchestrator.BroadcastState {
        @org.jetbrains.annotations.NotNull
        private final com.morninggrace.orchestrator.BroadcastContent content = null;
        
        public Broadcasting(@org.jetbrains.annotations.NotNull
        com.morninggrace.orchestrator.BroadcastContent content) {
        }
        
        @org.jetbrains.annotations.NotNull
        public final com.morninggrace.orchestrator.BroadcastContent getContent() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        public final com.morninggrace.orchestrator.BroadcastContent component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        public final com.morninggrace.orchestrator.BroadcastState.Broadcasting copy(@org.jetbrains.annotations.NotNull
        com.morninggrace.orchestrator.BroadcastContent content) {
            return null;
        }
        
        @java.lang.Override
        public boolean equals(@org.jetbrains.annotations.Nullable
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public java.lang.String toString() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/morninggrace/orchestrator/BroadcastState$Failed;", "Lcom/morninggrace/orchestrator/BroadcastState;", "reason", "", "(Ljava/lang/String;)V", "getReason", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "orchestrator_debug"})
    public static final class Failed extends com.morninggrace.orchestrator.BroadcastState {
        @org.jetbrains.annotations.NotNull
        private final java.lang.String reason = null;
        
        public Failed(@org.jetbrains.annotations.NotNull
        java.lang.String reason) {
        }
        
        @org.jetbrains.annotations.NotNull
        public final java.lang.String getReason() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        public final com.morninggrace.orchestrator.BroadcastState.Failed copy(@org.jetbrains.annotations.NotNull
        java.lang.String reason) {
            return null;
        }
        
        @java.lang.Override
        public boolean equals(@org.jetbrains.annotations.Nullable
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public java.lang.String toString() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/morninggrace/orchestrator/BroadcastState$Idle;", "Lcom/morninggrace/orchestrator/BroadcastState;", "()V", "orchestrator_debug"})
    public static final class Idle extends com.morninggrace.orchestrator.BroadcastState {
        @org.jetbrains.annotations.NotNull
        public static final com.morninggrace.orchestrator.BroadcastState.Idle INSTANCE = null;
        
        private Idle() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/morninggrace/orchestrator/BroadcastState$Preparing;", "Lcom/morninggrace/orchestrator/BroadcastState;", "()V", "orchestrator_debug"})
    public static final class Preparing extends com.morninggrace.orchestrator.BroadcastState {
        @org.jetbrains.annotations.NotNull
        public static final com.morninggrace.orchestrator.BroadcastState.Preparing INSTANCE = null;
        
        private Preparing() {
        }
    }
}
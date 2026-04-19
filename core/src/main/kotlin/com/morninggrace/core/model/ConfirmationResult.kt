package com.morninggrace.core.model

sealed class ConfirmationResult {
    object Confirmed : ConfirmationResult()
    object Skipped   : ConfirmationResult()
    object Timeout   : ConfirmationResult()
}

package com.example.feature_authentication.presentation.sign_in

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.settings.UserSessionStorage
import com.example.common.ui.UiText
import com.example.common.utils.Constants
import com.example.common.utils.Resource
import com.example.common.utils.StateStringPropertyDelegate
import com.example.feature_authentication.R
import com.example.feature_authentication.domain.use_case.SignInUseCase
import com.example.feature_authentication.domain.use_case.ValidateEmailUseCase
import com.example.feature_authentication.domain.use_case.ValidatePasswordUseCase
import com.example.feature_note.domain.use_case.SynchronizeNotesUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


class SignInViewModel(
    private val signInUseCase: SignInUseCase,
    private val validateEmail: ValidateEmailUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    private val synchronizeNotesUseCase: SynchronizeNotesUseCase,
    userSessionStorage: UserSessionStorage,
    state: SavedStateHandle
) : ViewModel() {

    private var email by StateStringPropertyDelegate(
        state = state,
        key = "email",
        initialValue = Constants.EMPTY_VALUE
    )
    private var password by StateStringPropertyDelegate(
        state = state,
        key = "password",
        initialValue = Constants.EMPTY_VALUE
    )

    private val _signInChannel = Channel<UiSignInEvent>()
    val singInEvent = _signInChannel.receiveAsFlow()

    init {
        if (userSessionStorage.getUserSessionId().isNotBlank()) {
            navigateToNoteListScreen()
        }
    }

    fun onEvent(event: SignInEvent) {
        when (event) {
            is SignInEvent.EmailChanged -> email = event.email
            is SignInEvent.PasswordChanged -> password = event.password
            is SignInEvent.RegisterNewAccountButtonClicked -> navigateToRegisterScreen()
            is SignInEvent.LoginButtonClicked -> signInUser()
        }
    }

    private fun signInUser() {
        if (!isValidationSuccessful()) return

        viewModelScope.launch {
            signInUseCase(email, password).onEach { event ->
                when (event) {
                    is Resource.Loading -> showProgressBar(true)
                    is Resource.Success -> {
                        synchronizeNotesUseCase()
                        showProgressBar(false)
                        navigateToNoteListScreen()
                    }
                    is Resource.Error -> {
                        event.error?.let { errorMessage ->
                            showSnackbar(UiText.DynamicString(errorMessage))
                        } ?: showSnackbar(UiText.StringResource(R.string.unexpected_error))
                        showProgressBar(false)
                    }
                }
            }.launchIn(this)
        }
    }

    private fun isValidationSuccessful(): Boolean {
        val emailResult = validateEmail(email)
        if (!emailResult.successful) {
            showSnackbar(emailResult.errorMessage!!)
            return false
        }

        val passwordResult = validatePassword(password)
        if (!passwordResult.successful) {
            showSnackbar(passwordResult.errorMessage!!)
            return false
        }

        return true
    }

    private fun navigateToNoteListScreen() = viewModelScope.launch {
        _signInChannel.send(UiSignInEvent.NavigateToNoteListScreen)
    }


    private fun navigateToRegisterScreen() = viewModelScope.launch {
        _signInChannel.send(UiSignInEvent.NavigateToRegisterScreen)
    }


    private fun showSnackbar(message: UiText) = viewModelScope.launch {
        _signInChannel.send(UiSignInEvent.ShowSnackbar(message = message))
    }


    private fun showProgressBar(isLoading: Boolean) = viewModelScope.launch {
        _signInChannel.send(UiSignInEvent.ShowProgressBar(isLoading))
    }


    sealed class UiSignInEvent {
        data class ShowSnackbar(val message: UiText) : UiSignInEvent()
        data class ShowProgressBar(val isLoading: Boolean) : UiSignInEvent()
        object NavigateToNoteListScreen : UiSignInEvent()
        object NavigateToRegisterScreen : UiSignInEvent()
    }
}
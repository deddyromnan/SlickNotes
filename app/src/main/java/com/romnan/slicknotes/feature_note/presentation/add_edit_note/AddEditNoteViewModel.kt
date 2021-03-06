package com.romnan.slicknotes.feature_note.presentation.add_edit_note

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romnan.slicknotes.feature_note.domain.model.InvalidNoteException
import com.romnan.slicknotes.feature_note.domain.model.Note
import com.romnan.slicknotes.feature_note.domain.use_case.NoteUseCases
import com.romnan.slicknotes.feature_note.presentation.util.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditNoteViewModel @Inject constructor(
    private val noteUseCases: NoteUseCases,
    savedStateHandle: SavedStateHandle
) :
    ViewModel() {
    private var currentNoteId: Int? = null

    private val _timestampState = mutableStateOf(NoteTimestampState(System.currentTimeMillis()))
    val timestampState: State<NoteTimestampState> = _timestampState

    private val _reminderState = mutableStateOf(NoteReminderState())
    val reminderState: State<NoteReminderState> = _reminderState

    private val _titleState = mutableStateOf(NoteTextFieldState(hint = "Enter title"))
    val titleState: State<NoteTextFieldState> = _titleState

    private val _contentState = mutableStateOf(NoteTextFieldState(hint = "Enter some content"))
    val contentState: State<NoteTextFieldState> = _contentState

    private val _colorState = mutableStateOf(Note.noteColors.random().toArgb())
    val colorState: State<Int> = _colorState

    private val _eventFlow = MutableSharedFlow<UIEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        savedStateHandle.get<Int>(Screen.AddEditNote.ARG_NOTE_ID)?.let { noteId ->
            if (noteId != -1) {
                viewModelScope.launch {
                    noteUseCases.find(noteId)?.also { note ->
                        currentNoteId = note.id
                        _titleState.value = titleState.value.copy(
                            text = note.title,
                            isHintVisible = false
                        )
                        _contentState.value = contentState.value.copy(
                            text = note.content,
                            isHintVisible = false
                        )
                        _reminderState.value = _reminderState.value.copy(
                            timeInMillis = note.reminderTimeStamp
                        )

                        _timestampState.value = _timestampState.value.copy(
                            timestamp = note.timeStamp
                        )

                        _colorState.value = note.color
                    }
                }
            }
        }
    }

    fun onEvent(event: AddEditNoteEvent) {
        when (event) {
            is AddEditNoteEvent.ChangeReminder -> {
                _reminderState.value = reminderState.value.copy(
                    timeInMillis = event.reminderDateTime,
                    hasChanged = true
                )
            }

            is AddEditNoteEvent.EnterTitle -> {
                _titleState.value = titleState.value.copy(
                    text = event.title
                )
            }

            is AddEditNoteEvent.ChangeTitleFocus -> {
                _titleState.value = titleState.value.copy(
                    isHintVisible = !event.focusState.isFocused && titleState.value.text.isBlank()
                )
            }

            is AddEditNoteEvent.EnterContent -> {
                _contentState.value = contentState.value.copy(
                    text = event.content
                )
            }

            is AddEditNoteEvent.ChangeContentFocus -> {
                _contentState.value = contentState.value.copy(
                    isHintVisible = !event.focusState.isFocused && contentState.value.text.isBlank()
                )
            }

            is AddEditNoteEvent.ChangeColor -> {
                _colorState.value = event.color
            }

            is AddEditNoteEvent.SaveNote -> {
                viewModelScope.launch {
                    try {
                        noteUseCases.save(
                            Note(
                                id = currentNoteId,
                                title = titleState.value.text,
                                content = contentState.value.text,
                                timeStamp = System.currentTimeMillis(),
                                reminderTimeStamp = reminderState.value.timeInMillis,
                                color = colorState.value
                            )
                        )
                        _eventFlow.emit(UIEvent.SaveNote)
                    } catch (e: InvalidNoteException) {
                        _eventFlow.emit(
                            UIEvent.ShowSnackbar(message = e.message ?: "Couldn't save note")
                        )
                    }
                }
            }
        }
    }

    sealed class UIEvent {
        data class ShowSnackbar(val message: String) : UIEvent()
        object SaveNote : UIEvent()
    }
}
package com.fsacchi.schoolmate.presentation.states

import androidx.compose.runtime.Stable
import com.fsacchi.schoolmate.data.local.entity.UserEntity
import com.fsacchi.schoolmate.data.model.discipline.DisciplineModel
import com.fsacchi.schoolmate.data.model.home.JobCalendarModel
import com.fsacchi.schoolmate.data.model.home.JobHomeModel
import com.fsacchi.schoolmate.data.model.job.JobModel

@Stable
data class JobCalendarUiState(
    val screenType: ScreenType = ScreenType.Await
) {
    sealed interface ScreenType {
        data object Await: ScreenType
        data class Loaded(
            val jobCalendarModel: List<JobCalendarModel>
        ): ScreenType

        data class Error(
            val errorTitle: String?,
            val errorMessage: String?
        ): ScreenType
    }
}

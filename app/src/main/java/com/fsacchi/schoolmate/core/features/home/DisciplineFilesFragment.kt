package com.fsacchi.schoolmate.core.features.home

import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.fsacchi.schoolmate.R
import com.fsacchi.schoolmate.core.extensions.capitalizeFirstLetter
import com.fsacchi.schoolmate.core.extensions.clickListener
import com.fsacchi.schoolmate.core.extensions.createProgressDialog
import com.fsacchi.schoolmate.core.extensions.showMessage
import com.fsacchi.schoolmate.core.extensions.startActionView
import com.fsacchi.schoolmate.core.features.home.adapter.FileListAdapter
import com.fsacchi.schoolmate.core.features.home.adapter.JobListAdapter
import com.fsacchi.schoolmate.core.features.home.sheets.FileBottomSheet
import com.fsacchi.schoolmate.core.features.home.sheets.JobBottomSheet
import com.fsacchi.schoolmate.core.features.home.sheets.OptionsBottomSheet
import com.fsacchi.schoolmate.core.features.home.sheets.UploadFileBottomSheet
import com.fsacchi.schoolmate.core.platform.BaseFragment
import com.fsacchi.schoolmate.core.platform.PagerAdapter
import com.fsacchi.schoolmate.data.model.discipline.DisciplineModel
import com.fsacchi.schoolmate.data.model.file.FileModel
import com.fsacchi.schoolmate.data.model.file.FileUserModel
import com.fsacchi.schoolmate.data.model.job.JobModel
import com.fsacchi.schoolmate.databinding.FragmentDisciplineDetailBinding
import com.fsacchi.schoolmate.databinding.FragmentDisciplineFilesBinding
import com.fsacchi.schoolmate.presentation.features.DisciplineViewModel
import com.fsacchi.schoolmate.presentation.features.FileViewModel
import com.fsacchi.schoolmate.presentation.features.JobViewModel
import com.fsacchi.schoolmate.presentation.states.DefaultUiState
import com.fsacchi.schoolmate.presentation.states.FilesUiState
import com.fsacchi.schoolmate.presentation.states.JobUiState
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class DisciplineFilesFragment : BaseFragment<FragmentDisciplineFilesBinding>() {

    private lateinit var disciplineSelected: DisciplineModel
    private lateinit var homeActivity: HomeActivity
    private lateinit var fileSelected: FileUserModel

    private val fileViewModel: FileViewModel by inject()
    private val dialog by lazy { createProgressDialog() }
    private val adapter by lazy { FileListAdapter() }

    override val layoutRes: Int
        get() = R.layout.fragment_discipline_files

    override fun start() {
        homeActivity = (activity as HomeActivity)
        binding.showEmptyState = true
        insertListeners()
        observe()
    }

    private fun observe() {
        lifecycleScope.launch {
            fileViewModel.uiState.files.collect { fileUiState ->
                fileUiState?.let {
                    when(it.screenType) {
                        FilesUiState.ScreenType.Await -> {
                            dialog.show()
                        }
                        is FilesUiState.ScreenType.Error -> {
                            dialog.dismiss()
                            homeActivity.showAlertMessage(
                                isError = true,
                                title = it.screenType.errorTitle ?: "Erro ao carregar atividades",
                                message = it.screenType.errorMessage.orEmpty()
                            )
                        }
                        is FilesUiState.ScreenType.Loaded -> {
                            dialog.dismiss()
                            bindAdapter(it.screenType.listFiles)
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            fileViewModel.uiState.deleteFile.collect { deleteUiState ->
                deleteUiState?.let {
                    when(it.screenType) {
                        is DefaultUiState.ScreenType.Error -> {
                            dialog.dismiss()
                            homeActivity.showAlertMessage(
                                isError = true,
                                title = it.screenType.errorTitle ?: "Erro ao deletar arquivo",
                                message = it.screenType.errorMessage.orEmpty()
                            )
                        }
                        DefaultUiState.ScreenType.Loading -> {
                            dialog.show()
                        }
                        DefaultUiState.ScreenType.Success -> {
                            fileViewModel.getFiles(homeActivity.user.uid, disciplineSelected)
                        }
                    }
                }
            }
        }
    }

    private fun bindAdapter(listFiles: List<FileUserModel>) {
        binding.showEmptyState = listFiles.isEmpty()

        binding.rvFile.adapter = adapter.apply {
            submitList(listFiles)
            rootListener = {
                fileSelected = it
                activity?.startActionView(it.urlFirebase, it.extension)
            }
            listenerOptions = {
                fileSelected = it
                showOptionsMenu()
            }
        }
    }

    private fun showOptionsMenu() {
        OptionsBottomSheet
            .newInstance(jobMenuItems)
            .setListener(::handleOptionsMenu)
            .show(childFragmentManager)
    }

    private fun handleOptionsMenu(optionItem: OptionItem) {
        when(optionItem.desc) {
            R.string.edit -> {
                val fileModel = FileModel(
                    extension = fileSelected.extension,
                    nameFile = fileSelected.titleFile,
                    urlFirebase = fileSelected.urlFirebase
                )
                showFileBottomSheet(fileSelected, fileModel)

            }
            R.string.delete -> {
                showMessage {
                    title(R.string.warning)
                    message(getString(R.string.confirm_delete_file, fileSelected.titleFile))
                    positiveListener {
                        fileViewModel.deleteFileModel(fileSelected, homeActivity.user.uid)
                    }
                    negativeListener {}
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fileViewModel.getFiles(homeActivity.user.uid, disciplineSelected)
    }

    private fun insertListeners() {
        binding.btnCreateFile.clickListener{
            UploadFileBottomSheet.newInstance().setListener {
                showFileBottomSheet(FileUserModel(), it)
            }.show(childFragmentManager)
        }
    }

    fun setDisciplineSelected(disciplineModel: DisciplineModel) {
        disciplineSelected = disciplineModel
    }

    private fun showFileBottomSheet(fileUserModel: FileUserModel, fileModel: FileModel) {
        FileBottomSheet.newInstance(
            disciplineSelected,
            fileUserModel = fileUserModel,
            fileModel = fileModel,
            homeActivity.user.uid,
            successListener = {
                fileViewModel.getFiles(homeActivity.user.uid, disciplineSelected)
            },
            errorListener = { errorMessage ->
                homeActivity.showAlertMessage(
                    isError = true,
                    title = "Erro ao salvar arquivo",
                    message = errorMessage

                )
            },
            newDisciplineListener = {}
        ).show(childFragmentManager)
    }
}

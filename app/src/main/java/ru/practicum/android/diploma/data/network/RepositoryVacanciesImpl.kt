package ru.practicum.android.diploma.data.network

import android.content.SharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import ru.practicum.android.diploma.R
import ru.practicum.android.diploma.data.NetworkClient
import ru.practicum.android.diploma.data.dto.SearchRequest
import ru.practicum.android.diploma.data.dto.responses.vacancy.list.mapToVacancies
import ru.practicum.android.diploma.data.models.EMPTY_PARAM_SRT
import ru.practicum.android.diploma.data.models.SearchSettings
import ru.practicum.android.diploma.data.models.ValuesSearchId
import ru.practicum.android.diploma.domain.api.RepositoryVacancies
import ru.practicum.android.diploma.domain.models.SearchResultData
import ru.practicum.android.diploma.domain.models.vacancy.Vacancies
import java.net.ConnectException

const val SEARCHING_OPTIONS = "searching_options" // заглушка до реализации фичи фильтрации

class RepositoryVacanciesImpl(
    private val client: NetworkClient,
    private val settingsPref: SharedPreferences,
    private val json: Gson
) : RepositoryVacancies {
    override suspend fun getVacancies(query: String, pageNum: Int): Flow<SearchResultData<Vacancies>> = flow {
        val searchResult = client.getVacancies(SearchRequest.setSearchOptions(query, pageNum, updateSearchSettings()))
        val data = searchResult.getOrNull()
        val error = searchResult.exceptionOrNull()

        when {
            data != null -> {
                if (data.found == 0) {
                    emit(SearchResultData.Empty(R.string.empty))
                } else {
                    emit(SearchResultData.Data(data.mapToVacancies()))
                }
            }

            error is ConnectException -> {
                emit(SearchResultData.NoInternet(R.string.no_internet))
            }

            error is HttpException -> {
                emit(SearchResultData.ErrorServer(R.string.server_error))
            }
        }
    }

    private fun updateSearchSettings(): SearchSettings {
        val settingsStr = settingsPref.getString(SEARCHING_OPTIONS, EMPTY_PARAM_SRT)
        if (settingsStr != EMPTY_PARAM_SRT) {
            return json.fromJson(settingsStr, SearchSettings::class.java)
        } else {
            return SearchSettings(settingsId = ValuesSearchId.BASE)
        }

    }
}

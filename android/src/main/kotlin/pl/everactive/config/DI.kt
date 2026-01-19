package pl.everactive.config

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import pl.everactive.BuildConfig
import pl.everactive.clients.EveractiveApi
import pl.everactive.clients.EveractiveApiClient
import pl.everactive.clients.EveractiveApiToken
import pl.everactive.services.DataStoreService
import pl.everactive.services.ServiceController

val mainModule = module {
    singleOf(::DataStoreService)
    singleOf(::ServiceController)

    singleOf(::EveractiveApiToken)
    single {
        val client = EveractiveApi.createKtorClient(BuildConfig.API_BASE_URL, get())

        EveractiveApi(client)
    }
    singleOf(::EveractiveApiClient)
}

package pl.everactive.config

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import pl.everactive.clients.EveractiveApi
import pl.everactive.clients.EveractiveApiClient
import pl.everactive.clients.EveractiveApiToken
import pl.everactive.services.DataStoreService

val mainModule = module {
    singleOf(::DataStoreService)

    singleOf(::EveractiveApiToken)
    single {
        val client = EveractiveApi.createKtorClient("", get())

        EveractiveApi(client)
    }
    singleOf(::EveractiveApiClient)
}

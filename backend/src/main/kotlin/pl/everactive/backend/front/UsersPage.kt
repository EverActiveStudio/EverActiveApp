package pl.everactive.backend.front

import kotlinx.html.BODY
import kotlinx.html.*

import pl.everactive.backend.entities.UserEntity

class UsersPage(
    private val users: List<UserEntity>
) : BasePage("Users management") {

    override fun BODY.body() {
        // Nagłówek i przycisk powrotu
        div {
            style = "display: flex; justify-content: space-between; align-items: center; width: 100%; border-bottom: 1px solid #2c5364; padding-bottom: 1rem;"

            h2 {
                style = "margin: 0;"
                +"Zarządzanie Użytkownikami"
            }

            div {
                style = "display: flex; gap: 1rem;"

                a(href = "/manager/users/create") {
                    role = "button"
                    style = "background-color: #4ca1af; border: none;"
                    +"Dodaj"
                }

                form(action = "/logout", method = FormMethod.post) {
                    style = "margin: 0;"
                    button(type = ButtonType.submit) {
                        style = "background-color: transparent; border: 1px solid #e74c3c; color: #e74c3c; margin: 0;"
                        +"Wyloguj"
                    }
                }
            }
        }

        // Tabela użytkowników
        table {
            thead {
                tr {
                    th { +"ID" }
                    th { +"Imię i Nazwisko" }
                    th { +"Email" }
                    th { +"Rola" }
                    th { +"Akcje" }
                }
            }
            tbody {
                users.forEach { user ->
                    tr {
                        td { +"${user.id}" }
                        td { +user.name }
                        td { +user.email }
                        td {
                            span {
                                style = "padding: 4px 8px; border-radius: 4px; background-color: ${if (user.role.name == "Manager") "#e3f2fd" else "#f5f5f5"}; color: ${if (user.role.name == "Manager") "#1565c0" else "#333"};"
                                +user.role.name
                            }
                        }
                        td {
                            // Ustawiamy komórkę tabeli jako Flexbox, żeby elementy były w jednej linii
                            // vertical-align: middle jest dla pewności, gdyby flex nie zadziałał w specyficznym kontekście tabeli
                            style = "display: flex; align-items: center; gap: 8px; height: 100%; vertical-align: middle;"

                            // --- 1. PRZYCISK EDYCJI ---
                            a(href = "/manager/users/${user.id}/edit") {
                                role = "button"
                                style = """
                                    display: flex;                /* Ważne: Flex wewnątrz przycisku centruje tekst */
                                    align-items: center;
                                    justify-content: center;
                                    height: 36px;                 /* Sztywna wysokość */
                                    padding: 0 12px;              /* Padding tylko boczny, wysokość ustala height */
                                    background-color: #2980b9;    /* Niebieski */
                                    color: white;
                                    border: 1px solid #2980b9;    /* Ramka */
                                    border-radius: 4px;
                                    text-decoration: none;
                                    font-size: 0.85rem;
                                    font-weight: 500;
                                    box-sizing: border-box;       /* Padding nie powiększa elementu */
                                    line-height: 1;               /* Reset wysokości linii */
                                """.trimIndent()
                                +"Edytuj"
                            }

                            // --- 2. FORMULARZ Z PRZYCISKIEM USUWANIA ---
                            form(action = "/manager/users/${user.id}/delete", method = FormMethod.post) {
                                // KLUCZOWE: display: flex na formularzu sprawia, że zachowuje się on jak zwykły kontener
                                style = "margin: 0; padding: 0; display: flex;"

                                button(type = ButtonType.submit) {
                                    style = """
                                        display: flex;            /* Tak samo jak w linku wyżej */
                                        align-items: center;
                                        justify-content: center;
                                        height: 36px;             /* Identyczna wysokość co Edytuj */
                                        padding: 0 12px;
                                        background-color: #e74c3c; /* Czerwony */
                                        color: white;
                                        border: 1px solid #e74c3c; /* Ramka */
                                        border-radius: 4px;
                                        cursor: pointer;
                                        font-size: 0.85rem;
                                        font-weight: 500;
                                        box-sizing: border-box;
                                        line-height: 1;
                                        font-family: inherit;      /* Ważne: buttony czasem mają inną czcionkę */
                                    """.trimIndent()

                                    onClick = "return confirm('Czy na pewno chcesz usunąć użytkownika ${user.name}?')"

                                    +"Usuń"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

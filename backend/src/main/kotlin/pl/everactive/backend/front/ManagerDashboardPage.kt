package pl.everactive.backend.front

import kotlinx.html.BODY
import kotlinx.html.h1
import kotlinx.html.p

class ManagerDashboardPage(
    private val managerName: String,
) : BasePage("Manager Dashboard") {
    override fun BODY.body() {
        h1 {
            +"Manager Dashboard"
        }
        p {
            +"Welcome ${managerName}!"
        }
    }
}

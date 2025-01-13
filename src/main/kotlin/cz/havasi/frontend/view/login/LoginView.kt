package cz.havasi.frontend.view.login

import com.vaadin.flow.component.login.LoginI18n
import com.vaadin.flow.component.login.LoginOverlay

@com.vaadin.flow.server.auth.AnonymousAllowed
@com.vaadin.flow.router.PageTitle("Login")
@com.vaadin.flow.router.Route(value = "login")
public class LoginView public constructor(authenticatedUser: AuthenticatedUser) : LoginOverlay(),
    com.vaadin.flow.router.BeforeEnterObserver {

    init {
        this.authenticatedUser = authenticatedUser
        setAction(
            com.vaadin.flow.router.internal.RouteUtil.getRoutePath(
                com.vaadin.flow.server.VaadinService.getCurrent().getContext(), javaClass
            )
        )

        val i18n: LoginI18n = LoginI18n.createDefault()
        i18n.setHeader(LoginI18n.Header())
        i18n.getHeader().setTitle("Havasi Reality")
        i18n.getHeader().setDescription("Login using user/user or admin/admin")
        i18n.setAdditionalInformation(null)
        setI18n(i18n)

        setForgotPasswordButtonVisible(false)
        setOpened(true)
    }

    override fun beforeEnter(event: com.vaadin.flow.router.BeforeEnterEvent) {
        if (authenticatedUser.get().isPresent()) {
            // Already logged in
            setOpened(false)
            event.forwardTo("")
        }

        setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"))
    }
}

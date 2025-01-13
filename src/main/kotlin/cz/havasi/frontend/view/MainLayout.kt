package cz.havasi.frontend.view

import com.example.application.data.User
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.ComponentEventListener
import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.applayout.DrawerToggle
import com.vaadin.flow.component.avatar.Avatar
import com.vaadin.flow.component.contextmenu.MenuItem
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Footer
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Header
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.SvgIcon
import com.vaadin.flow.component.menubar.MenuBar
import com.vaadin.flow.component.orderedlayout.Scroller
import com.vaadin.flow.component.sidenav.SideNav
import com.vaadin.flow.component.sidenav.SideNavItem
import com.vaadin.flow.router.Layout
import com.vaadin.flow.server.InputStreamFactory
import com.vaadin.flow.server.StreamResource
import com.vaadin.flow.server.auth.AccessAnnotationChecker
import com.vaadin.flow.server.auth.AnonymousAllowed
import com.vaadin.flow.server.menu.MenuConfiguration
import com.vaadin.flow.server.menu.MenuEntry
import com.vaadin.flow.theme.lumo.LumoUtility
import org.jcp.xml.dsig.internal.dom.DOMKeyInfo.getContent
import java.io.ByteArrayInputStream
import java.util.Optional
import java.util.function.Consumer

/**
 * The main view is a top-level placeholder for other views.
 */
@Layout
@AnonymousAllowed
public class MainLayout public constructor(
    authenticatedUser: AuthenticatedUser,
    accessChecker: AccessAnnotationChecker?
) : AppLayout() {
    private var viewTitle: H1? = null

    private val authenticatedUser: AuthenticatedUser
    private val accessChecker: AccessAnnotationChecker?

    init {
        this.authenticatedUser = authenticatedUser
        this.accessChecker = accessChecker

        setPrimarySection(AppLayout.Section.DRAWER)
        addDrawerContent()
        addHeaderContent()
    }

    private fun addHeaderContent() {
        val toggle: DrawerToggle = DrawerToggle()
        toggle.setAriaLabel("Menu toggle")

        viewTitle = H1()
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE)

        addToNavbar(true, toggle, viewTitle)
    }

    private fun addDrawerContent() {
        val appName = Span("Havasi Reality")
        appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE)
        val header = Header(appName)

        val scroller: Scroller = Scroller(createNavigation())

        addToDrawer(header, scroller, createFooter())
    }

    private fun createNavigation(): SideNav {
        val nav: SideNav = SideNav()

        val menuEntries = MenuConfiguration.getMenuEntries()
        menuEntries.forEach(Consumer { entry: MenuEntry? ->
            if (entry!!.icon() != null) {
                nav.addItem(SideNavItem(entry.title(), entry.path(), SvgIcon(entry.icon())))
            } else {
                nav.addItem(SideNavItem(entry.title(), entry.path()))
            }
        })

        return nav
    }

    private fun createFooter(): Footer {
        val layout: Footer = Footer()

        val maybeUser: Optional<User> = authenticatedUser.get()
        if (maybeUser.isPresent()) {
            val user: User = maybeUser.get()

            val avatar: Avatar = Avatar(user.getName())
            val resource = StreamResource(
                "profile-pic",
                InputStreamFactory { ByteArrayInputStream(user.getProfilePicture()) })
            avatar.setImageResource(resource)
            avatar.setThemeName("xsmall")
            avatar.getElement().setAttribute("tabindex", "-1")

            val userMenu = MenuBar()
            userMenu.setThemeName("tertiary-inline contrast")

            val userName = userMenu.addItem("")
            val div = Div()
            div.add(avatar)
            div.add(user.getName())
            div.add(Icon("lumo", "dropdown"))
            div.getElement().getStyle().set("display", "flex")
            div.getElement().getStyle().set("align-items", "center")
            div.getElement().getStyle().set("gap", "var(--lumo-space-s)")
            userName.add(div)
            userName.getSubMenu().addItem("Sign out", ComponentEventListener { e: ClickEvent<MenuItem?>? ->
                authenticatedUser.logout()
            })

            layout.add(userMenu)
        } else {
            val loginLink: Anchor = Anchor("login", "Sign in")
            layout.add(loginLink)
        }

        return layout
    }

    override fun afterNavigation() {
        super.afterNavigation()
        viewTitle.setText(this.currentPageTitle)
    }

    private val currentPageTitle: String
        get() = MenuConfiguration.getPageHeader(getContent()).orElse("")
}

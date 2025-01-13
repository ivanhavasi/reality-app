package cz.havasi.frontend.view.dashboard


import com.example.application.views.dashboard.ServiceHealth.Status

@com.vaadin.flow.router.PageTitle("Dashboard")
@com.vaadin.flow.router.Route("")
@com.vaadin.flow.router.Menu(order = 0, icon = LineAwesomeIconUrl.CHART_AREA_SOLID)
@jakarta.annotation.security.PermitAll
public class DashboardView public constructor() : com.vaadin.flow.component.html.Main() {
    init {
        addClassName("dashboard-view")

        val board: Board = Board()
        board.addRow(
            createHighlight("Current users", "745", 33.7), createHighlight("View events", "54.6k", -112.45),
            createHighlight("Conversion rate", "18%", 3.9), createHighlight("Custom metric", "-123.45", 0.0)
        )
        board.addRow(createViewEvents())
        board.addRow(createServiceHealth(), createResponseTimes())
        add(board)
    }

    private fun createHighlight(
        title: kotlin.String?,
        value: kotlin.String?,
        percentage: kotlin.Double
    ): com.vaadin.flow.component.Component {
        var icon: VaadinIcon = VaadinIcon.ARROW_UP
        var prefix = ""
        var theme = "badge"

        if (percentage == 0.0) {
            prefix = "±"
        } else if (percentage > 0) {
            prefix = "+"
            theme += " success"
        } else if (percentage < 0) {
            icon = VaadinIcon.ARROW_DOWN
            theme += " error"
        }

        val h2: H2 = H2(title)
        h2.addClassNames(LumoUtility.FontWeight.NORMAL, Margin.NONE, TextColor.SECONDARY, LumoUtility.FontSize.XSMALL)

        val span = com.vaadin.flow.component.html.Span(value)
        span.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.XXXLARGE)

        val i: com.vaadin.flow.component.icon.Icon = icon.create()
        i.addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Padding.XSMALL)

        val badge =
            com.vaadin.flow.component.html.Span(i, com.vaadin.flow.component.html.Span(prefix + percentage.toString()))
        badge.getElement().getThemeList().add(theme)

        val layout: VerticalLayout = VerticalLayout(h2, span, badge)
        layout.addClassName(LumoUtility.Padding.LARGE)
        layout.setPadding(false)
        layout.setSpacing(false)
        return layout
    }

    private fun createViewEvents(): com.vaadin.flow.component.Component {
        // Header
        val year: com.vaadin.flow.component.select.Select<*> = com.vaadin.flow.component.select.Select<kotlin.Any?>()
        year.setItems("2011", "2012", "2013", "2014", "2015", "2016", "2017", "2018", "2019", "2020", "2021")
        year.setValue("2021")
        year.setWidth("100px")

        val header: HorizontalLayout = createHeader("View events", "City/month")
        header.add(year)

        // Chart
        val chart: Chart = Chart(ChartType.AREASPLINE)
        val conf: Configuration = chart.getConfiguration()
        conf.getChart().setStyledMode(true)

        val xAxis: XAxis = XAxis()
        xAxis.setCategories("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        conf.addxAxis(xAxis)

        conf.getyAxis().setTitle("Values")

        val plotOptions: PlotOptionsAreaspline = PlotOptionsAreaspline()
        plotOptions.setPointPlacement(PointPlacement.ON)
        plotOptions.setMarker(Marker(false))
        conf.addPlotOptions(plotOptions)

        conf.addSeries(ListSeries("Berlin", 189, 191, 291, 396, 501, 403, 609, 712, 729, 942, 1044, 1247))
        conf.addSeries(ListSeries("London", 138, 246, 248, 348, 352, 353, 463, 573, 778, 779, 885, 887))
        conf.addSeries(ListSeries("New York", 65, 65, 166, 171, 293, 302, 308, 317, 427, 429, 535, 636))
        conf.addSeries(ListSeries("Tokyo", 0, 11, 17, 123, 130, 142, 248, 349, 452, 454, 458, 462))

        // Add it all together
        val viewEvents: VerticalLayout = VerticalLayout(header, chart)
        viewEvents.addClassName(LumoUtility.Padding.LARGE)
        viewEvents.setPadding(false)
        viewEvents.setSpacing(false)
        viewEvents.getElement().getThemeList().add("spacing-l")
        return viewEvents
    }

    private fun createServiceHealth(): com.vaadin.flow.component.Component {
        // Header
        val header: HorizontalLayout = createHeader("Service health", "Input / output")

        // Grid
        val grid: com.vaadin.flow.component.grid.Grid<ServiceHealth?> =
            com.vaadin.flow.component.grid.Grid<kotlin.Any?>()
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER)
        grid.setAllRowsVisible(true)

        grid.addColumn(
            com.vaadin.flow.data.renderer.ComponentRenderer<com.vaadin.flow.component.html.Span?, ServiceHealth?>(
                com.vaadin.flow.function.SerializableFunction { serviceHealth: ServiceHealth? ->
                    val status = com.vaadin.flow.component.html.Span()
                    val statusText = getStatusDisplayName(serviceHealth!!)
                    status.getElement().setAttribute("aria-label", "Status: " + statusText)
                    status.getElement().setAttribute("title", "Status: " + statusText)
                    status.getElement().getThemeList().add(getStatusTheme(serviceHealth))
                    status
                })
        ).setHeader("").setFlexGrow(0).setAutoWidth(true)
        grid.addColumn(com.vaadin.flow.function.ValueProvider { obj: ServiceHealth? -> obj!!.city })
            .setHeader("City").setFlexGrow(1)
        grid.addColumn(com.vaadin.flow.function.ValueProvider { obj: ServiceHealth? -> obj!!.input })
            .setHeader("Input").setAutoWidth(true).setTextAlign(ColumnTextAlign.END)
        grid.addColumn(com.vaadin.flow.function.ValueProvider { obj: ServiceHealth? -> obj!!.output })
            .setHeader("Output").setAutoWidth(true)
            .setTextAlign(ColumnTextAlign.END)

        grid.setItems(
            ServiceHealth(Status.EXCELLENT, "Münster", 324, 1540),
            ServiceHealth(Status.OK, "Cluj-Napoca", 311, 1320),
            ServiceHealth(Status.FAILING, "Ciudad Victoria", 300, 1219)
        )

        // Add it all together
        val serviceHealth: VerticalLayout = VerticalLayout(header, grid)
        serviceHealth.addClassName(LumoUtility.Padding.LARGE)
        serviceHealth.setPadding(false)
        serviceHealth.setSpacing(false)
        serviceHealth.getElement().getThemeList().add("spacing-l")
        return serviceHealth
    }

    private fun createResponseTimes(): com.vaadin.flow.component.Component {
        val header: HorizontalLayout = createHeader("Response times", "Average across all systems")

        // Chart
        val chart: Chart = Chart(ChartType.PIE)
        val conf: Configuration = chart.getConfiguration()
        conf.getChart().setStyledMode(true)
        chart.setThemeName("gradient")

        val series: DataSeries = DataSeries()
        series.add(DataSeriesItem("System 1", 12.5))
        series.add(DataSeriesItem("System 2", 12.5))
        series.add(DataSeriesItem("System 3", 12.5))
        series.add(DataSeriesItem("System 4", 12.5))
        series.add(DataSeriesItem("System 5", 12.5))
        series.add(DataSeriesItem("System 6", 12.5))
        conf.addSeries(series)

        // Add it all together
        val serviceHealth: VerticalLayout = VerticalLayout(header, chart)
        serviceHealth.addClassName(LumoUtility.Padding.LARGE)
        serviceHealth.setPadding(false)
        serviceHealth.setSpacing(false)
        serviceHealth.getElement().getThemeList().add("spacing-l")
        return serviceHealth
    }

    private fun createHeader(title: kotlin.String?, subtitle: kotlin.String?): HorizontalLayout {
        val h2: H2 = H2(title)
        h2.addClassNames(LumoUtility.FontSize.XLARGE, Margin.NONE)

        val span = com.vaadin.flow.component.html.Span(subtitle)
        span.addClassNames(TextColor.SECONDARY, LumoUtility.FontSize.XSMALL)

        val column: VerticalLayout = VerticalLayout(h2, span)
        column.setPadding(false)
        column.setSpacing(false)

        val header: HorizontalLayout = HorizontalLayout(column)
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN)
        header.setSpacing(false)
        header.setWidthFull()
        return header
    }

    private fun getStatusDisplayName(serviceHealth: ServiceHealth): kotlin.String? {
        val status: Status = serviceHealth.getStatus()
        if (status === Status.OK) {
            return "Ok"
        } else if (status === Status.FAILING) {
            return "Failing"
        } else if (status === Status.EXCELLENT) {
            return "Excellent"
        } else {
            return status.toString()
        }
    }

    private fun getStatusTheme(serviceHealth: ServiceHealth): kotlin.String {
        val status: Status = serviceHealth.getStatus()
        var theme = "badge primary small"
        if (status === Status.EXCELLENT) {
            theme += " success"
        } else if (status === Status.FAILING) {
            theme += " error"
        }
        return theme
    }
}

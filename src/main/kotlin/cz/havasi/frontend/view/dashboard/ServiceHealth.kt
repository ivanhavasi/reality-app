package cz.havasi.frontend.view.dashboard

/**
 * Simple DTO class for the inbox list to demonstrate complex object data
 */
public class ServiceHealth {
    private var status: Status? = null
        get() {
            return field
        }
        set(status) {
            field = status
        }

    private var city: String? = null

    private var input = 0

    private var output = 0

    private val theme: String? = null

    internal enum class Status {
        EXCELLENT, OK, FAILING
    }

    public constructor()

    public constructor(status: Status?, city: String?, input: Int, output: Int) {
        this.status = status
        this.city = city
        this.input = input
        this.output = output
    }

    public fun getStatus(): Status? {
        return field
    }

    public fun setStatus(status: Status?) {
        field = status
    }

    public fun getCity(): String? {
        return city
    }

    public fun setCity(city: String?) {
        this.city = city
    }

    public fun getInput(): Int {
        return input
    }

    public fun setInput(input: Int) {
        this.input = input
    }

    public fun getOutput(): Int {
        return output
    }

    public fun setOutput(output: Int) {
        this.output = output
    }
}

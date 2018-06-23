package example

import com.typesafe.config.ConfigFactory
import org.celtric.kotlin.html.*
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.server.Netty
import org.http4k.server.asServer

fun main(args: Array<String>) {
    val resource = Class::class.java.getResource("/application.conf")
    val config = ConfigFactory.parseURL(resource)

    val skipLog = args.contains("--skip-logs")

    val app: HttpHandler = routes(
        "/static" bind static(Classpath("/static")),
        "/" bind Method.GET to {
            _: Request -> Response(OK).body("Hello world!")
        },

        "/greet" bind routes(
            "/" bind Method.GET to {
                _: Request -> Response(OK).body(htmlTemplate("anon!").render())
            },
            "/{name}" bind Method.GET to {
                req: Request -> Response(OK).body(htmlTemplate(req.path("name") ?: "").render())
            }
        )
    )

    val simpleResponse = app(Request(Method.GET, "/"))
    val greetResponse = app(Request(Method.GET, "/greet/"))

    log(simpleResponse.status.description, skipLog)
    log(greetResponse.status.description, skipLog)

    val portPath = "deployment.port"
    val port = if(config.hasPath(portPath)) config.getInt(portPath) else 9000

    log("Listening on http://127.0.0.1:$port", skipLog)
    val server = app.asServer(Netty(port)).start()
    server.stop()
}

fun log(string: String, skip: Boolean) {
    if(!skip) println(string)
}

fun htmlTemplate(name: String) =
        doctype("html") +
        html {
            head {
                title("My website title") +
                script(type = "text/javascript", src = "/static/hello.js") {}
            } +
            body {
                div {
                    "Hello $name!"
                }
            }
        }
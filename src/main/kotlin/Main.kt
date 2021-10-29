import com.seansoper.batil.brokers.etrade.auth.Authorization
import com.seansoper.batil.brokers.etrade.auth.Session
import com.seansoper.batil.brokers.etrade.services.Market
import com.seansoper.batil.config.ClientConfig
import com.seansoper.batil.config.GlobalConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

fun main(args: Array<String>) {
    val session = createSession()
    val watchlist = Watchlist(tickers = listOf(
        "PLTR",
        "TSLA",
        "CLOV",
        "SOFI",
        "AMC"
    ))

    runBlocking {
        scan(watchlist, session)
    }
}

suspend fun scan(watchlist: Watchlist, session: Session, delay: Long = 2000L) {
    val service = Market(session, production = true, verbose = false)
    println()

    while (true) {
        service.tickers(watchlist.tickers)?.let {
            it.forEach { security ->
                security.symbol?.let { ticker ->
                    security.tickerData.lastTrade?.let { price ->
                        watchlist.setPrice(ticker, price)
                    }
                }
            }
        }

        val results = watchlist.prices.map {
           "${it.value.third}${it.key} at ${it.value.first} ${it.value.second}"
        }

        print("\r${results.joinToString("  ")}")

        delay(delay)
    }
}

class Watchlist(val tickers: List<String>) {

    private val reset = "\u001B[0m"
    private val red = "\u001B[31m"
    private val green = "\u001B[32m"
    private val default = " "

    val prices: MutableMap<String, Triple<Float, String, String>> = tickers.associate {
        it to Triple(0f, default, reset)
    }.toMutableMap()

    fun setPrice(ticker: String, price: Float) {
        val (trend, color) = getTrendColor(ticker, price)
        prices[ticker] = Triple(price, trend, color)
    }

    private fun getTrendColor(ticker: String, price: Float): Pair<String, String> {
        return prices[ticker]?.first?.let { currentPrice ->
            if (currentPrice > 0f) {
                if (currentPrice > price) {
                    "↑" to green
                } else if (currentPrice < price) {
                    "↓" to red
                } else {
                    default to reset
                }
            } else {
                default to reset
            }
        } ?: (default to reset)
    }
}

fun createSession(): Session {
    val clientConfig = ClientConfig(Paths.get("/Users/ssoper/workspace/Batil/batil.yaml"), verbose = true, production = true)
    val globalConfig = GlobalConfig.parse(clientConfig)
    val client = Authorization(globalConfig, production = true, verbose = true)

    return client.renewSession() ?: client.createSession()
}

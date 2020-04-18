import discord4j.core.DiscordClientBuilder
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono
import java.io.IOException
import java.io.InputStream
import java.util.Random

fun main(args: Array<String>) {
    DiscordClientBuilder.create(args.first()).build().withGateway { gateway ->
        Mono.`when`(
                gateway.eventDispatcher.on(MessageCreateEvent::class.java)
                        .map { it.message }
                        .filter { it.content == "test-message" }
                        .flatMap { message ->
                            message.channel.flatMap { channel ->
                                channel.createMessage("reply")
                            }

                        }.then(),
                gateway.eventDispatcher.on(MessageCreateEvent::class.java)
                        .map { it.message }
                        .filter { it.content == "test-file" }
                        .flatMap { message ->
                            message.channel.flatMap { channel ->
                                channel.createMessage { spec ->
                                    spec.addFile("data", RandomInputStream(5000000))
                                    spec.setContent("here is your file")
                                }
                            }

                        }.then(),
                gateway.onDisconnect()
        )
    }.block()
}

// This class generates a specific number of random bytes
class RandomInputStream(private val size: Int) : InputStream() {
    private val generator = Random()
    private var closed = false
    private var read = 0

    override fun read(): Int {
        checkOpen()
        if (read >= size) return -1
        read += 1
        var result = generator.nextInt() % 256
        if (result < 0) {
            result = -result
        }
        return result
    }

    override fun read(data: ByteArray, offset: Int, length: Int): Int {
        checkOpen()
        if (read >= size) return -1
        val temp = ByteArray(length)
        generator.nextBytes(temp)
        System.arraycopy(temp, 0, data, offset, length)
        read += length
        return length
    }

    override fun read(data: ByteArray): Int {
        checkOpen()
        if (read >= size) return -1
        generator.nextBytes(data)
        read += data.size
        return data.size
    }

    override fun skip(bytesToSkip: Long): Long {
        checkOpen()
        return bytesToSkip
    }

    override fun close() {
        closed = true
    }

    private fun checkOpen() {
        if (closed) {
            throw IOException("Input stream closed")
        }
    }

    override fun available() = size - read
}
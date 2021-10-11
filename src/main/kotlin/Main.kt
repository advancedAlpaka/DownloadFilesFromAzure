import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.common.StorageSharedKeyCredential
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.file.Files
import kotlin.io.path.Path

class Query(accountName: String, accountKey: String, containerName: String, filesRegex: String) {
    val containerClient: BlobContainerClient
    val fileRegex: Regex

    init {
        val credential = StorageSharedKeyCredential(accountName, accountKey)
        val endPoint = String.format("https://%s.blob.core.windows.net", accountName)
        containerClient = BlobServiceClientBuilder().endpoint(endPoint).credential(credential).buildClient()
            .getBlobContainerClient(containerName)
        fileRegex = Regex(filesRegex)
    }
}

fun printAndRead(msg: String): String {
    print(msg)
    return readLine() ?: ""
}

fun interactiveInput(): Query {
    print(
        """
            Choose mode of work : 
            1 - config from file
            2 - config from console
            Enter : """.trimIndent()
    )
    while (true) {
        when (printAndRead("")) {
            "1" -> {
                return fromFile(printAndRead("Enter path to config : "))
            }
            "2" -> {
                val accountName = printAndRead("Enter Account Name : ")
                val accountKey = printAndRead("Enter Account Key : ")
                val containerName = printAndRead("Enter Container Name : ")
                val filesRegex =
                    printAndRead("Enter regex, determining paths to files, which you want download ([^/]*/ads.txt - for Ads) : ")
                print("Do you want save these data on local disk? (Y/N) : ")
                while (printAndRead("").uppercase() == "Y") {
                    val pathToSave = Path(printAndRead("Enter path : "))
                    try {
                        Files.writeString(
                            pathToSave,
                            Json.encodeToString(QueryJson(accountName, accountKey, containerName, filesRegex))
                        )
                    } catch (e: IOException) {
                        print(
                            """
                                You have problems with saving to file. Original exception :
                                $e
                                Do you want repeat? (Y/N)
                            """.trimIndent()
                        )
                    }
                }
                return Query(accountName, accountKey, containerName, filesRegex)
            }
            else -> {
                print("You choose not right variant, repeat : ")
            }
        }
    }
}

@Serializable
data class QueryJson(val accountName: String, val accountKey: String, val containerName: String, val filesRegex: String)

fun fromFile(path: String): Query {
    val config = Json.decodeFromString<QueryJson>(Files.readString(Path(path)))
    return Query(config.accountName, config.accountKey, config.containerName, config.filesRegex)
}

fun fromArgs(args: Array<String>): Query {
    if (args.size != 4) {
        System.console().printf("Incorrect count of args, must be 4, try interactive input :")
        return interactiveInput()
    }
    return Query(args[0], args[1], args[2], args[3])
}

fun doWork(query: Query) {
    val result = Path("qu")
    Files.createDirectories(result)
    query.containerClient.listBlobs().filter {
        //println(it.name)
        it.name.matches(query.fileRegex)
    }.map { it.name }.forEach {
        println(it)
        val blobClient = query.containerClient.getBlobClient(it).blockBlobClient
        Files.createDirectories(result.resolve(Path(it)).parent)
        blobClient.downloadToFile(result.resolve(Path(it)).toString())
    }
}

fun main(args: Array<String>) {
    val query = when (args.size) {
        0 -> interactiveInput()
        1 -> fromFile(args[0])
        else -> fromArgs(args)
    }
    doWork(query)
}
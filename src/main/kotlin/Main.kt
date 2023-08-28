import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.CSVRecord
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readLines


@Composable
@Preview
fun App() {
    var fileDirStr by remember { mutableStateOf("") }
    var errMsgList by remember { mutableStateOf(listOf<String>()) }
    var status by remember { mutableStateOf("") }

    MaterialTheme {

        Column (Modifier.padding(20.dp)){
            TextField(fileDirStr, onValueChange = {
                fileDirStr = it
                status = ""
            })
            Button(onClick = {
                val outputDir = makeOutputDir(fileDirStr)
                if (outputDir != null) {
                    val chunks = Chunk.makeChunks(fileDirStr)
                    if (chunks != null) {
                        try {
                            writeChunks(outputDir, chunks,
                                onWrite = {
                                    status = "đang chạy"
                                },
                                onDone = {
                                    status = "Xong"
                                }

                            )
                        } catch (e: Exception) {
                            status = "Lỗi"
                            errMsgList = errMsgList + "Lỗi lúc ghi file"
                        }

                    } else {
                        status = "Lỗi"
                        errMsgList = errMsgList + "ko đọc dc file"
                    }
                } else {
                    status = "Lỗi"
                    errMsgList = errMsgList + "Folder ko ổn"
                }
            }) {
                Text("Chạy")
            }

            Text(status)

        }

        if (errMsgList.isNotEmpty()) {
            errMsgList.forEach { err ->
                ErrDialog(err = err) {
                    errMsgList = errMsgList.filter { it != err }
                    if(errMsgList.isEmpty()){
                        status = ""
                    }
                }
            }

        }
    }
}

@Composable
fun ErrDialog(err: String, onDismiss: () -> Unit) {

    Dialog(
        onCloseRequest = onDismiss,
        state = rememberDialogState(
            size = DpSize(width = 300.dp, height = 150.dp)
        ),
        title="Lỗi"
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(err)
            Spacer(modifier = Modifier.height(30.dp))
            Button(onDismiss) {
                Text("OK")
            }
        }
    }
}

fun writeChunks(
    fileDir: String, chunks: List<Chunk>,
    onWrite: () -> Unit,
    onDone: () -> Unit,
) {
    try {
        onWrite()
        FileWriter(fileDir).use { fw ->
            val format = CSVFormat.EXCEL
                .builder()
                .setSkipHeaderRecord(false)
                .build()
            CSVPrinter(fw, format).use { csvPrinter ->
                csvPrinter.printRecord("tên", "địa chỉ", "sđt")
                chunks.forEach { c ->
                    csvPrinter.printRecord(c.name, c.addr, c.phone)
                }
            }
        }

    } catch (e: IOException) {
        throw e
    }
    onDone()
}

fun makeOutputDir(inputDir: String): String? {
    val inputPath = Path.of(inputDir)
    if (inputPath.isRegularFile()) {
        val inputFileName = inputPath.fileName.toString()
        val dotIndex: Int = inputFileName.lastIndexOf('.')
        val inputFileWithoutExtension = if (dotIndex > 0) {
            inputFileName.substring(0, dotIndex)
        } else {
            inputFileName
        }

        val outputFile = inputFileWithoutExtension + "_output.csv"
        return inputPath.parent.resolve(outputFile).toAbsolutePath().toString()
    } else {
        return null
    }
}

fun readFile(fileDir: String): List<String>? {
    val path = Path.of(fileDir)
    try {
        return path.readLines()
    } catch (e: Exception) {
        return null
    }
}


fun cleanText(input: List<String>): List<String> {
    return input.map {
        it.trim()
    }.filter {
        it.isNotBlank()
    }
}

class Chunk(
    val name: String?,
    val addr: String?,
    val phone: String?
) {
    companion object {
        fun makeChunks(input: List<String>): List<Chunk> {
            val chunks = input.chunked(3)
            return chunks.map {
                Chunk(
                    name = it.firstOrNull(),
                    phone = it.getOrNull(1),
                    addr = it.getOrNull(2),
                )
            }
        }

        fun makeChunks(fileDir: String): List<Chunk>? {
            val file = readFile(fileDir)
            if (file != null) {
                return makeChunks(cleanText(file))
            } else {
                return null
            }
        }
    }
}

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(width = 450.dp, height = 230.dp)
    )
    Window(onCloseRequest = ::exitApplication, resizable = false, state = windowState) {
        App()
    }
}

package foatto.server.util

import foatto.core.util.getRandomInt
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.ByteOrder
import java.nio.channels.SocketChannel
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

@Volatile
var inLogging: Boolean = true

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//--- список неинтересной телеметрии
private val arrUnusedPrefix = arrayOf("awt.toolkit", "file.", "java.awt.", "java.compiler", "java.home", "java.io.tmpdir", "java.vendor.url.bug", "java.vm.info", "sun.", "user.")
private val arrUnusedPostfix = arrayOf(".dirs", ".path", ".runtime.name", ".separator", ".specification.name", ".url", ".vendor")

fun getSystemProperties(): Map<String, String> {
    val hmSP = mutableMapOf<String, String>()
    try {
        val prop = System.getProperties()
        OUT@
        for (spName in prop.stringPropertyNames()) {
            for (unusedPrefix in arrUnusedPrefix) {
                if (spName.startsWith(unusedPrefix)) continue@OUT
            }
            for (unusedPostfix in arrUnusedPostfix) {
                if (spName.endsWith(unusedPostfix)) continue@OUT
            }

            hmSP[spName] = prop.getProperty(spName)
        }
    } catch (t: Throwable) {
        hmSP.put("System.getProperties error", t.stackTraceToString())
    }
    return hmSP
}

//--- чтение в буфер -----------------------------------------------------------------------------------------------------------------------------------------------------

fun readChannelToBuffer(socketChannel: SocketChannel, aBbIn: AdvancedByteBuffer? = null, byteOrder: ByteOrder, byteCount: Int, exceptionText: CharSequence): AdvancedByteBuffer {
    var bbIn = aBbIn
    if (bbIn == null) {
        bbIn = AdvancedByteBuffer(byteCount, byteOrder)
        bbIn.flip()
    } else {
        //--- перед чтением проверяем/расширяем буфер, т.к. socketChannel.read этого делать не будет
        //--- (делаем пару compact/flip, т.к. bbIn придёт к нам в режиме чтения)
        bbIn.compact()
        bbIn.checkSize(byteCount)
        bbIn.flip()
    }

    while (bbIn.remaining() < byteCount) {
        bbIn.compact()
        val num = socketChannel.read(bbIn.buffer)
        if (num == -1) throw IOException(exceptionText.toString())
        bbIn.flip()
    }
    return bbIn
}

fun readFileToBuffer(file: File, bbIn: AdvancedByteBuffer, isWriteSize: Boolean) {
    val fileSize = file.length().toInt()

    if (isWriteSize) {
        bbIn.putInt(fileSize)
    }

    //--- перед чтением проверяем/расширяем буфер, т.к. fileChannel.read этого делать не будет
    bbIn.checkSize(fileSize)
    val fileChannel = FileInputStream(file).channel
    fileChannel.read(bbIn.buffer)
    fileChannel.close()
}

//--- поиск свободного имени файла/директории ----------------------------------------------------------------------------------------------------------------------------

fun getFreeFile(rootDirName: String, arrFileExt: Array<String>): String {
    val dir = File(rootDirName)
    NEXT_FILE@
    while (true) {
        val newFileName = getRandomInt().toString()
        for (ext in arrFileExt) {
            if (File(dir, "$newFileName.$ext").exists()) {
                continue@NEXT_FILE
            }
        }
        return newFileName
    }
}

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

fun replaceFileRoot(sourPath: String, newRoot: File): Pair<File, File> {
    var file = File(sourPath)
    val alPath = ArrayList<String>()
    val oldRoot: File
    while (true) {
        val parent = file.parentFile
        if (parent == null) {
            oldRoot = file
            break
        }
        alPath.add(0, file.name)
        file = parent
    }
    var rootNew = newRoot
    for (s in alPath) {
        rootNew = File(rootNew, s)
    }
    return Pair(rootNew, oldRoot)
}

//--- очистка папок от старых файлов --------------------------------------------------------------------------------------------------------------------------------------------------

fun clearOldFiles(path: File, expireTime: Int) {
    val arrFile = path.listFiles()
    if (arrFile != null) {
        for (file in arrFile) {
            if (file.isDirectory) {
                clearOldFiles(file, expireTime)
            } else if (file.lastModified() < expireTime) {
                file.delete()
            }
        }
    }
}

fun clearStorage(storageRoot: File, hsStorageExt: Set<String>, needFreeSpaceByte: Long, needFreeSpacePercent: Int, isDeleteEmptyDir: Boolean) {
    val tsStorageRoot = TreeSet<File>()
    tsStorageRoot.add(storageRoot)
    clearStorage(tsStorageRoot, hsStorageExt, needFreeSpaceByte, needFreeSpacePercent, isDeleteEmptyDir)
}

fun clearStorage(tsStorageRoot: TreeSet<File>, hsStorageExt: Set<String>, needFreeSpaceByte: Long, needFreeSpacePercent: Int, isDeleteEmptyDir: Boolean) {
    val pqFile = PriorityQueue(1024, AscendingFileTimeComparator)

    collectFileQueue(tsStorageRoot, hsStorageExt, isDeleteEmptyDir, pqFile)

    //--- чистим папки, пока не освободится необходимое свободное место
    //--- поскольку все папки с одного диска, то достаточно место смотреть по первой папке)
    while (tsStorageRoot.first().freeSpace < needFreeSpaceByte || tsStorageRoot.first().freeSpace < tsStorageRoot.first().totalSpace * needFreeSpacePercent / 100) {
        val deleteFile = pqFile.poll() ?: break
        //--- кончились удалимые файлы
        deleteFile.delete()
    }
}

fun collectFileQueue(hsStorageRoot: TreeSet<File>, hsStorageExt: Set<String>?, isDeleteEmptyDir: Boolean, pqFile: PriorityQueue<File>) {
    val alDir = mutableListOf<File>()
    alDir.addAll(hsStorageRoot)

    //--- проходим по всем папкам и набираем видео-файлы и логи ffmpeg'a
    //--- (именно через использование dirIndex, т.к. внутри цикла происходит добавление в цикл)
    for (dir in alDir) {
        val arrDir = dir.listFiles()
        //--- всякие там спец/системные/служебные папки
        if (arrDir == null) {
            continue
        } else if (arrDir.isEmpty()) {
            //--- если можно удалять пустые папки и они не являются корневыми/заданными папками
            if (isDeleteEmptyDir && !hsStorageRoot.contains(dir)) {
                dir.delete()
            }
        } else {
            for (file in arrDir) {
                if (file.isDirectory) {
                    alDir.add(file)
                } else {
                    val fileName = file.name
                    //--- если список расширений не задан, то по нему не фильтруем
                    if (hsStorageExt == null || hsStorageExt.isEmpty()) {
                        pqFile.offer(file)
                    } else {
                        val dotPos = fileName.indexOf('.')
                        if (dotPos != -1) {
                            val ext = fileName.substring(dotPos + 1).lowercase(Locale.getDefault())
                            if (hsStorageExt.contains(ext)) {
                                pqFile.offer(file)
                            }
                        }
                    }
                }
            }
        }
    }
}

//--- запуск внешних команд ----------------------------------------------------------------------------------------------------------------------------------------------------------

private fun runCommandAndWait(cmdLine: String, workDir: File? = null): Triple<Int, String, String> {
    val processBuilder = ProcessBuilder(cmdLine.split(" ").filter { it.isNotEmpty() })
        .directory(workDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)

    val process = processBuilder.start()

    val returnCode = process.waitFor()
    val stdOutput = process.inputStream.bufferedReader().readText()
    val errOutput = process.errorStream.bufferedReader().readText()

    return Triple(returnCode, stdOutput, errOutput)
}

private fun runCommandInBackground(
    cmdLine: String,
    workDir: File? = null,
    logSubDir: String,
    successString: String?,
    processList: MutableList<Process>? = null
) {
    val processBuilder = ProcessBuilder(cmdLine.split(" ").filter { it.isNotEmpty() })
        .directory(workDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)

    //--- стартуем процесс
    val process = processBuilder.start()
    //--- некоторые процессы останавливаются путём запуска внешних скриптов, нет необходимости отслеживать и потом прибивать эти процессы
    processList?.let {
        processList += process
    }
    val inputReader = process.inputStream.bufferedReader()
    val errorReader = process.errorStream.bufferedReader()

    val latch = if (!successString.isNullOrEmpty()) {
        CountDownLatch(1)
    } else {
        null
    }

    //--- с корутинами здесь грустно - тредпул почти сразу забивается блокирующими операциями.
    //GlobalScope.launch {

    //--- inputStream и errorStream разделены на два отдельных потока - т.к. чтение/ожидание одного потока блокирует чтение из другого
    thread {
        try {
            errorReader.useLines { lineSequence ->
                lineSequence.forEach {
                    if (inLogging) {
                        AdvancedLogger.error(it, logSubDir)
                    } else {
                        return@forEach
                    }
                }
            }
        } catch (t: Throwable) {
            AdvancedLogger.error(t)
        }
    }
    thread {
        try {
            inputReader.useLines { lineSequence ->
                lineSequence.forEach { line ->
                    latch?.let {
                        if (line.contains(successString!!)) {
                            latch.countDown()
                        }
                    }
                    if (inLogging) {
                        AdvancedLogger.debug(line, logSubDir)
                    } else {
                        return@forEach
                    }
                }
            }
        } catch (t: Throwable) {
            AdvancedLogger.error(t)
        }
    }

    //--- подождём полной загрузки, если задана ключевая строка, но не более 1 минуты (на случай ошибочно заданных ключевых выражений)
    latch?.await(1L, TimeUnit.MINUTES)
}

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//--- Примечание к своим будущим попыткам "улучшить":
//--- комбинация BufferedWriter - OutputStreamWriter - FileOutputStream
//--- многократно проверена через док-ции и статьи,
//--- и нечего здесь пытаться улучшать (ну разве что через NIO-классы)
//--- (просто PrintWriter применить нельзя, т.к. он не обеспечивает ДОБАВЛЕНИЯ (append) в файл)
fun getFileWriter(fileName: String, append: Boolean, charset: Charset = Charsets.UTF_8): BufferedWriter = BufferedWriter(OutputStreamWriter(FileOutputStream(fileName, append), charset))
fun getFileWriter(file: File, append: Boolean, charset: Charset = Charsets.UTF_8): BufferedWriter = BufferedWriter(OutputStreamWriter(FileOutputStream(file, append), charset))

//--- работа с текстовыми файлами -----------------------------------------------------------------------------------------------------------------------------------------------------

fun loadTextFile(fileName: String, charset: Charset = Charsets.UTF_8, commentString: String? = null, skipEmptyLines: Boolean = true): List<String> =
    loadTextFile(Path.of(fileName), charset, commentString, skipEmptyLines)

fun loadTextFile(file: File, charset: Charset = Charsets.UTF_8, commentString: String? = null, skipEmptyLines: Boolean = true): List<String> =
    loadTextFile(file.toPath(), charset, commentString, skipEmptyLines)

fun loadTextFile(path: Path, charset: Charset = Charsets.UTF_8, commentString: String? = null, skipEmptyLines: Boolean = true): List<String> =
    Files.readAllLines(path, charset).map { line ->
        line.trim()
    }.filterNot { line ->
        skipEmptyLines && line.isEmpty() ||
            !commentString.isNullOrEmpty() && line.startsWith(commentString)
    }

//--- работа с конфиг-файлами ---------------------------------------------------------------------------------------------------------------------------------------------------------

//--- загрузка конфигов из файла
fun loadConfig(
    fileName: String,
    charset: Charset = Charsets.UTF_8,
    commentMark: String = "#",
    includeMark: String? = "@",
    paramMark: String? = "$",
    hmConfig: MutableMap<String, String> = mutableMapOf()
): MutableMap<String, String> {

    val fileConfig = File(fileName)
    if (!fileConfig.exists()) {
        return hmConfig
    }

    val alIni = loadTextFile(fileName, charset, commentMark, true)
    for (line in alIni) {
        val key = line.substringBefore('=', "").trim()
        var value = line.substringAfter('=', "").trim()

        if (key.isEmpty() || value.isEmpty()) {
            continue
        }

        //--- включение другого ini-файла
        if (!includeMark.isNullOrEmpty() && key.startsWith(includeMark)) {
            loadConfig(File(fileConfig.parentFile, value).canonicalPath, charset, commentMark, includeMark, paramMark, hmConfig)
        } else {
            //--- применение/вставка/замена ранее загруженных параметров в текущее значение
            //--- (например, $root_dir$ )
            if (!paramMark.isNullOrEmpty()) {
                while (true) {
                    val pos1 = value.indexOf(paramMark)
                    if (pos1 == -1) {
                        break
                    }
                    val pos2 = value.indexOf(paramMark, pos1 + paramMark.length)
                    if (pos2 == -1) {
                        break
                    }

                    val paramName = value.substring(pos1 + paramMark.length, pos2)
                    val paramValue = hmConfig[paramName]

                    value = value.substring(0, pos1) + (paramValue ?: "") + value.substring(pos2 + paramMark.length)
                }
            }
            hmConfig[key] = value
        }
    }
    return hmConfig
}

fun saveConfig(hmConfig: Map<String, String>, fileName: String, charset: Charset = Charsets.UTF_8) {
    val bwText = getFileWriter(fileName, false, charset)
    for ((key, value) in hmConfig) {
        bwText.write(key)
        bwText.write(" = ")
        bwText.write(value)
        bwText.newLine()
    }
    bwText.close()
}

fun loadLinkConfig(hmConfig: Map<String, String>, keyServerIP: String, keyServerPort: String, alServerIP: MutableList<String>, alServerPort: MutableList<Int>) {
    var index = 0
    while (true) {
        val serverIP = hmConfig[keyServerIP + index]
        val serverPort = hmConfig[keyServerPort + index]
        if (serverIP == null || serverIP.isEmpty() || serverPort == null || serverPort.isEmpty()) break
        alServerIP.add(serverIP)
        alServerPort.add(Integer.parseInt(serverPort))
        index++
    }
}

import java.io.File
import kotlin.math.ceil

fun main(args: Array<String>) {

    print("Please enter the target directory > ")
    val targetDir = File(readLine() ?: "")

    print("Do you want to use Pytorch format? [Enter/N] > ")
    val isPytorchFormat = (readLine()?.uppercase() != "N")

    if(!targetDir.exists() || targetDir.isFile) {
        println("[ERROR] No such directory exists.")
        return
    }

    if(isPytorchFormat) {
        createDataset(getFile(targetDir))
    } else {
        createDatasetToLinear(getFile(targetDir))
    }
}

fun createDataset(allFiles: List<File>) {
    val currentDir = File(System.getProperty("user.dir"))
    val datasetDir = getDir(currentDir, "dataset")
    val trainDir = getDir(datasetDir, "/train/")
    val validDir = getDir(datasetDir, "/valid/")

    val copiedMap = mutableMapOf<Trump, MutableList<File>>()
    val validData = mutableMapOf<Trump, Int>()
    val validRate = 0.25

    for (file in allFiles) {
        val id = getId(file.name)?.uppercase() ?: continue
        val name = getName(file.name).uppercase()
        val trump = Trump.values().findLast { it.isMatch(id) } ?: continue
        val fileName = "${trump.value}-$name-${copiedMap[trump]?.size ?: 0}.${file.extension}"
        val saveDir = getDir(trainDir, trump.value)
        val saveFile = File(saveDir, fileName)

        file.copyTo(saveFile, overwrite = true)

        if(copiedMap[trump] != null) copiedMap[trump]!!.add(saveFile)
        else copiedMap[trump] = mutableListOf(saveFile)

        println("[${trump.value}] $id, $name, ${file.absolutePath}")
    }

    for ((trump, files) in copiedMap) {
        val validSize = ceil(files.size * validRate).toInt()
        val moveFiles = files.shuffled().take(validSize)

        validData[trump] = moveFiles.size

        for ((i, file) in moveFiles.withIndex()) {
            val saveFile = File(getDir(validDir, trump.value), file.name)

            file.copyTo(saveFile)
            file.delete()

            println("Valid data moving... [${trump.value}:$i] ${saveFile.absolutePath}")
        }
    }

    val allSize = copiedMap.values.flatten().size
    val validSize = validData.values.sum()
    val trainSize = allSize - validSize

    println("[FINISH] Created $allSize data. (train: $trainSize, valid: $validSize) [DIR: ${datasetDir.absolutePath}]")
}

fun createDatasetToLinear(allFiles: List<File>) {
    val currentDir = File(System.getProperty("user.dir"))
    val datasetDir = getDir(currentDir, "dataset")
    val errorDir = getDir(datasetDir, "error")

    val copiedMap = mutableMapOf<Trump, MutableList<File>>()

    for (file in allFiles) {
        val id = getId(file.name)?.uppercase()
        val name = getName(file.name).uppercase()
        val trump = id?.let { Trump.values().findLast { it.isMatch(id) } }

        if(id == null || trump == null) {
            file.copyTo(File(errorDir, file.name), overwrite = true)
            continue
        }

        val fileName: String
        val saveFile: File

        if(name.uppercase() == "YS" || file.extension.uppercase() == "HEIC") {
            fileName = "${trump.value}-$name-${copiedMap[trump]?.size ?: 0}.HEIC"
            saveFile = File(errorDir, fileName)
        } else {
            fileName = "${trump.value}-$name-${copiedMap[trump]?.size ?: 0}.${file.extension}"
            saveFile = File(datasetDir, fileName)
        }

        file.copyTo(saveFile, overwrite = true)

        if(copiedMap[trump] != null) copiedMap[trump]!!.add(saveFile)
        else copiedMap[trump] = mutableListOf(saveFile)

        println("[${trump.value}] $id, $name, ${file.absolutePath}")
    }

    println("[FINISH] Created ${copiedMap.values.flatten().size} data. (Linear) [DIR: ${datasetDir.absolutePath}]")
}

fun getFile(parentFile: File): List<File> {
    val childFiles = parentFile.listFiles() ?: emptyArray()
    val resultFiles = mutableListOf<File>()

    for (file in childFiles) {
        resultFiles.addAll(if (file.isDirectory) getFile(file) else listOf(file))
    }

    return resultFiles
}

fun getDir(parentDir: File, name: String): File {
    val file = File(parentDir, name)
    if(!file.exists()) file.mkdir()
    return file
}

fun getId(name: String): String? {
    val index = name.indexOf("-")
    if(index != -1) return name.substring(0, index)

    val otherIndex = name.indexOf("_")
    val idCandidate = if(otherIndex != -1) name.substring(0, otherIndex) else return null

    return Trump.values().findLast { it.isMatch(idCandidate) }?.value
}

fun getName(name: String): String {
    val index = name.indexOf("-")
    return if (index != -1) name.substring(index + 1, index + 3) else "DM"
}

fun Trump.isMatch(name: String): Boolean {
    val index1 = name.indexOf(this.value)
    val index2 = name.indexOf(this.subValue ?: this.value)

    return (index1 != -1 || index2 != -1)
}

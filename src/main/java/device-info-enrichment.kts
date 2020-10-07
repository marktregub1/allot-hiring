import org.homesecure.devicedata.DeviceDataEnricher
import org.homesecure.devicedata.DeviceDataManager
import java.io.File
import kotlin.system.exitProcess

//dfc79b8cfa10fc76dfb17caee35f13d0
//

val USAGE = "cmd !input_csv_file! !out_csv_file! !access_key!"

if(args.size < 3) {
    println(USAGE)
    exitProcess(1)
}

val csvFile = File(args[0])
if(!csvFile.exists()) {
    println("Mandatory file doesn't exist: ${csvFile.absolutePath}");
    exitProcess(1)
}
val enrichmentUrl = "http://api.userstack.com/detect?access_key=${args[2]}&fields=device&ua="

val manager = DeviceDataManager(csvFile.inputStream())
manager.enrichData(
        DeviceDataEnricher(enrichmentUrl), File(args[1]).outputStream())


import dataprocessing.OutputLogger
import simulation.constants.Constants
import simulation.logic.Logic


fun main(args: Array<String>) {
  //  println("Algorithms for Decision Support - Simulation Assignment \n")
//    val q = intArrayOf(3, 5, 7, 9, 11, 13, 15)
//    println("Welcome to the Uithof Lijn simulation")
//    println("Do you want to set any parameters?: Y/N ")
//    if(readLine() == "y" || readLine() == "y'" ) {
//        println("What q value should we set (in minutes)?  default is 3: ")
//        var answ = readLine()
//        if (answ != null || answ != "") {
//            Constants.endPointDwell = readLine()!!.toInt() * 60
//        }
//        println("What scaling should we set ? Default is 1.0: ")
//        answ = readLine()
//        if (answ != null || answ != "") {
//            Constants.passengerScaler = readLine()!!.toDouble()
//        }
//        println("What peak frequency should we set? Default is 4 minutes: ")
//        answ = readLine()
//        if (answ != null || answ != "") {
//            Constants.peakTramFrequency = readLine()!!.toInt() * 60
//        }
//        println("What off peak tram frequency should we set?: Default is 12 minutes: ")
//        if (answ != null || answ != "") {
//            Constants.offPeakTramFrequency = readLine()!!.toInt() * 60
//        }
//    }

    println("Let's start the simulation")
//    val scalings = arrayOf(4,5)
//    for(scale in scalings) {
//        Constants.endPointLanes = scale
//        repeat(1000) {
            val logic = Logic()
            logic.runSimulation()
            logic.report()
//            logic.logOutput()
//        }
//    }

//    // Run the validation inputs
//    val inputFiles = arrayListOf(
//            "../Datasets/Validation/input-data-passengers-01.csv",
//            "../Datasets/Validation/input-data-passengers-02.csv",
//            "../Datasets/Validation/input-data-passengers-03.csv",
//            "../Datasets/Validation/input-data-passengers-04.csv",
//            "../Datasets/Validation/input-data-passengers-06.csv",
//            "../Datasets/Validation/input-data-passengers-015.csv",
//            "../Datasets/Validation/input-data-passengers-025.csv"
//    )
//
//    for (input in inputFiles) {
//        Constants.validationFile = input
//        repeat(1000) {
//            val logic = Logic()
//            logic.runSimulation()
////            logic.report()
//            logic.logOutput()
//        }
//    }


    if (Constants.validationFile != "") {
        System.err.println("This simulation ran using this validation input file: ${Constants.validationFile}")
    }
}

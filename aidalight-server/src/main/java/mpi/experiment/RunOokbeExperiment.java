package mpi.experiment;

import java.io.File;
import java.util.Arrays;

import mpi.aida.AidaManager;


public class RunOokbeExperiment {
  public static void main(String[] args) throws Exception {
    AidaManager.init();
    String result = new RunExperiment().run(args);
    String[] extArgs = Arrays.copyOf(args, args.length + 2);
    extArgs[extArgs.length - 2] = "--ookbepreprocess";
    extArgs[extArgs.length - 1] = result;
    new RunExperiment().run(extArgs);
    //new File(result).delete();
  }
}
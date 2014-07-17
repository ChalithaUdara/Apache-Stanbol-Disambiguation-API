package mpi.aida;

import java.util.Arrays;
import java.util.List;

import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.data.PreparedInput;
import mpi.aida.util.RunningTimer;

public class Preparator {

  public PreparedInput prepare(String docId, String text, PreparationSettings settings) {
    Integer timerId = RunningTimer.start("Preparator");
    PreparedInput preparedInput = AidaManager.prepareInputData(
        text, docId, settings.getMentionsFilter());
    String[] types = settings.getFilteringTypes();
    if (types != null) {
      List<String> filteringTypes = Arrays.asList(settings.getFilteringTypes());
      preparedInput.getMentions().setEntitiesTypes(filteringTypes);
    }
    RunningTimer.end("Preparator", timerId);
    return preparedInput;
  }
}
